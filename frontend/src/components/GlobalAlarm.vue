<template>
  <!-- 左上角常驻告警角标 -->
  <Transition name="badge-pop">
    <div v-if="activeAlarms.length > 0 && !visible" class="alarm-badge" @click="visible = true">
      <span class="badge-dot"></span>
      <span class="badge-icon">⚠</span>
      <span class="badge-count">{{ activeAlarms.length }}</span>
    </div>
  </Transition>

  <!-- 弹窗 -->
  <Transition name="alarm-pop">
    <div v-if="visible" class="global-alarm-mask" @click.self="dismiss">
      <div class="global-alarm-popup">
        <div class="alarm-glow"></div>
        <div class="alarm-icon">⚠</div>
        <div class="alarm-content">
          <div class="alarm-title">设备故障告警</div>
          <div class="alarm-list">
            <div v-for="item in activeAlarms" :key="item.id" class="alarm-item">
              <span class="alarm-dot"></span>
              <span class="alarm-device">{{ item.deviceName }}</span>
              <span class="alarm-loc">{{ item.deviceLocation }}</span>
              <span class="alarm-time">{{ item.changeTime }}</span>
            </div>
          </div>
        </div>
        <button class="alarm-close" @click="dismiss">确认</button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import request from '@/api/request'

const visible = ref(false)
const activeAlarms = ref([])
const LEGACY_STORAGE_KEY = 'sewage_fault_alarms'
const DISMISSED_KEY = 'global_alarm_dismissed'
let timer = null

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

const isDemoAlarm = row => {
  const text = [row?.device_name, row?.deviceName, row?.device_code, row?.deviceCode]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return text.includes('粗格栅除污机') || text.includes('演示') || text.includes('测试') || text.includes('demo')
}

const normalizeCurrentFault = row => {
  const rawName = row.device_name || row.deviceName || ''
  const deviceName = /[\u4e00-\u9fff]/.test(rawName) ? rawName : '未知设备'
  const plcCode = String(row.plc_code || row.plcCode || '').toLowerCase()
  return {
    id: row.id || `${plcCode}-${row.device_code || row.deviceCode}-${row.last_change_time || row.lastChangeTime}`,
    deviceName,
    deviceLocation: PLC_LOCATIONS[plcCode] || '未知区域',
    changeTime: formatTime(row.last_change_time || row.lastChangeTime),
  }
}

const checkAndShow = () => {
  const dismissed = localStorage.getItem(DISMISSED_KEY)
  const dismissedTime = dismissed ? parseInt(dismissed) : 0
  const hasNew = activeAlarms.value.length > 0 && activeAlarms.value.some(a => {
    const t = new Date(a.changeTime.replace(' ', 'T')).getTime()
    return t > dismissedTime
  })
  if (hasNew) {
    visible.value = true
  }
}

const dismiss = () => {
  visible.value = false
  localStorage.setItem(DISMISSED_KEY, String(Date.now()))
}

const removeLegacyDemoAlarms = () => {
  try {
    const rows = JSON.parse(localStorage.getItem(LEGACY_STORAGE_KEY) || '[]')
    const cleaned = Array.isArray(rows) ? rows.filter(row => !isDemoAlarm(row)) : []
    localStorage.setItem(LEGACY_STORAGE_KEY, JSON.stringify(cleaned))
  } catch {
    localStorage.removeItem(LEGACY_STORAGE_KEY)
  }
}

const fetchCurrentFaults = async () => {
  try {
    const response = await request.get('/monitor/faults/current', { params: { limit: 1000 } })
    activeAlarms.value = unwrapRows(response).filter(row => !isDemoAlarm(row)).map(normalizeCurrentFault)
    checkAndShow()
  } catch (error) {
    console.error('左上角当前故障读取失败', error)
  }
}

onMounted(() => {
  removeLegacyDemoAlarms()
  fetchCurrentFaults()
  timer = window.setInterval(fetchCurrentFaults, 5000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
/* 左上角常驻告警角标 */
.alarm-badge {
  position: fixed;
  top: 8px;
  left: 8px;
  z-index: 9998;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border: 1px solid rgba(255, 60, 60, 0.5);
  background: linear-gradient(135deg, rgba(60, 10, 10, 0.92), rgba(30, 5, 5, 0.95));
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 0 26px rgba(255, 60, 60, 0.3), 0 5px 14px rgba(0, 0, 0, 0.35);
  animation: badge-glow 2s ease-in-out infinite;
  transition: all 0.2s ease;
}

.alarm-badge:hover {
  border-color: rgba(255, 100, 100, 0.7);
  box-shadow: 0 0 36px rgba(255, 60, 60, 0.4), 0 5px 18px rgba(0, 0, 0, 0.45);
  transform: scale(1.05);
}

.badge-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ff4040;
  box-shadow: 0 0 10px rgba(255, 60, 60, 0.7);
  animation: dot-blink 1s ease-in-out infinite;
}

.badge-icon {
  font-size: 1.25rem;
  animation: alarm-shake 0.5s ease-in-out infinite alternate;
}

.badge-count {
  font-size: 1rem;
  font-weight: 700;
  color: #ff8080;
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 60, 60, 0.2);
  border-radius: 50%;
  text-shadow: 0 0 6px rgba(255, 60, 60, 0.4);
}

@keyframes badge-glow {
  0%, 100% { box-shadow: 0 0 26px rgba(255,60,60,0.3), 0 5px 14px rgba(0,0,0,0.35); }
  50% { box-shadow: 0 0 38px rgba(255,60,60,0.42), 0 5px 14px rgba(0,0,0,0.35); }
}

.badge-pop-enter-active { transition: all 0.3s ease-out; }
.badge-pop-leave-active { transition: all 0.2s ease-in; }
.badge-pop-enter-from { opacity: 0; transform: scale(0.8); }
.badge-pop-leave-to { opacity: 0; transform: scale(0.8); }

.global-alarm-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
}

.global-alarm-popup {
  position: relative;
  width: 460px;
  max-width: 90vw;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  border: 2px solid rgba(255, 60, 60, 0.6);
  background: linear-gradient(135deg, rgba(40, 10, 10, 0.95), rgba(20, 5, 5, 0.98));
  border-radius: 12px;
  box-shadow:
    0 0 40px rgba(255, 60, 60, 0.3),
    0 0 80px rgba(255, 60, 60, 0.15),
    inset 0 0 30px rgba(255, 60, 60, 0.05);
  overflow: hidden;
  animation: alarm-pulse 2s ease-in-out infinite;
}

.alarm-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 0%, rgba(255, 60, 60, 0.15), transparent 60%);
  pointer-events: none;
}

.alarm-icon {
  text-align: center;
  font-size: 2.25rem;
  padding: 16px 0 8px;
  animation: alarm-shake 0.5s ease-in-out infinite alternate;
}

.alarm-content {
  padding: 0 20px 12px;
  overflow-y: auto;
}

.alarm-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #ff6060;
  text-align: center;
  margin-bottom: 12px;
  text-shadow: 0 0 12px rgba(255, 60, 60, 0.5);
  letter-spacing: 0.08em;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alarm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid rgba(255, 80, 80, 0.25);
  background: rgba(255, 60, 60, 0.06);
  border-radius: 6px;
}

.alarm-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4040;
  flex-shrink: 0;
  box-shadow: 0 0 8px rgba(255, 60, 60, 0.6);
  animation: dot-blink 1s ease-in-out infinite;
}

.alarm-device {
  font-size: 0.875rem;
  font-weight: 700;
  color: #ffb0b0;
  white-space: nowrap;
}

.alarm-loc {
  font-size: 0.75rem;
  color: #cc8080;
  flex: 1;
  text-align: right;
}

.alarm-time {
  font-size: 0.6875rem;
  color: #996060;
  white-space: nowrap;
}

.alarm-close {
  margin: 12px 20px 16px;
  padding: 10px 0;
  border: 1px solid rgba(255, 100, 100, 0.4);
  background: rgba(255, 60, 60, 0.12);
  color: #ff9090;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  letter-spacing: 0.06em;
}

.alarm-close:hover {
  background: rgba(255, 60, 60, 0.25);
  border-color: rgba(255, 100, 100, 0.6);
  color: #ffb0b0;
}

@keyframes alarm-pulse {
  0%, 100% { box-shadow: 0 0 40px rgba(255,60,60,0.3), 0 0 80px rgba(255,60,60,0.15); }
  50% { box-shadow: 0 0 50px rgba(255,60,60,0.4), 0 0 100px rgba(255,60,60,0.2); }
}

@keyframes alarm-shake {
  0% { transform: rotate(-3deg); }
  100% { transform: rotate(3deg); }
}

@keyframes dot-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.alarm-pop-enter-active { transition: all 0.3s ease-out; }
.alarm-pop-leave-active { transition: all 0.2s ease-in; }
.alarm-pop-enter-from { opacity: 0; }
.alarm-pop-enter-from .global-alarm-popup { transform: scale(0.9); }
.alarm-pop-leave-to { opacity: 0; }
.alarm-pop-leave-to .global-alarm-popup { transform: scale(0.95); }
</style>
