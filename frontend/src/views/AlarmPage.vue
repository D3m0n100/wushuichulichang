<template>
  <div class="alarm-page">
    <div class="wave-overlay"></div>

    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-deco left"></div>
        <div class="header-title-group">
          <span class="title-text">设备故障告警监控</span>
          <span class="title-en">FAULT ALARM MONITOR</span>
        </div>
        <div class="header-deco right"></div>
      </div>
      <div class="header-stats">
        <div class="stat-chip danger">
          <span class="chip-dot"></span>
          <span class="chip-num">{{ activeFaultCount }}</span>
          <span class="chip-label">故障中</span>
        </div>
        <div class="stat-chip warning">
          <span class="chip-dot"></span>
          <span class="chip-num">{{ historyFaultCount }}</span>
          <span class="chip-label">历史记录</span>
        </div>
        <button class="export-btn" @click="handleExportExcel">导出 Excel</button>
      </div>
    </div>

    <!-- Table -->
    <div class="table-card">
      <div class="frame-corner tl"></div>
      <div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div>
      <div class="frame-corner br"></div>

      <div class="card-inner">
        <div class="alarm-tabs" role="tablist" aria-label="故障数据分栏">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'current' }"
            @click="activeTab = 'current'"
          >
            当前故障 <span>{{ activeFaultCount }}</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'history' }"
            @click="activeTab = 'history'"
          >
            历史故障 <span>{{ historyFaultCount }}</span>
          </button>
          <div class="data-source-state" :class="{ loading, disconnected: !loading && !databaseConnected }">
            <span class="source-dot"></span>{{ loading ? '正在读取数据库' : databaseConnected ? '数据库已连接' : '数据库连接失败' }}
          </div>
        </div>

        <div class="table-scroll">
          <table v-if="activeTab === 'current'" class="alarm-table">
            <thead>
              <tr>
                <th class="col-id">记录编号</th>
                <th class="col-device">设备编号</th>
                <th class="col-location">设备地点</th>
                <th class="col-name">设备名称</th>
                <th class="col-type">设备类型</th>
                <th class="col-status">状态</th>
                <th class="col-time">状态变化时间</th>
                <th class="col-time">最后读取时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!loading && currentFaults.length === 0">
                <td colspan="8" class="empty-row">
                  <div class="empty-state">
                    <span class="empty-icon">◈</span>
                    <span class="empty-text">当前无活动设备故障</span>
                  </div>
                </td>
              </tr>
              <tr
                v-for="item in currentFaults"
                :key="`current-${item.id}`"
                class="alarm-row"
              >
                <td class="col-id"><span class="alarm-badge">FLT-{{ item.id }}</span></td>
                <td class="col-device">{{ item.deviceCode }}</td>
                <td class="col-location">{{ item.deviceLocation }}</td>
                <td class="col-name">{{ item.deviceName }}</td>
                <td class="col-type">{{ item.deviceType }}</td>
                <td class="col-status">
                  <span class="status-pill active">故障中</span>
                </td>
                <td class="col-time">{{ item.changeTime }}</td>
                <td class="col-time">{{ item.lastSeenTime }}</td>
              </tr>
            </tbody>
          </table>

          <table v-else class="alarm-table history-table">
            <thead>
              <tr>
                <th class="col-id">事件编号</th>
                <th class="col-device">设备编号</th>
                <th class="col-location">设备地点</th>
                <th class="col-name">设备名称</th>
                <th class="col-type">设备类型</th>
                <th class="col-status">事件</th>
                <th class="col-time">事件时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!loading && faultHistory.length === 0">
                <td colspan="7" class="empty-row">
                  <div class="empty-state">
                    <span class="empty-icon">◈</span>
                    <span class="empty-text">暂无历史故障事件</span>
                  </div>
                </td>
              </tr>
              <tr
                v-for="item in faultHistory"
                :key="`history-${item.id}`"
                class="alarm-row"
                :class="{ 'row-recovered': item.eventType === 'RECOVERED' }"
              >
                <td class="col-id"><span class="alarm-badge">EVT-{{ item.id }}</span></td>
                <td class="col-device">{{ item.deviceCode }}</td>
                <td class="col-location">{{ item.deviceLocation }}</td>
                <td class="col-name">{{ item.deviceName }}</td>
                <td class="col-type">{{ item.deviceType }}</td>
                <td class="col-status">
                  <span class="status-pill" :class="item.eventType === 'ACTIVE' ? 'active' : 'recovered'">
                    {{ item.eventLabel }}
                  </span>
                </td>
                <td class="col-time">{{ item.eventTime }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import request from '@/api/request'

const activeTab = ref('current')
const currentFaults = ref([])
const faultHistory = ref([])
const loading = ref(false)
const databaseConnected = ref(false)
let timer = null
let errorShown = false

const activeFaultCount = computed(() => currentFaults.value.length)
const historyFaultCount = computed(() => faultHistory.value.length)

const PLC_LOCATIONS = {
  'coarse-screen': '粗格栅',
  'fine-screen': '细格栅',
  'multilayer-cyclone': '旋流沉砂池',
  pac: 'PAC加药间',
  pam: 'PAM加药间',
  'sodium-acetate': '乙酸钠加药间',
  'sodium-hypochlorite': '次氯酸钠加药间',
  'baike-dosing': '百科加药系统',
  'baike-filter-press': '脱水车间',
  'baike-fiber-disc': '滤布滤池',
  fan: '鼓风机房',
  'sludge-silo': '污泥料仓',
  deodorization: '生物除臭系统',
  'rotor-pump': '转子泵系统',
  b1: '调节池',
  h1: 'AAO生物池',
  m3: '高效沉淀池',
}

const DEVICE_TYPES = {
  motor: '电机', pump: '水泵', valve: '阀门', fan: '风机',
  mixer: '搅拌机', conveyor: '输送机', feeder: '投料机',
}

const unwrapRows = (res) => {
  const body = res?.data ?? res
  if (Array.isArray(body)) return body
  if (Array.isArray(body?.data)) return body.data
  if (Array.isArray(body?.records)) return body.records
  return []
}

const formatTime = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  let date
  if (Array.isArray(value)) {
    date = new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0)
  } else if (typeof value === 'number') {
    date = new Date(value)
  } else {
    date = new Date(String(value).replace(' ', 'T'))
  }
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const normalizeDeviceType = type => DEVICE_TYPES[String(type || '').toLowerCase()] || type || '设备'
const getLocation = plcCode => PLC_LOCATIONS[String(plcCode || '').toLowerCase()] || plcCode || '未知区域'

const normalizeCurrentFault = row => ({
  id: row.id,
  deviceCode: row.device_code || row.deviceCode || '-',
  deviceName: row.device_name || row.deviceName || row.device_code || '未知设备',
  deviceType: normalizeDeviceType(row.device_type || row.deviceType),
  deviceLocation: getLocation(row.plc_code || row.plcCode),
  changeTime: formatTime(row.last_change_time || row.lastChangeTime),
  lastSeenTime: formatTime(row.last_seen_time || row.lastSeenTime),
})

const normalizeHistoryFault = row => {
  const eventType = String(row.event_type || row.eventType || '').toUpperCase()
  return {
    id: row.id,
    deviceCode: row.device_code || row.deviceCode || '-',
    deviceName: row.device_name || row.deviceName || row.device_code || '未知设备',
    deviceType: normalizeDeviceType(row.device_type || row.deviceType),
    deviceLocation: getLocation(row.plc_code || row.plcCode),
    eventType,
    eventLabel: eventType === 'RECOVERED' ? '故障恢复' : '故障发生',
    eventTime: formatTime(row.event_time || row.eventTime),
  }
}

const fetchFaultData = async () => {
  loading.value = true
  try {
    const [currentRes, historyRes] = await Promise.all([
      request.get('/monitor/faults/current', { params: { limit: 1000 } }),
      request.get('/monitor/faults/history', { params: { limit: 1000 } }),
    ])
    currentFaults.value = unwrapRows(currentRes).map(normalizeCurrentFault)
    faultHistory.value = unwrapRows(historyRes).map(normalizeHistoryFault)
    databaseConnected.value = true
    errorShown = false
  } catch (error) {
    databaseConnected.value = false
    console.error('读取数据库故障数据失败', error)
    if (!errorShown) {
      ElMessage.error('故障数据库接口不可用，请检查后端服务')
      errorShown = true
    }
  } finally {
    loading.value = false
  }
}

const handleExportExcel = () => {
  const isCurrent = activeTab.value === 'current'
  const source = isCurrent ? currentFaults.value : faultHistory.value
  if (!source.length) { ElMessage.warning('当前分栏无故障数据'); return }
  const header = isCurrent
    ? ['记录编号', '设备编号', '设备地点', '设备名称', '设备类型', '状态', '状态变化时间', '最后读取时间']
    : ['事件编号', '设备编号', '设备地点', '设备名称', '设备类型', '事件', '事件时间']
  const rows = isCurrent
    ? source.map(item => [`FLT-${item.id}`, item.deviceCode, item.deviceLocation, item.deviceName, item.deviceType, '故障中', item.changeTime, item.lastSeenTime])
    : source.map(item => [`EVT-${item.id}`, item.deviceCode, item.deviceLocation, item.deviceName, item.deviceType, item.eventLabel, item.eventTime])
  const worksheet = XLSX.utils.aoa_to_sheet([header, ...rows])
  worksheet['!cols'] = header.map((_, index) => ({ wch: index >= header.length - 2 ? 20 : 18 }))
  const workbook = XLSX.utils.book_new()
  const sheetName = isCurrent ? '当前故障' : '历史故障'
  XLSX.utils.book_append_sheet(workbook, worksheet, sheetName)
  XLSX.writeFile(workbook, `${sheetName}_${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.xlsx`)
  ElMessage.success('Excel 导出成功')
}

onMounted(() => { fetchFaultData(); timer = setInterval(fetchFaultData, 10000) })
onBeforeUnmount(() => { clearInterval(timer) })
</script>

<style scoped>
.alarm-page {
  width: 100%;
  height: calc(100vh - var(--nav-height, 106px));
  padding: 16px;
  box-sizing: border-box;
  background:
    radial-gradient(ellipse at 20% 40%, rgba(0, 232, 255, 0.03) 0%, transparent 50%),
    linear-gradient(180deg, var(--color-bg-deep) 0%, var(--color-bg-main) 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  position: relative;
}

.wave-overlay {
  position: absolute; inset: 0;
  background: repeating-linear-gradient(0deg, transparent, transparent 3px, rgba(0, 232, 255, 0.006) 3px, rgba(0, 232, 255, 0.006) 4px);
  pointer-events: none; z-index: 0;
}

/* ========== Header ========== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title-text {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-primary);
  text-shadow: 0 0 12px rgba(0, 232, 255, 0.4);
  letter-spacing: 2px;
}

.title-en {
  font-family: var(--font-display);
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(0, 232, 255, 0.3);
  letter-spacing: 3px;
}

.header-deco {
  width: 50px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-primary-dim));
}

.header-deco.right {
  background: linear-gradient(90deg, var(--color-primary-dim), transparent);
}

.header-stats {
  display: flex;
  gap: 10px;
  align-items: center;
}

.export-btn {
  padding: 6px 16px;
  border: 1px solid rgba(48, 230, 173, 0.4);
  background: rgba(48, 230, 173, 0.08);
  color: #7dffd1;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  letter-spacing: 0.04em;
}

.export-btn:hover {
  border-color: rgba(48, 230, 173, 0.7);
  background: rgba(48, 230, 173, 0.15);
  box-shadow: 0 0 12px rgba(48, 230, 173, 0.15);
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: none;
}

.stat-chip.danger { border-color: rgba(255, 23, 68, 0.2); }
.stat-chip.warning { border-color: rgba(255, 171, 0, 0.2); }

.chip-dot {
  width: 6px; height: 6px; border-radius: 50%;
}

.stat-chip.danger .chip-dot {
  background: var(--color-danger);
  box-shadow: 0 0 6px var(--color-danger);
}

.stat-chip.warning .chip-dot {
  background: var(--color-warning);
  box-shadow: 0 0 6px var(--color-warning);
}


.chip-num {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
}

.stat-chip.danger .chip-num { color: var(--color-danger); text-shadow: 0 0 8px rgba(255,23,68,0.4); }
.stat-chip.warning .chip-num { color: var(--color-warning); text-shadow: 0 0 8px rgba(255,171,0,0.4); }

.chip-label {
  font-size: 0.9375rem;
  color: rgba(224, 234, 245, 0.5);
}

/* ========== Table Card ========== */
.table-card {
  position: relative;
  flex: 1;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  z-index: 1;
}

.frame-corner { position: absolute; width: 12px; height: 12px; z-index: 3; pointer-events: none; }
.frame-corner::before, .frame-corner::after {
  content: ''; position: absolute; background: var(--color-primary); border-radius: 1px;
  box-shadow: 0 0 6px var(--color-primary);
}
.frame-corner.tl { top: 0; left: 0; }
.frame-corner.tl::before { top: 0; left: 0; width: 12px; height: 2px; }
.frame-corner.tl::after { top: 0; left: 0; width: 2px; height: 12px; }
.frame-corner.tr { top: 0; right: 0; }
.frame-corner.tr::before { top: 0; right: 0; width: 12px; height: 2px; }
.frame-corner.tr::after { top: 0; right: 0; width: 2px; height: 12px; }
.frame-corner.bl { bottom: 0; left: 0; }
.frame-corner.bl::before { bottom: 0; left: 0; width: 12px; height: 2px; }
.frame-corner.bl::after { bottom: 0; left: 0; width: 2px; height: 12px; }
.frame-corner.br { bottom: 0; right: 0; }
.frame-corner.br::before { bottom: 0; right: 0; width: 12px; height: 2px; }
.frame-corner.br::after { bottom: 0; right: 0; width: 2px; height: 12px; }

.card-inner {
  position: relative; z-index: 4; padding: 14px;
  height: 100%; box-sizing: border-box;
  display: flex; flex-direction: column;
}

.alarm-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 42px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.12);
}

.tab-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid rgba(0, 232, 255, 0.18);
  border-radius: 4px;
  background: rgba(15, 32, 50, 0.7);
  color: rgba(224, 234, 245, 0.68);
  font-size: 0.9375rem;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.tab-btn span {
  display: inline-block;
  min-width: 20px;
  margin-left: 6px;
  color: rgba(224, 234, 245, 0.5);
  font-family: var(--font-data);
}

.tab-btn.active {
  border-color: rgba(0, 232, 255, 0.55);
  background: rgba(0, 128, 255, 0.16);
  color: var(--color-primary);
  box-shadow: inset 0 -2px 0 var(--color-primary);
}

.tab-btn.active span { color: #fff; }

.data-source-state {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  color: rgba(125, 255, 209, 0.68);
  font-size: 0.8125rem;
}

.source-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 7px rgba(0, 230, 118, 0.7);
}

.data-source-state.loading { color: rgba(255, 171, 0, 0.75); }
.data-source-state.loading .source-dot { background: var(--color-warning); box-shadow: 0 0 7px rgba(255, 171, 0, 0.65); }
.data-source-state.disconnected { color: rgba(255, 122, 122, 0.78); }
.data-source-state.disconnected .source-dot { background: var(--color-danger); box-shadow: 0 0 7px rgba(255, 23, 68, 0.65); }

/* ========== Table ========== */
.table-scroll { flex: 1; min-height: 0; overflow: auto; padding-top: 8px; }

.alarm-table {
  width: 100%; border-collapse: collapse;
  font-size: 1.0625rem; color: #F2F3F5; min-width: 1000px;
}

.alarm-table thead th {
  position: sticky; top: 0;
  background: rgba(30, 41, 59, 0.95);
  color: var(--color-primary); font-weight: 600;
  font-size: 0.9375rem; letter-spacing: 1px;
  text-shadow: 0 0 5px rgba(0, 232, 255, 0.3);
  padding: 10px 8px; border-bottom: 1px solid rgba(0, 232, 255, 0.25);
  text-align: left; white-space: nowrap; z-index: 5;
}

.alarm-table tbody td {
  padding: 10px 8px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.04);
  transition: all 0.2s ease;
}

.alarm-row:hover td { background: rgba(0, 232, 255, 0.05); }
.row-recovered { opacity: 0.5; }

.col-id { width: 180px; }
.col-device { width: 120px; }
.col-location { width: 140px; }
.col-name { width: 180px; }
.col-type { width: 100px; }
.col-fault { width: 140px; }
.col-reason { width: 180px; }
.col-status { width: 100px; }
.col-time { width: 160px; }
.col-action { width: 120px; text-align: center; }

/* Alarm badge */
.alarm-badge {
  display: inline-block; padding: 3px 8px;
  background: rgba(255, 23, 68, 0.08);
  border: 1px solid rgba(255, 23, 68, 0.25);
  color: var(--color-danger);
  font-family: var(--font-data); font-size: 0.875rem;
  font-weight: bold; letter-spacing: 0.5px;
  text-shadow: 0 0 4px rgba(255, 23, 68, 0.3);
  border-radius: 2px;
}

/* Fault tag */
.fault-tag {
  display: inline-block; padding: 3px 8px;
  background: rgba(255, 171, 0, 0.08);
  border: 1px solid rgba(255, 171, 0, 0.2);
  color: var(--color-warning);
  font-size: 0.9375rem; font-weight: 500;
  text-shadow: 0 0 3px rgba(255, 171, 0, 0.2);
  border-radius: 2px;
}

/* Status pill */
.status-pill {
  display: inline-block; padding: 3px 8px;
  font-size: 0.9375rem; font-weight: 600; letter-spacing: 0.5px;
  border-radius: var(--radius-sm);
}

.status-pill.active {
  background: rgba(255, 23, 68, 0.1);
  border: 1px solid rgba(255, 23, 68, 0.3);
  color: var(--color-danger);
  text-shadow: 0 0 5px rgba(255, 23, 68, 0.3);
}

.status-pill.recovered {
  background: rgba(0, 230, 118, 0.08);
  border: 1px solid rgba(0, 230, 118, 0.2);
  color: var(--color-success);
  text-shadow: 0 0 5px rgba(0, 230, 118, 0.3);
}

.col-time {
  font-family: var(--font-data); font-size: 0.9375rem;
  color: rgba(224, 234, 245, 0.5);
}

.text-empty {
  color: rgba(224, 234, 245, 0.3);
  font-style: italic;
}

/* Buttons */
.edit-btn, .save-btn, .cancel-btn {
  padding: 4px 10px;
  font-size: 0.875rem; cursor: pointer;
  transition: all 0.3s ease;
  border-radius: var(--radius-sm);
  margin: 0 2px;
}

.edit-btn {
  background: rgba(0, 128, 255, 0.08);
  border: 1px solid rgba(0, 128, 255, 0.25);
  color: #00e8ff;
}

.edit-btn:hover {
  background: rgba(0, 128, 255, 0.2);
  box-shadow: 0 0 10px rgba(0, 128, 255, 0.2);
}

.save-btn {
  background: rgba(0, 230, 118, 0.08);
  border: 1px solid rgba(0, 230, 118, 0.25);
  color: #00e676;
}

.save-btn:hover {
  background: rgba(0, 230, 118, 0.2);
  box-shadow: 0 0 10px rgba(0, 230, 118, 0.2);
}

.cancel-btn {
  background: rgba(255, 171, 0, 0.08);
  border: 1px solid rgba(255, 171, 0, 0.25);
  color: #ffab00;
}

.cancel-btn:hover {
  background: rgba(255, 171, 0, 0.2);
  box-shadow: 0 0 10px rgba(255, 171, 0, 0.2);
}

/* Empty state */
.empty-row { text-align: center; padding: 60px 0; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.empty-icon { font-size: 1.75rem; color: var(--color-success); text-shadow: 0 0 12px rgba(0, 230, 118, 0.3); }
.empty-text { color: rgba(0, 230, 118, 0.7); font-size: 1.125rem; letter-spacing: 1px; }

/* Scrollbar */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: rgba(0, 15, 30, 0.3); }
::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--color-primary), var(--color-secondary));
  border-radius: 2px;
}
</style>
