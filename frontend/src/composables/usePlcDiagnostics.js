/**
 * PLC 诊断日志 — 记录所有读取失败的 PLC 信息
 */
import { ref } from 'vue'

// 模块级单例，所有组件共享
const failureLogs = ref([])
const MAX_LOGS = 500

/**
 * 记录一次 PLC 读取失败
 */
function logFailure(plcName, url, statusCode, errorMessage) {
  const now = new Date()
  const pad = (n) => n.toString().padStart(2, '0')
  const timestamp = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

  failureLogs.value.unshift({
    id: Date.now() + Math.random(),
    timestamp,
    plcName,
    url,
    statusCode,
    errorMessage: errorMessage || '未知错误',
  })

  if (failureLogs.value.length > MAX_LOGS) {
    failureLogs.value = failureLogs.value.slice(0, MAX_LOGS)
  }
}

function clearLogs() {
  failureLogs.value = []
}

export function usePlcDiagnostics() {
  return {
    failureLogs,
    logFailure,
    clearLogs,
  }
}
