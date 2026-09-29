# PLC 连接架构分析与优化方案

## 一、当前 PLC 连接架构

### 1. 两种 PLC 类型

| 类型 | 控制器 | 协议库 | IP 范围 |
|---|---|---|---|
| **S7-200 SMART** | 13台（粗格栅、细格栅、风机等） | Snap7 | 192.168.0.14~30 |
| **S7-1200** | 3台（B1、H1、M3） | PLC4X | 192.168.0.100~106 |

### 2. S7-200 SMART 连接方式（13台）

```
┌─────────────────────────────────────────────────────────┐
│  AbstractS7ConnectionPool                               │
│  ├── borrowConnection() → 每次创建新连接！               │
│  ├── returnConnection() → 立即关闭连接！                 │
│  └── 14个TSAP候选地址，失败后轮转                         │
└─────────────────────────────────────────────────────────┘
         ↓ 传入
┌─────────────────────────────────────────────────────────┐
│  AbstractS7200SmartPlcService                           │
│  ├── 内部 Snap7 连接池 (GenericObjectPool<S7Client>)    │
│  │   ├── maxTotal: 3                                    │
│  │   ├── maxIdle: 2                                     │
│  │   ├── minIdle: 1                                     │
│  │   └── maxWait: 3000ms                                │
│  ├── 每个标签单独读取 (ReadArea per tag)                  │
│  └── 无批量读取                                          │
└─────────────────────────────────────────────────────────┘
```

**问题：**
- `AbstractS7ConnectionPool.borrowConnection()` 每次都 `new PlcDriverManager().getConnection(url)`，**没有连接复用**
- `returnConnection()` 直接 `connection.close()`，**用完就销毁**
- 虽然 `AbstractS7200SmartPlcService` 内部有 Snap7 连接池，但外层连接池完全浪费
- 每个标签单独发一次 `ReadArea` 请求，读10个标签 = 10次 PLC 通信

### 3. S7-1200 连接方式（B1/H1/M3）

```
┌─────────────────────────────────────────────────────────┐
│  B1PlcConnectionPool (Apache Commons Pool2)             │
│  ├── maxTotal: 3                                        │
│  ├── maxIdle: 2                                         │
│  ├── minIdle: 1                                         │
│  └── 真正的连接复用                                      │
└─────────────────────────────────────────────────────────┘
         ↓ 传入
┌─────────────────────────────────────────────────────────┐
│  B1PlcService (PLC4X)                                   │
│  ├── 批量读取 (一次请求读所有标签)                         │
│  └── 效率高                                              │
└─────────────────────────────────────────────────────────┘
```

### 4. 连接池容量汇总

| PLC 模块 | IP | 连接池类型 | maxTotal | 实际连接方式 |
|---|---|---|---|---|
| coarse-screen | .27 | Snap7 内部池 | 3 | 每次新建→借出→归还→销毁 |
| fine-screen | .26 | Snap7 内部池 | 3 | 同上 |
| multilayer-cyclone | .25 | Snap7 内部池 | 3 | 同上 |
| pac | .20 | Snap7 内部池 | 3 | 同上 |
| pam | .19 | Snap7 内部池 | 3 | 同上 |
| sodium-acetate | .18 | Snap7 内部池 | 3 | 同上 |
| sodium-hypochlorite | .17 | Snap7 内部池 | 3 | 同上 |
| baike-dosing | .14 | Snap7 内部池 | 3 | 同上 |
| baike-filter-press | .15 | Snap7 内部池 | 3 | 同上 |
| baike-fiber-disc | .16 | Snap7 内部池 | 3 | 同上 |
| fan | .24 | Snap7 内部池 | 3 | 同上 |
| sludge-silo | .28 | Snap7 内部池 | 3 | 同上 |
| deodorization | .29 | Snap7 内部池 | 3 | 同上 |
| rotor-pump | .30 | Snap7 内部池 | 3 | 同上 |
| **b1** | **.106** | **PLC4X 池** | **3** | **真正的连接复用** |
| **h1** | **.100** | **PLC4X 池** | **3** | **真正的连接复用** |
| **m3** | **.103** | **PLC4X 池** | **3** | **真正的连接复用** |

### 5. 连接竞争场景

前端多个页面同时轮询同一个 PLC 时，会争抢连接池中的连接：

| 来源 | 调用 | 轮询间隔 |
|---|---|---|
| `plc-debug.html` | `GET /api/plc/b1/overview` | 3000ms（默认） |
| 前端 `B1.vue` | `GET /api/plc/b1/overview` | 3000ms |
| 前端 `Dashboard.vue` | `GET /api/plc/b1/overview` | 5000ms |
| 前端 `AlarmPage.vue` | `GET /api/plc/b1/overview` | 轮询 |
| 前端 `ProcessControl.vue` | `GET /api/plc/b1/overview` | 轮询 |

当多个页面同时轮询时，3 个连接要被 2-5 个并发请求争抢，可能导致：
- 请求排队等待连接（`borrowObject` 阻塞）
- 5秒超时后报错 `Failed to read B1 PLC tags`

---

## 二、优化方案

### 方案一：S7-200 SMART 真正实现连接池化（优先级：高）

当前 `AbstractS7ConnectionPool` 每次都新建连接再销毁，完全浪费。应该像 B1 那样用 Apache Commons Pool2 管理连接：

```java
// 当前（每次新建）
public PlcConnection borrowConnection() {
    return new PlcDriverManager().getConnection(url);  // 新建TCP连接
}

public void returnConnection(PlcConnection connection) {
    connection.close();  // 立即销毁
}
```

改为：

```java
// 优化后（复用连接）
private final GenericObjectPool<PlcConnection> pool;

public PlcConnection borrowConnection() {
    return pool.borrowObject();  // 从池中借出
}

public void returnConnection(PlcConnection connection) {
    pool.returnObject(connection);  // 归还到池中
}
```

**效果：** 减少 90% 的 TCP 连接建立/销毁开销，每次请求节省 50-200ms。

### 方案二：S7-200 SMART 批量读取（优先级：中）

当前每个标签单独发一次 `ReadArea`，读10个标签 = 10次 PLC 通信。应该合并为一次：

```java
// 当前（逐个读取）
for (String tagCode : tagCodes) {
    readArea(client, area, 0, byteOffset, buffer);  // 每个标签一次通信
}

// 优化后（批量读取）
// 将连续地址的标签合并为一次 ReadArea 调用
// 例如 DB1.DBX16.0~DB1.DBX16.4 可以一次读取 1 字节
```

**效果：** 减少 PLC 通信次数，降低网络延迟。

### 方案三：增大连接池容量（优先级：高）

当前所有池都是 `maxTotal=3`，前端多个页面同时轮询时会排队。建议：

```java
// S7-200 SMART 池
config.setMaxTotal(5);    // 3 → 5
config.setMaxIdle(3);     // 2 → 3

// B1/H1/M3 池
config.setMaxTotal(5);    // 3 → 5
config.setMaxIdle(3);     // 2 → 3
```

**效果：** 支持更多并发请求，减少超时。

### 方案四：前端轮询优化（优先级：低）

当前多个页面（B1.vue、Dashboard.vue、AlarmPage.vue、ProcessControl.vue）各自独立轮询，同一个 PLC 被请求多次。可以：

```javascript
// 方案A：后端增加缓存层
// overview 结果缓存 1-2 秒，多个前端请求共享同一份数据

// 方案B：前端共享轮询
// 使用一个全局 store，只发一次请求，多个组件共享数据
```

---

## 三、优先级建议

| 优先级 | 方案 | 改动量 | 效果 |
|---|---|---|---|
| 🔴 高 | 方案一：连接池化 | 中 | 减少 90% 连接开销 |
| 🔴 高 | 方案三：增大池容量 | 小 | 减少超时 |
| 🟡 中 | 方案二：批量读取 | 大 | 减少 PLC 通信次数 |
| 🟢 低 | 方案四：前端轮询优化 | 中 | 减少总请求数 |

---

## 四、注意事项

1. **关闭 `plc-debug.html` 后不会残留连接** — HTTP 请求是无状态的，关闭页面后所有连接立即释放
2. **S7-200 SMART 的 TSAP 轮转机制** — 14个候选地址按顺序尝试，失败后切换到下一个，成功后记住该地址
3. **S7-1200 的 PLC4X 连接** — 支持真正的连接池化和批量读取，效率远高于 S7-200 SMART
4. **脉冲信号 vs 持续信号** — 粗格栅等设备使用脉冲信号（写true→等待→写false），B1/H1/M3 使用持续信号（写1等待写0）
