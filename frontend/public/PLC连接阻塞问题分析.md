# PLC 连接阻塞导致全屏 ✕ 问题分析

> 日期：2026-07-06

---

## 一、问题现象

- ProcessControl.vue 页面设备名称旁边突然全部显示 ✕
- 持续约 10 秒甚至更长
- 不是某个 PLC 的问题，是所有 PLC 同时 ✕

---

## 二、根因分析

### 2.1 直接原因：后端 servlet 线程耗尽

Snap7 的 `ConnectTo()` 方法在 PLC 不可达时，依赖操作系统 TCP 超时（Windows 默认 20-75 秒）。

```
PLC 不可达 (如 PAM 192.168.0.19)
  → ConnectTo() 阻塞 20+ 秒
  → servlet 线程被占用
  → 13 个 PLC 并发请求 × 每个可能阻塞 20 秒
  → 200 个 servlet 线程（Tomcat 默认）被耗尽
  → 其他 PLC 的请求排队等待
  → 前端 3 秒超时断开
  → 所有设备 lastReadTime 不更新
  → 超过 5 秒阈值 → 全屏 ✕
```

### 2.2 时间线还原

```
T=0s    前端发起 13 个 PLC 请求
T=0.1s  PAC、PAM 等不可达 PLC 的 ConnectTo 开始阻塞
T=0.5s  可达的 PLC（B1、H1 等）请求被 servlet 线程排队
T=3s    前端超时断开，但后端线程仍在阻塞
T=5s    前端 lastReadTime 超过阈值，全屏 ✕
T=20s+  后端 ConnectTo 超时，线程释放（太晚了）
```

### 2.3 为什么之前的修改没有解决

| 尝试的修改 | 为什么无效 |
|-----------|----------|
| 前端超时从 15s 改为 3s | 前端断开了，但后端线程还在阻塞 |
| Promise.all 改 forEach | 每个请求独立，但都排队等 servlet 线程 |
| ✕ 阈值从 3s 改为 5s/6s | 根本问题是后端线程耗尽，不是前端判定太快 |
| OverviewCache 改 ReentrantLock | 缓存层没问题，问题是底层连接阻塞 |

---

## 三、修复方案

### 3.1 核心修复：Snap7 连接线程级超时

**文件：** `AbstractS7200SmartPlcService.java`

**改前：**
```java
private S7Client createConnectedClient() {
    S7Client client = new S7Client();
    client.RecvTimeout = 3000;
    int connectResult = client.ConnectTo(plcIp, rack, slot);  // 可能阻塞 20+ 秒
    if (connectResult != 0) {
        throw new IllegalStateException(...);
    }
    return client;
}
```

**改后：**
```java
private static final ExecutorService connectExecutor = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "snap7-connect");
    t.setDaemon(true);
    return t;
});

private S7Client createConnectedClient() throws Exception {
    Future<S7Client> future = connectExecutor.submit(() -> {
        S7Client client = new S7Client();
        client.RecvTimeout = 3000;
        int connectResult = client.ConnectTo(plcIp, rack, slot);
        if (connectResult != 0) {
            throw new IllegalStateException(...);
        }
        return client;
    });
    try {
        return future.get(2, TimeUnit.SECONDS);  // 硬超时 2 秒
    } catch (TimeoutException ex) {
        future.cancel(true);  // 取消阻塞的连接尝试
        throw new IllegalStateException("Snap7 connect timeout (2s), ip=" + plcIp);
    }
}
```

**效果：**
- PLC 不可达时，2 秒超时释放线程（原来 20+ 秒）
- servlet 线程不再被长时间占用
- 其他 PLC 的请求正常处理

### 3.2 辅助修复：GlobalExceptionHandler 静默处理客户端断开

**文件：** `GlobalExceptionHandler.java`

```java
@ExceptionHandler(ClientAbortException.class)
public void handleClientAbort(ClientAbortException exception) {
    log.debug("Client disconnected: {}", exception.getMessage());
    // 不写响应，客户端已断开
}
```

**效果：** 后端日志不再刷屏 `ClientAbortException`。

### 3.3 辅助修复：PLC4X 连接池 URL 尝试次数限制

**文件：** `AbstractS7ConnectionPool.java`

```java
// 改前：尝试全部 14 个 URL
for (int i = 0; i < connectionUrls.size(); i++) { ... }

// 改后：最多尝试 3 个
int maxAttempts = Math.min(3, connectionUrls.size());
for (int i = 0; i < maxAttempts; i++) { ... }
```

**效果：** PLC4X 路径的连接尝试从 14 次减少到 3 次。

---

## 四、涉及文件

| 文件 | 改动 |
|------|------|
| `AbstractS7200SmartPlcService.java` | `createConnectedClient` 改用 `Future.get(2s)` 超时 |
| `GlobalExceptionHandler.java` | 添加 `ClientAbortException` 静默处理 |
| `AbstractS7ConnectionPool.java` | URL 尝试次数从 14 限制为 3 |

---

## 五、验证方法

1. 断开某个 PLC（如 PAM 192.168.0.19）
2. 观察后端日志：应看到 `Snap7 connect timeout (2s)` 而非长时间阻塞
3. 观察前端：只有 PAM 的设备显示 ✕，其他 PLC 设备正常
4. 后端日志不应出现 `ClientAbortException` 刷屏

---

## 六、关键教训

| 问题 | 教训 |
|------|------|
| Snap7 `ConnectTo` 无内置超时 | 工业 PLC 通信必须在应用层加超时 |
| servlet 线程是有限资源 | 一个阻塞的 PLC 不应拖垮整个系统 |
| 前端超时 ≠ 后端释放 | 前端断开后，后端线程仍在运行 |
| TCP 超时因 OS 而异 | Windows 默认 20-75 秒，不能依赖 |
