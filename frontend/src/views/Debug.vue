<template>
  <div class="debug-page">
    <div class="wave-overlay"></div>

    <div class="page-header">
      <div class="header-title-group">
        <span class="header-dot"></span>
        <h2 class="header-title">联动调试</h2>
        <span class="header-line"></span>
        <span class="header-sub">LINKAGE DEBUG</span>
      </div>
    </div>

    <div class="debug-content">
      <!-- PLC连接状态 -->
      <div class="section-card">
        <div class="section-title">PLC连接状态</div>
        <div class="status-row">
          <span class="status-label">M3 PLC</span>
          <span class="status-dot" :class="m3Connected ? 'ok' : 'err'"></span>
          <span :class="m3Connected ? 'text-ok' : 'text-err'">{{ m3Connected ? '已连接' : '未连接' }}</span>
        </div>
      </div>

      <!-- 设备读取状态 -->
      <div class="section-card">
        <div class="section-title">设备读取状态 (DB1)</div>
        <div class="data-table">
          <div class="dt-header">
            <span>设备编号</span><span>设备名称</span><span>远程</span><span>运行/开到位</span><span>故障</span><span>手自动</span><span>频率反馈</span><span>频率给定</span>
          </div>
          <div v-for="d in deviceList" :key="d.code" class="dt-row" :class="{ 'row-err': hasDeviceError(d.code) }">
            <span>{{ d.code }}</span>
            <span>{{ d.name }}</span>
            <span :class="fieldClass(d.remote)">{{ fmtBool(d.remote) }}</span>
            <span :class="fieldClass(d.running)">{{ fmtBool(d.running) }}</span>
            <span :class="fieldClass(d.fault)">{{ fmtBool(d.fault) }}</span>
            <span :class="fieldClass(d.manualModeBit)">{{ fmtBool(d.manualModeBit) }}</span>
            <span :class="fieldClass(d.frequencyFeedback)">{{ fmtNum(d.frequencyFeedback) }}</span>
            <span :class="fieldClass(d.frequencySetpoint)">{{ fmtNum(d.frequencySetpoint) }}</span>
          </div>
        </div>
      </div>

      <!-- 仪表读取状态 -->
      <div class="section-card">
        <div class="section-title">仪表读取状态 (DB2)</div>
        <div class="data-table">
          <div class="dt-header">
            <span>仪表编号</span><span>仪表名称</span><span>输出值</span><span>上限值</span><span>下限值</span><span>偏差值</span>
          </div>
          <div v-for="inst in instrumentList" :key="inst.code" class="dt-row" :class="{ 'row-err': hasInstrumentError(inst.code) }">
            <span>{{ inst.code }}</span>
            <span>{{ inst.name }}</span>
            <span :class="fieldClass(inst.output)">{{ fmtNum(inst.output) }}</span>
            <span :class="fieldClass(inst.upperLimit)">{{ fmtNum(inst.upperLimit) }}</span>
            <span :class="fieldClass(inst.lowerLimit)">{{ fmtNum(inst.lowerLimit) }}</span>
            <span :class="fieldClass(inst.deviation)">{{ fmtNum(inst.deviation) }}</span>
          </div>
        </div>
      </div>

      <!-- 联动计时参数 -->
      <div class="section-card">
        <div class="section-title">联动计时参数 (DB3)</div>
        <div class="data-table">
          <div class="dt-header">
            <span>计时器</span><span>已运行时</span><span>已运行分</span><span>已运行秒</span><span>已停止时</span><span>已停止分</span><span>已停止秒</span><span>运行设定</span><span>停止设定</span>
          </div>
          <div v-for="t in timerList" :key="t.code" class="dt-row" :class="{ 'row-err': hasTimerError(t.code) }">
            <span>{{ t.code }}</span>
            <span :class="fieldClass(t.runElapsedHours)">{{ fmtNum(t.runElapsedHours) }}</span>
            <span :class="fieldClass(t.runElapsedMinutes)">{{ fmtNum(t.runElapsedMinutes) }}</span>
            <span :class="fieldClass(t.runElapsedSeconds)">{{ fmtNum(t.runElapsedSeconds) }}</span>
            <span :class="fieldClass(t.stopElapsedHours)">{{ fmtNum(t.stopElapsedHours) }}</span>
            <span :class="fieldClass(t.stopElapsedMinutes)">{{ fmtNum(t.stopElapsedMinutes) }}</span>
            <span :class="fieldClass(t.stopElapsedSeconds)">{{ fmtNum(t.stopElapsedSeconds) }}</span>
            <span :class="fieldClass(t.runSetHours)">{{ t.runSetHours ?? '-' }}:{{ t.runSetMinutes ?? '-' }}:{{ t.runSetSeconds ?? '-' }}</span>
            <span :class="fieldClass(t.stopSetHours)">{{ t.stopSetHours ?? '-' }}:{{ t.stopSetMinutes ?? '-' }}:{{ t.stopSetSeconds ?? '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 逻辑配置 -->
      <div class="section-card">
        <div class="section-title">逻辑配置参数</div>
        <div class="data-table">
          <div class="dt-header">
            <span>配置编号</span><span>名称</span><span>启动液位</span><span>停止液位</span><span>延时分</span><span>延时秒</span>
          </div>
          <div v-for="lc in logicConfigList" :key="lc.code" class="dt-row" :class="{ 'row-err': hasLogicError(lc.code) }">
            <span>{{ lc.code }}</span>
            <span>{{ lc.name }}</span>
            <span :class="fieldClass(lc.startLevel)">{{ fmtNum(lc.startLevel) }}</span>
            <span :class="fieldClass(lc.stopLevel)">{{ fmtNum(lc.stopLevel) }}</span>
            <span :class="fieldClass(lc.delayMinutes)">{{ fmtNum(lc.delayMinutes) }}</span>
            <span :class="fieldClass(lc.delaySeconds)">{{ fmtNum(lc.delaySeconds) }}</span>
          </div>
        </div>
      </div>

      <!-- 错误汇总 -->
      <div class="section-card" v-if="totalErrors > 0">
        <div class="section-title" style="color:#ff7a7a">⚠ 读取异常汇总 ({{ totalErrors }} 项)</div>
        <div class="error-list">
          <div v-for="(err, i) in allErrors" :key="i" class="error-item">
            <span class="err-code">{{ err.code }}</span>
            <span class="err-field">{{ err.field }}</span>
            <span class="err-desc">未读到数据</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/api/request'

const m3Connected = ref(false)
const overviewData = ref({ devices: [], instruments: [], timers: [], logicConfigs: [] })
const m3Errors = ref({ devices: [], instruments: [], timers: [], logicConfigs: [] })
let pollTimer = null

const fetchOverview = async () => {
  try {
    const res = await request.get('/plc/m3/overview')
    if (!res.data?.success) { m3Connected.value = false; return }
    m3Connected.value = true
    const data = res.data.data
    overviewData.value = {
      devices: data.devices || [],
      instruments: data.instruments || [],
      timers: data.timers || [],
      logicConfigs: data.logicConfigs || []
    }
    // 检测错误
    const errors = { devices: [], instruments: [], timers: [], logicConfigs: [] }
    data.devices?.forEach(d => {
      if (d.remote === null || d.remote === undefined) errors.devices.push({ code: d.code, field: 'remote' })
      if (d.fault === null || d.fault === undefined) errors.devices.push({ code: d.code, field: 'fault' })
    })
    data.instruments?.forEach(inst => {
      if (inst.output === null || inst.output === undefined) errors.instruments.push({ code: inst.code, field: 'output' })
    })
    data.timers?.forEach(t => {
      if (t.runSetHours === null && t.runSetMinutes === null && t.runSetSeconds === null) errors.timers.push({ code: t.code, field: 'runSet' })
    })
    data.logicConfigs?.forEach(lc => {
      if (lc.startLevel === null || lc.startLevel === undefined) errors.logicConfigs.push({ code: lc.code, field: 'startLevel' })
    })
    m3Errors.value = errors
  } catch (e) {
    m3Connected.value = false
  }
}

const deviceList = computed(() => overviewData.value.devices)
const instrumentList = computed(() => overviewData.value.instruments)
const timerList = computed(() => overviewData.value.timers)
const logicConfigList = computed(() => overviewData.value.logicConfigs)

const hasDeviceError = (code) => m3Errors.value.devices.some(e => e.code === code)
const hasInstrumentError = (code) => m3Errors.value.instruments.some(e => e.code === code)
const hasTimerError = (code) => m3Errors.value.timers.some(e => e.code === code)
const hasLogicError = (code) => m3Errors.value.logicConfigs.some(e => e.code === code)

const allErrors = computed(() => [
  ...m3Errors.value.devices,
  ...m3Errors.value.instruments,
  ...m3Errors.value.timers,
  ...m3Errors.value.logicConfigs
])
const totalErrors = computed(() => allErrors.value.length)

const fieldClass = (val) => (val === null || val === undefined) ? 'val-err' : 'val-ok'
const fmtBool = (val) => (val === null || val === undefined) ? '—' : val ? '是' : '否'
const fmtNum = (val) => (val === null || val === undefined) ? '—' : val

onMounted(() => {
  fetchOverview()
  pollTimer = setInterval(fetchOverview, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.debug-page {
  position: relative;
  min-height: calc(100vh - var(--nav-height, 132px));
  padding: 20px 24px;
  background: linear-gradient(180deg, #1E293B 0%, #1E293B 100%);
  overflow: hidden;
}

.wave-overlay {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background: repeating-linear-gradient(0deg, transparent 0 2px, rgba(0, 232, 255, 0.015) 2px 4px);
  mask-image: linear-gradient(180deg, rgba(255,255,255,0.6), transparent 90%);
}

.page-header { position: relative; z-index: 1; margin-bottom: 16px; }
.header-title-group { display: flex; align-items: center; gap: 10px; }
.header-dot { width: 8px; height: 8px; border-radius: 50%; background: #00e8ff; box-shadow: 0 0 8px rgba(0,232,255,0.5); }
.header-title { font-size: 1.5rem; font-weight: 700; color: #F2F3F5; margin: 0; }
.header-line { width: 40px; height: 2px; background: linear-gradient(90deg, rgba(0,128,255,0.6), transparent); }
.header-sub { font-size: 0.9375rem; color: rgba(140,238,255,0.6); letter-spacing: 2px; }

.debug-content {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; gap: 16px;
}

.section-card {
  border: 1px solid rgba(0, 128, 255, 0.22);
  background: rgba(15, 23, 42, 0.88);
  padding: 14px;
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
}

.section-title {
  font-size: 1rem; font-weight: 700; color: #8ceeff; margin-bottom: 12px;
}

.status-row {
  display: flex; align-items: center; gap: 8px; font-size: 0.9375rem;
}
.status-label { color: #C9CDD4; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; }
.status-dot.ok { background: #30e6ad; box-shadow: 0 0 8px rgba(48,230,173,0.6); }
.status-dot.err { background: #ff7a7a; box-shadow: 0 0 8px rgba(255,122,122,0.6); }
.text-ok { color: #88ffd8; }
.text-err { color: #ffaeae; }

.data-table {
  overflow-x: auto;
}

.dt-header, .dt-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(80px, 1fr));
  gap: 4px;
  padding: 6px 8px;
  font-size: 0.8125rem;
  align-items: center;
}

.dt-header {
  color: rgba(0, 232, 255, 0.7);
  font-weight: 600;
  border-bottom: 1px solid rgba(0, 128, 255, 0.15);
  font-size: 0.75rem;
  letter-spacing: 0.04em;
}

.dt-row {
  border-bottom: 1px solid rgba(0, 128, 255, 0.06);
  color: #C9CDD4;
}

.dt-row:hover { background: rgba(0, 232, 255, 0.04); }
.dt-row.row-err { background: rgba(255, 122, 122, 0.06); border-left: 3px solid rgba(255, 122, 122, 0.5); }

.val-ok { color: #88ffd8; }
.val-err { color: #ff7a7a; font-weight: 700; }

.error-list {
  display: flex; flex-direction: column; gap: 4px;
}

.error-item {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 10px;
  border-left: 3px solid rgba(255, 122, 122, 0.6);
  background: rgba(255, 122, 122, 0.06);
  border-radius: 4px;
  font-size: 0.8125rem;
}

.err-code { color: #ffaeae; font-weight: 600; min-width: 140px; }
.err-field { color: #ffd68f; min-width: 100px; }
.err-desc { color: #C9CDD4; }
</style>
