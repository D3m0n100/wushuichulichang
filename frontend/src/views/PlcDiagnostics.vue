<template>
  <div class="diag-page">
    <div class="page-header">
      <div class="header-title-group">
        <span class="header-dot"></span>
        <h2 class="header-title">PLC 诊断日志</h2>
        <span class="header-line"></span>
        <span class="header-sub">PLC FAILURE DIAGNOSTICS</span>
      </div>
      <div class="header-actions">
        <span class="log-count">共 {{ failureLogs.length }} 条</span>
        <button class="btn-clear" @click="handleClear">清空日志</button>
      </div>
    </div>

    <div v-if="failureLogs.length === 0" class="empty-state">
      <span class="empty-icon">✅</span>
      <span class="empty-text">暂无失败记录</span>
    </div>

    <div v-else class="log-table-wrap">
      <table class="log-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>PLC</th>
            <th>状态码</th>
            <th>错误信息</th>
            <th>请求地址</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in failureLogs" :key="log.id">
            <td class="col-time">{{ log.timestamp }}</td>
            <td class="col-plc">{{ log.plcName }}</td>
            <td class="col-code" :class="{ timeout: log.statusCode === 0 }">{{ log.statusCode || '超时' }}</td>
            <td class="col-msg">{{ log.errorMessage }}</td>
            <td class="col-url">{{ log.url }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { usePlcDiagnostics } from '@/composables/usePlcDiagnostics'

const { failureLogs, clearLogs } = usePlcDiagnostics()

const handleClear = () => {
  clearLogs()
}
</script>

<style scoped>
.diag-page {
  padding: 16px 20px;
  background: linear-gradient(180deg, #060d1f 0%, #0a1628 40%, #0d1e36 100%);
  min-height: calc(100vh - var(--nav-height, 113px));
  overflow-y: auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-dot {
  width: 7px;
  height: 7px;
  background: var(--color-danger);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--color-danger);
}

.header-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: 2px;
}

.header-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 80, 80, 0.4), transparent);
}

.header-sub {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 2px;
  font-family: var(--font-display);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.log-count {
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.5);
}

.btn-clear {
  padding: 6px 16px;
  font-size: 0.8125rem;
  font-weight: 600;
  border: 1px solid rgba(255, 80, 80, 0.4);
  background: rgba(255, 60, 60, 0.08);
  color: #ff8080;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-clear:hover {
  border-color: rgba(255, 80, 80, 0.7);
  background: rgba(255, 60, 60, 0.18);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 0;
}

.empty-icon {
  font-size: 3rem;
}

.empty-text {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.5);
}

.log-table-wrap {
  overflow-x: auto;
  border: 1px solid rgba(0, 128, 255, 0.15);
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.85);
}

.log-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.log-table th {
  text-align: left;
  padding: 10px 14px;
  color: #00e8ff;
  font-weight: 600;
  font-size: 0.75rem;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(0, 128, 255, 0.25);
  background: rgba(0, 20, 50, 0.5);
  white-space: nowrap;
}

.log-table td {
  padding: 8px 14px;
  border-bottom: 1px solid rgba(0, 128, 255, 0.06);
  color: #c0d8ee;
  vertical-align: top;
}

.log-table tr:hover td {
  background: rgba(0, 128, 255, 0.05);
}

.col-time {
  font-family: var(--font-data);
  font-size: 0.75rem;
  color: rgba(224, 234, 245, 0.6);
  white-space: nowrap;
}

.col-plc {
  font-weight: 700;
  color: #ff8080;
  white-space: nowrap;
}

.col-code {
  font-family: var(--font-data);
  font-weight: 600;
  color: #ffd060;
}

.col-code.timeout {
  color: #ff5050;
}

.col-msg {
  max-width: 300px;
  word-break: break-all;
  color: rgba(255, 120, 120, 0.8);
}

.col-url {
  font-family: var(--font-data);
  font-size: 0.75rem;
  color: rgba(0, 232, 255, 0.5);
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
