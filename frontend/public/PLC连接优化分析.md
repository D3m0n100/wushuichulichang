# PLC 后端连接优化分析

> 分析日期：2026-07-06
> 后端路径：`C:\Users\yang\Desktop\14\water\water412\waterfactoryceshi`

---

## 一、架构概览

### 1.1 两套 PLC 驱动

| PLC | 驱动 | 连接池 | 协议 |
|-----|------|--------|------|
| B1 (S7-1200) | Apache PLC4X | `GenericObjectPool<PlcConnection>` | S7 |
| H1 (S7-1500) | Apache PLC4X | `GenericObjectPool<PlcConnection>` | S7 |
| M3 (S7-1200) | Apache PLC4X | `GenericObjectPool<PlcConnection>` | S7 |
| 其他 13 个 (S7-200 Smart) | Snap7 | `GenericObjectPool<S7Client>` | S7 |

### 1.2 关键文件

| 文件 | 作用 |
|------|------|
| `service/B1PlcService.java` | B1 PLC 读写（PLC4X） |
| `service/H1PlcService.java` | H1 PLC 读写（PLC4X） |
| `service/M3PlcService.java` | M3 PLC 读写（PLC4X） |
| `service/B1PlcConnectionPool.java` | B1 PLC4X 连接池 |
| `service/H1PlcConnectionPool.java` | H1 PLC4X 连接池 |
| `service/M3PlcConnectionPool.java` | M3 PLC4X 连接池 |
| `service/OverviewCache.java` | 共享缓存（stale-while-revalidate） |
| `service/AbstractS7200SmartPlcService.java` | Snap7 基类（已优化批量读取） |

---

## 二、问题分析

### 🔴 问题 1：逐设备读取（最严重）

**现象：** `readOverviewInternal()` 中，`readDevices()` / `readInstruments()` / `readTimers()` 每个设备/仪表/定时器各自调用一次 `readTags()`。

**H1 调用链（最严重）：**

```
readOverviewInternal()
  ├── readDevices()           → 27 个设备 × 1 次 readTags() = 27 次 PLC 读取
  ├── readInstruments()       → 26 个仪表 × 1 次 readTags() = 26 次 PLC 读取
  ├── readTimers()            → 6 个定时器 × 1 次 readTags()  = 6 次 PLC 读取
  ├── readLogicConfigs()      → 2 个 × 1 次 readTags()        = 2 次 PLC 读取
  ├── readCommunicationValues() → 1 个 × 1 次 readTags()      = 1 次 PLC 读取
  └── readParameters()        → 批量读 1 次                    = 1 次 PLC 读取
  ──────────────────────────────────────────────────────────
  合计: ~63 次连接借用 + 63 次 PLC 读取 + 63 次连接归还
```

**各服务统计：**

| 服务 | 设备 | 仪表 | 定时器 | 逻辑 | 其他 | 总 readTags 调用 |
|------|------|------|--------|------|------|-----------------|
| B1PlcService | 16 | 3 | 0 | 0 | 1(参数) | **~20 次** |
| H1PlcService | 27 | 26 | 6 | 2 | 2(通信+参数) | **~63 次** |
| M3PlcService | 8 | ? | ? | ? | 2(气体+参数) | **~20+ 次** |

**每次 `readTags()` 内部：**

```java
connection = connectionPool.borrowConnection();  // 借连接
PlcReadRequest request = builder.build();
PlcReadResponse response = request.execute().get(5, TimeUnit.SECONDS);  // 5 秒超时
connectionPool.returnConnection(connection);      // 还连接
```

**对比 Snap7（`AbstractS7200SmartPlcService`）：**

```java
// Snap7 做法：先收集所有 tag，一次读取
readOverview()
  └── readTags(new ArrayList<>(tags.keySet()))  // 所有 tag 一次传入
        → buildReadBatches() 自动合并相邻地址
        → 几次 ReadArea 调用搞定
```

**影响：**
- H1 每次 overview 请求耗时 ≈ 63 × (连接借用 + PLC 读取 + 连接归还)
- 连接池 maxTotal=5，63 次读取严重争抢连接
- 前端 2 秒轮询 + 后端 63 次读取 → 响应可能超时

**修复方案：**

```java
// 改前（逐设备读）：
private List<Map<String, Object>> readDevices() {
    for (DeviceDef device : devices.values()) {
        result.add(buildDeviceState(device, readTags(device.getReadTags())));  // 每个设备一次
    }
}

// 改后（批量读）：
private Map<String, Object> readOverviewInternal() {
    // 1. 收集所有 tag
    List<String> allTags = new ArrayList<>();
    for (DeviceDef d : devices.values()) allTags.addAll(d.getReadTags());
    for (InstrumentDef i : instruments.values()) allTags.addAll(i.getReadTags());
    for (TimerDef t : timers.values()) allTags.addAll(t.getReadTags());
    // ...

    // 2. 一次读取
    Map<String, Object> snapshot = readTags(allTags);

    // 3. 从 snapshot 提取各设备数据
    result.put("devices", readDevices(snapshot));
    result.put("instruments", readInstruments(snapshot));
    // ...
}
```

---

### 🔴 问题 2：OverviewCache 全局锁

**代码位置：** `OverviewCache.java` 第 25 行

```java
public synchronized CacheResult getResilient(long ttlMs, Loader loader) {
    if (value != null && now - timestamp <= ttlMs) {
        return new CacheResult(value, true, false, ...);  // 缓存命中
    }
    Object freshValue = loader.load();  // ⚠️ 可能耗时 5+ 秒！
    // ...
}
```

**问题：**
- `synchronized` 是实例级锁
- 当 Loader（PLC 读取）耗时时，所有其他线程被阻塞
- 前端 13 个页面同时轮询 → 13 个 HTTP 线程排队
- 最坏情况：13 × 5 秒 = 65 秒全部阻塞

**修复方案：**

```java
// 方案 A：ReentrantLock + tryLock
private final ReentrantLock lock = new ReentrantLock();

public CacheResult getResilient(long ttlMs, Loader loader) {
    long now = System.currentTimeMillis();
    // 无锁读缓存
    if (value != null && now - timestamp <= ttlMs) {
        return new CacheResult(value, true, false, ...);
    }
    // 尝试获取锁，拿不到就返回旧缓存
    if (lock.tryLock()) {
        try {
            // 双重检查
            if (value != null && System.currentTimeMillis() - timestamp <= ttlMs) {
                return new CacheResult(value, true, false, ...);
            }
            Object freshValue = loader.load();
            // ...
        } finally {
            lock.unlock();
        }
    } else if (value != null) {
        // 其他线程在刷新，返回旧数据
        return new CacheResult(value, false, true, ...);
    }
    throw new IllegalStateException("No cache and refresh in progress");
}

// 方案 B：后台定时刷新（推荐）
@Scheduled(fixedDelay = 2000)
public void backgroundRefresh() {
    try {
        readOverviewInternal();  // 写入缓存
    } catch (Exception e) {
        log.error("Background refresh failed", e);
    }
}
// HTTP 请求只读缓存，不触发 PLC 读取
```

---

### 🟡 问题 3：B1 连接池 testOnBorrow=false

**代码位置：** `B1PlcConnectionPool.java` 第 27 行

```java
config.setTestOnBorrow(false);  // 借用前不验证连接
```

**对比 Snap7 连接池：**

```java
// AbstractS7200SmartPlcService.java:739
config.setTestOnBorrow(true);   // 借用前验证 client.Connected
```

**后果：** B1 借到已断开的连接 → `execute().get(5, SECONDS)` 超时 → 浪费一轮轮询。

**修复：**

```java
config.setTestOnBorrow(true);
// 并在 factory 中实现 validateObject
@Override
public boolean validateObject(PooledObject<PlcConnection> p) {
    try {
        return p.getObject() != null && p.getObject().isConnected();
    } catch (Exception ex) {
        return false;
    }
}
```

---

### 🟡 问题 4：PLC4X 连接池无失败销毁

**Snap7 的做法（已优化）：**

```java
// AbstractS7200SmartPlcService.java:702-730
private <T> T withClient(Snap7Callback<T> callback, String action) {
    S7Client client = null;
    boolean invalidate = false;
    try {
        client = snap7ClientPool.borrowObject();
        return callback.execute(client);
    } catch (Exception ex) {
        invalidate = true;  // 标记销毁
        throw ...;
    } finally {
        if (client != null) {
            if (invalidate) {
                snap7ClientPool.invalidateObject(client);  // 销毁坏连接
            } else {
                snap7ClientPool.returnObject(client);       // 归还好连接
            }
        }
    }
}
```

**PLC4X 的做法（未优化）：**

```java
// B1PlcService.java:220-264
private Map<String, Object> readTags(Collection<String> tagCodes) {
    PlcConnection connection = null;
    try {
        connection = connectionPool.borrowConnection();
        // ...
    } catch (Exception ex) {
        throw new IllegalStateException(...);  // 直接抛异常，连接可能泄漏
    } finally {
        if (connection != null) {
            connectionPool.returnConnection(connection);  // ⚠️ 坏连接也被归还
        }
    }
}
```

**后果：** 坏连接被归还到池中 → 下次借用还是坏的 → 连续失败。

**修复：**

```java
private Map<String, Object> readTags(Collection<String> tagCodes) {
    PlcConnection connection = null;
    boolean invalidate = false;
    try {
        connection = connectionPool.borrowConnection();
        // ... 读取 ...
        return values;
    } catch (Exception ex) {
        invalidate = true;
        throw new IllegalStateException(...);
    } finally {
        if (connection != null) {
            if (invalidate) {
                connectionPool.invalidateConnection(connection);  // 销毁
            } else {
                connectionPool.returnConnection(connection);      // 归还
            }
        }
    }
}
```

---

### 🟡 问题 5：无自动重试

**当前行为：** 读取失败 → 抛异常 → OverviewCache 返回 stale 数据。

**建议：** 失败后重试 1 次（可能是连接断了，销毁后重建即可）。

```java
Map<String, Object> values = null;
try {
    values = readTagsInternal(allTags);
} catch (Exception e) {
    log.warn("First read failed, retrying...", e);
    // 销毁当前连接，重试一次
    values = readTagsInternal(allTags);
}
```

---

### 🟢 已做得好的地方

| 特性 | 说明 |
|------|------|
| Snap7 批量读取 | `buildReadBatches()` 自动合并相邻 tag（gap≤8 字节，max 64 字节） |
| Snap7 连接验证 | `testOnBorrow=true` + `validateObject()` |
| Snap7 失败销毁 | `withClient()` 中 `invalidate=true` 时销毁连接 |
| OverviewCache stale 缓存 | 失败时返回旧数据，前端不会白等 |
| lastSuccessAt / lastFailureAt | 已暴露给前端，可用于连接状态判定 |
| PLC4X 连接池空闲驱逐 | 3 分钟驱逐 + 60 秒检查 |

---

## 三、优化建议汇总

| 优先级 | 改动 | 预估效果 |
|--------|------|---------|
| **P0** | H1/B1/M3 改批量读取 | 63 次 → 几次，延迟降 90%+ |
| **P0** | OverviewCache 改异步/无锁 | 13 个前端不再排队阻塞 |
| **P1** | B1 开启 testOnBorrow=true | 避免借到死连接 |
| **P1** | PLC4X 添加失败销毁逻辑 | 坏连接不再循环使用 |
| **P1** | 添加读取重试 | 提高单次请求成功率 |
| **P2** | 后台定时采集（@Scheduled） | 工业级标准方案 |

---

## 四、连接池配置对比

| 参数 | B1 (PLC4X) | Snap7 (其他) |
|------|-----------|-------------|
| maxTotal | 5 | 5 |
| maxIdle | 3 | 3 |
| minIdle | 1 | 1 |
| maxWait | 5000ms | 3000ms |
| testOnBorrow | **false** | true |
| testWhileIdle | true | true |
| 驱逐时间 | 180s | 180s |
| 驱逐间隔 | 60s | 60s |
| RecvTimeout | N/A | 3000ms |
| 失败销毁 | **无** | 有 |

---

## 五、前端轮询现状

| 页面 | 轮询间隔 | 请求的 PLC 端点 |
|------|---------|----------------|
| Dashboard | 5s | /plc/b1/overview + /water/realtime |
| GlobalAlarm | 10s | 13 个 /plc/*/overview（已改为共享池） |
| B1.vue | 3-10s | /plc/b1/overview + /plc/coarse-screen/overview |
| H1.vue | 3s | /plc/h1/overview |
| M3.vue | 3s | /plc/m3/overview |
| ProcessControl.vue | 2s | 全部 13 个端点 |

**前端已优化：**
- Dashboard + GlobalAlarm 共享 `usePlcStatus` 轮询池
- 所有 PLC 页面连续 3 次失败才显示"未连接"
- ProcessControl ✕ 阈值改为 6 秒（3 个轮询周期）
