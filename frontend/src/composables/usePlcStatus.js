/**
 * 共享 PLC 状态管理
 *
 * 统一轮询所有 PLC 端点，避免 Dashboard / GlobalAlarm 各自重复请求。
 * 连接状态采用防抖判定：连续 3 次失败 或 距上次成功超过 10 秒 才标记"未连接"。
 */
import { ref, readonly } from 'vue'
import request from '@/api/request'

// ── PLC 端点配置 ────────────────────────────────────────────
const PLC_ENDPOINTS = [
  { url: '/plc/b1/overview', location: '调节池' },
  { url: '/plc/h1/overview', location: 'AAO生物池' },
  { url: '/plc/m3/overview', location: '高效沉淀池' },
  { url: '/plc/coarse-screen/overview', location: '粗格栅' },
  { url: '/plc/fine-screen/overview', location: '细格栅' },
  { url: '/plc/multilayer-cyclone/overview', location: '旋流沉砂池' },
  { url: '/plc/fan/overview', location: '鼓风机房' },
  { url: '/plc/pac/overview', location: 'PAC加药间' },
  { url: '/plc/pam/overview', location: 'PAM加药间' },
  { url: '/plc/sodium-acetate/overview', location: '乙酸钠加药间' },
  { url: '/plc/sodium-hypochlorite/overview', location: '次氯酸钠加药间' },
  { url: '/plc/baike-fiber-disc/overview', location: '滤布滤池' },
  { url: '/plc/baike-filter-press/overview', location: '脱水车间' },
]

// ── 防抖参数 ────────────────────────────────────────────────
const FAILURE_THRESHOLD = 3       // 连续失败 3 次才判定断开
const STALE_THRESHOLD_MS = 10000  // 距上次成功超过 10 秒判定断开

// ── 响应式状态（模块级单例，所有组件共享） ─────────────────
/** 各端点设备数据 { [url]: { devices: [...], plc: {...} } } */
const endpointData = ref({})
/** 各端点连接状态 { [url]: { connected, consecutiveFailures, lastSuccessAt } } */
const connectionStatus = ref({})
/** 汇总后的设备故障列表（供 GlobalAlarm / Dashboard 使用） */
const allDevices = ref([])

let pollTimer = null
let isPolling = false

// ── 内部：更新连接状态 ─────────────────────────────────────
function markSuccess(url) {
  const now = Date.now()
  const prev = connectionStatus.value[url] || {}
  connectionStatus.value[url] = {
    connected: true,
    consecutiveFailures: 0,
    lastSuccessAt: now,
  }
}

function markFailure(url) {
  const prev = connectionStatus.value[url] || {}
  const failures = (prev.consecutiveFailures || 0) + 1
  // 只有连续失败 >= 3 次才标记断开；否则保持上一次状态
  const connected = failures >= FAILURE_THRESHOLD ? false : (prev.connected ?? true)
  connectionStatus.value[url] = {
    connected,
    consecutiveFailures: failures,
    lastSuccessAt: prev.lastSuccessAt || 0,
  }
}

// ── 内部：根据 lastSuccessAt 二次校验 ──────────────────────
function refreshConnectionByTime() {
  const now = Date.now()
  for (const [url, status] of Object.entries(connectionStatus.value)) {
    if (status.connected && status.lastSuccessAt && (now - status.lastSuccessAt > STALE_THRESHOLD_MS)) {
      connectionStatus.value[url] = { ...status, connected: false }
    }
  }
}

// ── 核心：单次轮询 ─────────────────────────────────────────
async function pollOnce() {
  const results = await Promise.allSettled(
    PLC_ENDPOINTS.map(({ url }) =>
      request.get(url, { timeout: 8000 }).then(res => ({ url, res }))
    )
  )

  const devices = []

  for (let i = 0; i < results.length; i++) {
    const { url, location } = PLC_ENDPOINTS[i]
    const result = results[i]

    if (result.status === 'fulfilled' && result.value?.res?.data?.success) {
      const data = result.value.res.data.data
      endpointData.value[url] = data
      markSuccess(url)

      // 收集设备信息
      if (data?.devices) {
        data.devices.forEach(d => {
          devices.push({
            deviceCode: d.code,
            deviceLocation: location,
            isFault: !!d.fault,
            faultType: d.fault ? '设备故障' : '',
            source: url,
          })
        })
      }
    } else {
      markFailure(url)
    }
  }

  // 二次校验：距离上次成功超过阈值的也标记断开
  refreshConnectionByTime()

  allDevices.value = devices
}

// ── 自适应轮询调度 ─────────────────────────────────────────
function scheduleNext() {
  // 找出最差的连续失败数，决定下次间隔
  let maxFailures = 0
  for (const status of Object.values(connectionStatus.value)) {
    if ((status.consecutiveFailures || 0) > maxFailures) {
      maxFailures = status.consecutiveFailures
    }
  }
  const interval = maxFailures >= 5 ? 10000 : maxFailures >= 2 ? 6000 : 5000

  pollTimer = setTimeout(async () => {
    if (!isPolling) return
    await pollOnce()
    scheduleNext()
  }, interval)
}

// ── 公开 API ───────────────────────────────────────────────

/**
 * 启动轮询（多次调用安全，不会重复启动）
 */
function startPolling() {
  if (isPolling) return
  isPolling = true
  pollOnce()
  scheduleNext()
}

/**
 * 停止轮询
 */
function stopPolling() {
  isPolling = false
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

/**
 * 判断某个端点是否已连接（供页面直接使用）
 */
function isEndpointConnected(url) {
  const status = connectionStatus.value[url]
  if (!status) return false
  // 双重判定：consecutiveFailures + lastSuccessAt
  if (status.consecutiveFailures >= FAILURE_THRESHOLD) return false
  if (status.lastSuccessAt && (Date.now() - status.lastSuccessAt > STALE_THRESHOLD_MS)) return false
  return status.connected ?? false
}

/**
 * 获取所有故障设备列表
 */
function getFaultDevices() {
  return allDevices.value.filter(d => d.isFault)
}

/**
 * Vue composable 入口
 */
export function usePlcStatus() {
  return {
    // 状态（只读）
    endpointData: readonly(endpointData),
    connectionStatus: readonly(connectionStatus),
    allDevices: readonly(allDevices),

    // 方法
    startPolling,
    stopPolling,
    isEndpointConnected,
    getFaultDevices,
    pollOnce,

    // 常量
    PLC_ENDPOINTS,
    FAILURE_THRESHOLD,
    STALE_THRESHOLD_MS,
  }
}
