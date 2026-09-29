<template>
  <div class="m3-page">
    <div class="scene-background"></div>

    <div class="page-aurora aurora-left"></div>
    <div class="page-aurora aurora-right"></div>
    <div class="page-noise"></div>
    <div class="page-vignette"></div>
    <div class="page-gridlines"></div>
    <div class="page-flare flare-top"></div>
    <div class="page-flare flare-bottom"></div>
    <div class="hud-node node-top-left"></div>
    <div class="hud-node node-top-right"></div>
    <div class="hud-node node-bottom-left"></div>
    <div class="hud-node node-bottom-right"></div>

    <div v-if="!isMobileDevice" class="model-control-toggle" @click="showControlPanel = !showControlPanel">
      <span>{{ showControlPanel ? '收起' : '模型' }}</span>
    </div>
    <Transition name="panel-slide">
      <div v-if="showControlPanel && !isMobileDevice" class="model-control-panel">
        <div class="control-title">模型调节</div>
        <div class="control-row"><label>视角锁定</label><button class="ctrl-btn" :class="{ locked: cameraLock }" @click="toggleCameraLock">{{ cameraLock ? '已锁定' : '未锁定' }}</button></div>
        <div class="control-divider-title">中心点</div>
        <div class="control-row"><label>Target X</label><input type="range" :min="-200" :max="200" :step="1" v-model.number="sliderTargetX" @input="updateTargetFromSlider" /><span class="slider-val">{{ sliderTargetX.toFixed(0) }}</span></div>
        <div class="control-row"><label>Target Y</label><input type="range" :min="-200" :max="200" :step="1" v-model.number="sliderTargetY" @input="updateTargetFromSlider" /><span class="slider-val">{{ sliderTargetY.toFixed(0) }}</span></div>
        <div class="control-row"><label>Target Z</label><input type="range" :min="-200" :max="200" :step="1" v-model.number="sliderTargetZ" @input="updateTargetFromSlider" /><span class="slider-val">{{ sliderTargetZ.toFixed(0) }}</span></div>
        <div class="control-divider-title">摄像机位置</div>
        <div class="control-row"><label>Cam X</label><input type="range" :min="-500" :max="500" :step="1" v-model.number="sliderCamX" @input="updateCameraPosFromSlider" /><span class="slider-val">{{ sliderCamX.toFixed(0) }}</span></div>
        <div class="control-row"><label>Cam Y</label><input type="range" :min="-500" :max="500" :step="1" v-model.number="sliderCamY" @input="updateCameraPosFromSlider" /><span class="slider-val">{{ sliderCamY.toFixed(0) }}</span></div>
        <div class="control-row"><label>Cam Z</label><input type="range" :min="-500" :max="500" :step="1" v-model.number="sliderCamZ" @input="updateCameraPosFromSlider" /><span class="slider-val">{{ sliderCamZ.toFixed(0) }}</span></div>
        <div class="control-divider-title">模型位置</div>
        <div class="control-row"><label>Model X</label><input type="range" :min="-500" :max="500" :step="1" v-model.number="sliderModelX" @input="updateModelPosFromSlider" /><span class="slider-val">{{ sliderModelX.toFixed(0) }}</span></div>
        <div class="control-row"><label>Model Y</label><input type="range" :min="-500" :max="500" :step="1" v-model.number="sliderModelY" @input="updateModelPosFromSlider" /><span class="slider-val">{{ sliderModelY.toFixed(0) }}</span></div>
        <div class="control-row"><label>Model Z</label><input type="range" :min="-500" :max="500" :step="1" v-model.number="sliderModelZ" @input="updateModelPosFromSlider" /><span class="slider-val">{{ sliderModelZ.toFixed(0) }}</span></div>
        <div class="control-divider-title">视角</div>
        <div class="control-row"><label>水平角 α</label><input type="range" :min="0" :max="6.28" :step="0.01" v-model.number="sliderAlpha" @input="updateCameraFromSlider" /><span class="slider-val">{{ sliderAlpha.toFixed(2) }}</span></div>
        <div class="control-row"><label>垂直角 β</label><input type="range" :min="0.1" :max="3.04" :step="0.01" v-model.number="sliderBeta" @input="updateCameraFromSlider" /><span class="slider-val">{{ sliderBeta.toFixed(2) }}</span></div>
        <div class="control-row"><label>距离</label><input type="range" :min="5" :max="600" :step="1" v-model.number="sliderRadius" @input="updateCameraFromSlider" /><span class="slider-val">{{ sliderRadius.toFixed(0) }}</span></div>
        <div class="control-actions">
          <button class="ctrl-btn save" @click="onSaveConfig">保存</button>
          <button class="ctrl-btn reset" @click="resetCamera">重置</button>
        </div>
      </div>
    </Transition>

    <div class="command-deck">
      <!-- 左侧面板：工艺数据（传感器） -->
      <aside class="side-panel left-panel">
        <section class="panel-block process-data-block">
          <div class="panel-title">工艺参数数据</div>
          <div class="monitor-list">
            <div v-for="item in monitorEntries" :key="item.key" class="monitor-item">
              <div class="monitor-head">
                <span class="monitor-name">{{ item.data.name }}</span>
                <span class="monitor-val">{{ formatMonitorValue(item.data.value) }} {{ item.data.unit }}</span>
              </div>
            </div>
          </div>
        </section>
      </aside>

      <!-- 中间区域：3D模型 -->
      <main class="stage-panel">
        <section class="hero-stage">
          <div class="stage-frame">
            <canvas ref="canvasRef" class="stage-canvas"></canvas>
            <ModelLoader :model-loaded="modelLoaded" :loading-progress="loadingProgress" />
            <div class="stage-canopy"></div>
            <div class="stage-orbit orbit-left"></div>
            <div class="stage-orbit orbit-right"></div>
            <div class="stage-hud-grid"></div>

            <div class="stage-badges">
              <div class="stage-badge"><span>工艺段</span><strong>高效沉淀池</strong></div>
            </div>

            <div class="frame-corner top-left"></div>
            <div class="frame-corner top-right"></div>
            <div class="frame-corner bottom-left"></div>
            <div class="frame-corner bottom-right"></div>


          </div>
        </section>
      </main>

      <!-- 右侧面板：设备控制 -->
      <aside class="side-panel right-panel">
        <section class="panel-block device-control-block">
          <div class="panel-title">设备控制</div>
          <div class="device-list">
            <div
              v-for="item in deviceEntries"
              :key="item.key"
              class="device-card"
              :class="{ fault: item.data.fault, running: isDeviceRunning(item.data) }"
            >
              <!-- 第一行：名称 + 就地/远程旋钮 -->
              <div class="card-row-top">
                <span class="card-device-name">{{ item.data.name }}</span>
                <span v-if="!deviceReadStatus[item.key]" class="card-unread" title="未读取到数据">✕</span>
                <div class="knob-switch">
                  <span class="knob-label-left" :class="{ active: !item.data.remote }">就地</span>
                  <div class="knob-dial" :class="{ remote: item.data.remote }">
                    <div class="knob-pointer"></div>
                  </div>
                  <span class="knob-label-right" :class="{ active: item.data.remote }">远程</span>
                </div>
              </div>
              <!-- 第二行：手自动 + 启动停止 + 状态 -->
              <div v-if="item.data.type === 'valve' && !item.data.fault" class="card-valve-status">
                <span class="valve-pos" :class="{ on: item.data.open }">开到位</span>
                <span class="valve-pos" :class="{ on: item.data.close }">关到位</span>
              </div>
              <div v-if="item.data.freqBack !== undefined" class="card-freq-block">
                <div class="card-freq-row">
                  <span class="freq-label">频率反馈</span>
                  <span class="freq-val">{{ formatMonitorValue(item.data.freqBack) }} Hz</span>
                </div>
                <div class="card-freq-row">
                  <span class="freq-label">频率给定</span>
                  <el-input-number v-model="item.data.setFreq" size="small" :min="0" :max="50" :step="0.5" :precision="1" controls-position="right" style="width:72px" />
                  <button class="card-freq-btn" @click="setFrequency(item.key)">设定</button>
                </div>
              </div>
              <div class="card-row-bottom">
                <button
                  class="card-btn mode-btn"
                  :class="{ manual: item.data.manualMode }"
                  @click="toggleManualMode(item.key, item.data.manualMode)"
                >
                  {{ item.data.manualMode ? '手动' : '自动' }}
                </button>
                <template v-if="item.data.type === 'valve'">
                  <button class="card-btn start-btn" :disabled="!canControl(item.data)" @click="openValve(item.key)">开阀</button>
                  <button class="card-btn stop-btn" :disabled="!canControl(item.data)" @click="closeValve(item.key)">关阀</button>
                </template>
                <template v-else>
                  <button class="card-btn start-btn" :disabled="!canControl(item.data)" @click="startMotor(item.key)">启动</button>
                  <button class="card-btn stop-btn" :disabled="!canControl(item.data)" @click="stopMotor(item.key)">停止</button>
                </template>
                <div class="card-status-group">
                  <span class="card-status-dot" :class="statusDotClass(item.data)"></span>
                  <span class="card-status-text" :class="statusDotClass(item.data)">{{ deviceStateText(item.data) }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as BABYLON from 'babylonjs'
import 'babylonjs-loaders'
import jwt_decode from 'jwt-decode'
import request from '@/api/request'
import ModelLoader from '@/components/ModelLoader.vue'
import { readPlcRealtimeValue } from '@/utils/plcInstruments'

// ====================== 基础状态 ======================
const isMobileDevice = ref(false)
const selectedDeviceKey = ref(null)
const pinLock = ref(true)
const canvasRef = ref(null)
const modelLoaded = ref(false)
const loadingProgress = ref(0)
const showControlPanel = ref(false)
const cameraLock = ref(true)
const sliderAlpha = ref(3.2)
const sliderBeta = ref(0.9)
const sliderRadius = ref(120)
const sliderTargetX = ref(0)
const sliderTargetY = ref(0)
const sliderTargetZ = ref(0)
const sliderCamX = ref(0)
const sliderCamY = ref(0)
const sliderCamZ = ref(0)
const sliderModelX = ref(0)
const sliderModelY = ref(0)
const sliderModelZ = ref(0)

const token = localStorage.getItem('token')
let role = 'user'
try { role = jwt_decode(token || '{}')?.role || 'user' } catch { role = 'user' }

const checkMobile = () => {
  const ua = navigator.userAgent
  isMobileDevice.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua) || window.innerWidth < 1024
}

// ====================== M3 点位表设备数据 ======================
// 依据 Excel M3 点位表：9个设备 + 9个仪表
const defaultDeviceData = {
  M3_QM1: { name: '高效沉淀池电动闸板', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  M3_GP1: { name: '1#回流污泥转子泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false, setFreq: 0, freqBack: 0 },
  M3_GP2: { name: '2#回流污泥转子泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false, setFreq: 0, freqBack: 0 },
  M3_GP3: { name: '剩余污泥转子泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false, setFreq: 0, freqBack: 0 },
  M3_HW1: { name: '高效沉淀池絮凝搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false, setFreq: 0, freqBack: 0 },
  M3_HW2: { name: '高效沉淀池混凝搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false, setFreq: 0, freqBack: 0 },
  M3_GP4: { name: '集水坑潜污泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  M3_HL1: { name: '高效沉淀池刮泥机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
}

const defaultMonitorData = {
  M3_RIT01: { name: '污泥界面仪', value: 0, unit: 'm' },
  M3_LIT01: { name: '絮凝池液位', value: 0, unit: 'm' },
  M3_LIT02: { name: '混凝池液位', value: 0, unit: 'm' },
  M3_AIT01: { name: '浊度计', value: 0, unit: 'NTU' },
  M3_FIT01: { name: '一期转盘进水流量计', value: 0, unit: 'm³/h' },
  M3_FIT02: { name: '高效沉淀池剩余污泥流量计', value: 0, unit: 'm³/h' },
  M3_GAS_H2S: { name: '硫化氢', value: 0, unit: 'ppm' },
  M3_GAS_CO: { name: '一氧化碳', value: 0, unit: 'ppm' },
  M3_GAS_CH4: { name: '甲烷', value: 0, unit: '%LEL' },
  M3_GAS_O2: { name: '氧气', value: 0, unit: '%' },
}

// ====================== localStorage ======================
const safeRead = (key, fallback) => {
  try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback } catch { return fallback }
}

const savedCameraConfig = safeRead('M3_CAMERA_V1', null)
sliderModelX.value = savedCameraConfig?.model?.x ?? 0
sliderModelY.value = savedCameraConfig?.model?.y ?? 0
sliderModelZ.value = savedCameraConfig?.model?.z ?? 0

// Pin 默认位置
const defaultPinPositions = {
  M3_QM1: { left: '20%', top: '30%' },
  M3_GP1: { left: '40%', top: '25%' },
  M3_GP2: { left: '60%', top: '25%' },
  M3_GP3: { left: '80%', top: '30%' },
  M3_HW1: { left: '20%', top: '50%' },
  M3_HW2: { left: '40%', top: '50%' },
  M3_GP4: { left: '60%', top: '50%' },
  M3_HL1: { left: '80%', top: '50%' },
}

const storageKeys = {
  device: 'M3_DEVICE_V5',
  monitor: 'M3_MONITOR_V5',
  pinLock: 'M3_PIN_LOCK_V2',
  pins: 'M3_PINS_V5'
}

const deviceData = ref({ ...defaultDeviceData, ...safeRead(storageKeys.device, {}) })
const cachedMonitorData = safeRead(storageKeys.monitor, {})
const monitorData = ref(Object.fromEntries(
  Object.entries(defaultMonitorData).map(([key, data]) => [
    key,
    { ...data, ...(cachedMonitorData[key] || {}), name: data.name },
  ])
))
const pinPositions = ref({ ...defaultPinPositions, ...safeRead(storageKeys.pins, {}) })
pinLock.value = localStorage.getItem(storageKeys.pinLock) !== 'false'

// ====================== 计算属性 ======================
const deviceEntries = computed(() => Object.entries(deviceData.value).map(([key, data]) => ({ key, data })))
const monitorEntries = computed(() => Object.entries(monitorData.value).map(([key, data]) => ({ key, data })))
const formatMonitorValue = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(3) : value
}
const faultCount = computed(() => deviceEntries.value.filter(i => i.data.fault).length)

const stagePins = computed(() => {
  return deviceEntries.value
    .filter(({ key }) => pinPositions.value[key])
    .map(({ key, data }) => ({
      key,
      label: data.name,
      left: pinPositions.value[key]?.left || '50%',
      top: pinPositions.value[key]?.top || '50%',
      data,
      value: deviceStateText(data),
      statusClass: statusDotClass(data),
    }))
})

const isDeviceRunning = (d) => {
  if (!d) return false
  if (d.type === 'valve') return !d.open && !d.close
  return d.run
}

const deviceStateText = (d) => {
  if (!d) return '离线'
  if (d.fault) return '故障'
  if (d.type === 'valve') return d.open ? '已开启' : d.close ? '已关闭' : '运行中'
  return d.run ? '运行' : '待机'
}

const statusDotClass = (d) => {
  if (!d) return 'offline'
  if (d.fault) return 'fault'
  if (d.type === 'valve') return d.open ? 'standby' : d.close ? 'standby' : 'running'
  return d.run ? 'running' : 'standby'
}

const canControl = (d) => role === 'admin' && d?.remote && d?.manualMode

// ====================== 后端API对接 ======================
const plcConnected = ref(false)
const plcIp = ref('192.168.0.103')
const deviceReadStatus = ref(Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])))
let pollTimer = null
let consecutiveFailures = 0

const m3GasFields = {
  M3_GAS_H2S: 'h2s',
  M3_GAS_CO: 'co',
  M3_GAS_CH4: 'ch4',
  M3_GAS_O2: 'o2',
}

const applyM3MonitorValues = (overview) => {
  Object.entries(monitorData.value).forEach(([code, local]) => {
    const readCode = code.startsWith('M3_GAS_') ? code : `${code}_OUTPUT`
    const output = readPlcRealtimeValue(overview, readCode)
    if (output !== undefined && output !== null) local.value = output
  })

  Object.entries(m3GasFields).forEach(([code, field]) => {
    const output = overview?.gas?.[field]
    if (output !== undefined && output !== null && monitorData.value[code]) {
      monitorData.value[code].value = output
    }
  })
}

const fetchOverview = async () => {
  try {
    const res = await request.get('/plc/m3/overview')
    if (!res.data?.success) return
    const overview = res.data.data
    const { devices, plc } = overview
    if (plc?.ip) plcIp.value = plc.ip

    const readCodes = new Set()
    if (devices && devices.length > 0) {
      consecutiveFailures = 0
      plcConnected.value = true
      devices.forEach(d => {
        const local = deviceData.value[d.code]
        if (!local) return
        readCodes.add(d.code)
        local.remote = !!d.remote
        local.fault = !!d.fault
        if (local.type === 'valve') {
          local.open = !!d.openPosition
          local.close = !!d.closePosition
        } else {
          local.run = !!d.running
        }
        local.manualMode = !d.manualModeBit
        if (d.frequencyFeedback !== undefined) local.freqBack = d.frequencyFeedback
        if (d.frequencySetpoint !== undefined) local.setFreq = d.frequencySetpoint
      })
    }
    const statusMap = {}
    readCodes.forEach(c => { statusMap[c] = true })
    deviceReadStatus.value = statusMap

    applyM3MonitorValues(overview)
  } catch (e) {
    consecutiveFailures++
    if (consecutiveFailures >= 3) { plcConnected.value = false; deviceReadStatus.value = Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])) }
    console.error(`M3 overview 获取失败 (连续${consecutiveFailures}次):`, e.message)
  }
}

const startPolling = () => {
  fetchOverview()
  pollTimer = setInterval(fetchOverview, 3000)
}

// ====================== 设备选择 ======================
const selectDevice = (key) => { if (deviceData.value[key]) selectedDeviceKey.value = key }

const togglePinLock = () => {
  pinLock.value = !pinLock.value
  savePinLock()
  ElMessage.success(pinLock.value ? '标签位置已锁定' : '标签位置已解锁')
}

// ====================== 拖拽 ======================
const dragTarget = ref(null)
let dragOffset = { x: 0, y: 0 }

const startDrag = (e, key) => {
  if (pinLock.value) return
  dragTarget.value = key
  const frame = e.currentTarget.closest('.stage-frame')
  if (!frame) return
  const rect = frame.getBoundingClientRect()
  const pinLeft = parseFloat(pinPositions.value[key]?.left || '50') / 100 * rect.width
  const pinTop = parseFloat(pinPositions.value[key]?.top || '50') / 100 * rect.height
  dragOffset = { x: e.clientX - rect.left - pinLeft, y: e.clientY - rect.top - pinTop }
}

const onDrag = (e) => {
  if (!dragTarget.value) return
  const frame = document.querySelector('.stage-frame')
  if (!frame) return
  const rect = frame.getBoundingClientRect()
  const x = e.clientX - rect.left - dragOffset.x
  const y = e.clientY - rect.top - dragOffset.y
  pinPositions.value[dragTarget.value] = {
    left: `${Math.min(95, Math.max(5, (x / rect.width) * 100)).toFixed(2)}%`,
    top: `${Math.min(88, Math.max(10, (y / rect.height) * 100)).toFixed(2)}%`
  }
}

const stopDrag = () => { dragTarget.value = null }

// ====================== 设备控制 ======================
const toggleRemote = async (key) => {
  // 就地/远程为只读状态，不发送指令
  ElMessage.info('就地/远程状态由现场旋钮控制，只读显示')
}

// M3 PLC: 手动=0, 自动=1, enabled 直接写入 PLC 位值
const toggleManualMode = async (key, currentMode) => {
  const it = deviceData.value[key]
  if (!it) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (!it.remote) return ElMessage.warning('该设备未授权远程控制')
  const enabled = currentMode
  try {
    await request.post(`/plc/m3/devices/${key}/manual-mode`, null, { params: { enabled } })
    ElMessage.success('手自动已切换')
    setTimeout(fetchOverview, 500)
    setTimeout(fetchOverview, 1500)
  } catch (e) {
    ElMessage.error('手自动切换失败')
  }
}

const startMotor = async (key) => {
  const it = deviceData.value[key]
  if (!it) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (!it.remote) return ElMessage.warning('该设备未授权远程控制')
  if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认启动「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/m3/devices/${key}/command`, null, { params: { action: 'START', pulseMs: 300 } })
    ElMessage.success('启动指令已发送')
    fetchOverview()
  } catch (e) {
    ElMessage.error('启动失败')
  }
}

const stopMotor = async (key) => {
  const it = deviceData.value[key]
  if (!it) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (!it.remote) return ElMessage.warning('该设备未授权远程控制')
  if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认停止「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/m3/devices/${key}/command`, null, { params: { action: 'STOP', pulseMs: 300 } })
    ElMessage.success('停止指令已发送')
    fetchOverview()
  } catch (e) {
    ElMessage.error('停止失败')
  }
}

const setFrequency = async (key) => {
  const it = deviceData.value[key]; if (!it) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (!it.remote) return ElMessage.warning('该设备未授权远程控制')
  try {
    await request.post(`/plc/m3/devices/${key}/frequency`, null, { params: { value: it.setFreq } })
    ElMessage.success(`${it.name} 频率 ${it.setFreq}Hz 已设定`)
    fetchOverview()
  } catch (e) { ElMessage.error('频率设定失败') }
}

const openValve = async (key) => {
  const it = deviceData.value[key]
  if (!it) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (!it.remote) return ElMessage.warning('该设备未授权远程控制')
  if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认开启「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/m3/devices/${key}/command`, null, { params: { action: 'OPEN', pulseMs: 300 } })
    ElMessage.success('开阀指令已发送')
    fetchOverview()
  } catch (e) {
    ElMessage.error('开阀失败')
  }
}

const closeValve = async (key) => {
  const it = deviceData.value[key]
  if (!it) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (!it.remote) return ElMessage.warning('该设备未授权远程控制')
  if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认关闭「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/m3/devices/${key}/command`, null, { params: { action: 'CLOSE', pulseMs: 300 } })
    ElMessage.success('关阀指令已发送')
    fetchOverview()
  } catch (e) {
    ElMessage.error('关阀失败')
  }
}

const savePinPositions = () => localStorage.setItem(storageKeys.pins, JSON.stringify(pinPositions.value))
const savePinLock = () => localStorage.setItem(storageKeys.pinLock, String(pinLock.value))

// ====================== 监听设备变化自动保存 ======================
watch(deviceData, () => {
  localStorage.setItem(storageKeys.device, JSON.stringify(deviceData.value))
}, { deep: true })

watch(monitorData, () => {
  localStorage.setItem(storageKeys.monitor, JSON.stringify(monitorData.value))
}, { deep: true })

watch(pinPositions, () => {
  localStorage.setItem(storageKeys.pins, JSON.stringify(pinPositions.value))
}, { deep: true })

// ====================== 生命周期 ======================

// ── Babylon.js 3D 场景 ──
// ── 设备颜色映射 + 搅拌机动画 ──
const DEVICE_COLOR_MAP = [
  { key: 'M3_HW1', keyword: '絮凝搅拌机', isValve: false },
  { key: 'M3_HW2', keyword: '混合搅拌机', isValve: false },
  { key: 'M3_HL1', keyword: '刮泥机', isValve: false },
  { key: 'M3_HL1', keyword: 'Cylinder334', isValve: false },
  { key: 'M3_GP4', keyword: '潜污泵', isValve: false },
  { key: 'M3_GP1', keyword: '1号污泥转子泵', isValve: false },
  { key: 'M3_GP2', keyword: '2号污泥转子泵', isValve: false },
  { key: 'M3_GP3', keyword: '3号污泥转子泵', isValve: false },
  { key: 'M3_QM1', keyword: '1号四杆阀门', isValve: true },
]
const STIRRER_MAP = { M3_HW1: '絮凝搅拌机', M3_HW2: '混合搅拌机', M3_HL1: '刮泥机' }

let engine = null, scene = null, camera = null, importedMeshes = [], rootMesh = null
const originalMaterials = new Map()
let stirrerMeshes = {}

const findMeshesByName = (meshes, keyword) => meshes.filter(m => m.name.includes(keyword))
const ensureUniqueMaterial = (mesh) => {
  if (originalMaterials.has(mesh.name)) return
  if (!mesh.material) {
    const generated = new BABYLON.PBRMaterial(mesh.name + '_mat', scene)
    generated.albedoColor = BABYLON.Color3.White()
    generated.emissiveColor = BABYLON.Color3.Black()
    generated.metallic = 0
    generated.roughness = 0.8
    originalMaterials.set(mesh.name, { material: generated, origColor: BABYLON.Color3.White(), generated: true })
    mesh.material = generated
    return
  }
  if (!originalMaterials.has(mesh.name)) {
    const cloned = mesh.material.clone(mesh.name + '_mat')
    originalMaterials.set(mesh.name, { material: cloned, origColor: mesh.material.albedoColor.clone(), generated: false })
    mesh.material = cloned
  }
}
const syncDeviceColors = () => {
  for (const { key, keyword, isValve } of DEVICE_COLOR_MAP) {
    const meshes = findMeshesByName(importedMeshes, keyword)
    if (!meshes.length) continue
    const d = deviceData.value[key]
    if (!d) continue
    const running = isValve ? d.open : d.run
    meshes.forEach(mesh => {
      if (!mesh.material && !d.fault && !running) return
      ensureUniqueMaterial(mesh)
      if (!mesh.material) return
      if (d.fault) { mesh.material.albedoColor = new BABYLON.Color3(1, 0.15, 0.15); mesh.material.emissiveColor = new BABYLON.Color3(0.8, 0, 0) }
      else if (running) { mesh.material.albedoColor = new BABYLON.Color3(0, 1, 0); mesh.material.emissiveColor = new BABYLON.Color3(0, 0, 0) }
      else {
        const saved = originalMaterials.get(mesh.name)
        if (saved?.generated) {
          mesh.material.dispose()
          mesh.material = null
          originalMaterials.delete(mesh.name)
          return
        }
        if (saved) mesh.material.albedoColor = saved.origColor.clone()
        mesh.material.emissiveColor = new BABYLON.Color3(0, 0, 0)
      }
    })
  }
}
watch(deviceData, syncDeviceColors, { deep: true })

const syncSliders = () => {
  if (!camera) return
  sliderAlpha.value = Number(camera.alpha.toFixed(2)); sliderBeta.value = Number(camera.beta.toFixed(2)); sliderRadius.value = Number(camera.radius.toFixed(0))
  sliderTargetX.value = Number(camera.target.x.toFixed(0)); sliderTargetY.value = Number(camera.target.y.toFixed(0)); sliderTargetZ.value = Number(camera.target.z.toFixed(0))
  sliderCamX.value = Number(camera.position.x.toFixed(0)); sliderCamY.value = Number(camera.position.y.toFixed(0)); sliderCamZ.value = Number(camera.position.z.toFixed(0))
  if (rootMesh) { sliderModelX.value = Number(rootMesh.position.x.toFixed(0)); sliderModelY.value = Number(rootMesh.position.y.toFixed(0)); sliderModelZ.value = Number(rootMesh.position.z.toFixed(0)) }
}
const updateCameraFromSlider = () => { if (!camera) return; camera.alpha = sliderAlpha.value; camera.beta = sliderBeta.value; camera.radius = sliderRadius.value; syncSliders() }
const updateTargetFromSlider = () => { if (!camera) return; camera.setTarget(new BABYLON.Vector3(sliderTargetX.value, sliderTargetY.value, sliderTargetZ.value)); syncSliders() }
const updateCameraPosFromSlider = () => { if (!camera) return; camera.setPosition(new BABYLON.Vector3(sliderCamX.value, sliderCamY.value, sliderCamZ.value)); syncSliders() }
const updateModelPosFromSlider = () => { if (!rootMesh) return; rootMesh.position.x = sliderModelX.value; rootMesh.position.y = sliderModelY.value; rootMesh.position.z = sliderModelZ.value }
const applyCameraLock = () => {
  const canvas = canvasRef.value
  if (!camera || !canvas) return
  camera.detachControl()
  canvas.style.pointerEvents = cameraLock.value ? 'none' : 'auto'
  if (!cameraLock.value) camera.attachControl(canvas, true)
}
const toggleCameraLock = () => {
  cameraLock.value = !cameraLock.value
  applyCameraLock()
  ElMessage.success(cameraLock.value ? '视角已锁定' : '视角已解锁')
}
const resetCamera = () => { if (!camera) return; camera.alpha = 3.2; camera.beta = 0.9; camera.radius = 120; camera.setTarget(BABYLON.Vector3.Zero()); if (rootMesh) rootMesh.position = new BABYLON.Vector3(0, 0, 0); syncSliders() }
const onSaveConfig = () => {
  if (!camera) return
  const config = { alpha: camera.alpha, beta: camera.beta, radius: camera.radius, target: { x: camera.target.x, y: camera.target.y, z: camera.target.z }, position: { x: camera.position.x, y: camera.position.y, z: camera.position.z }, model: rootMesh ? { x: rootMesh.position.x, y: rootMesh.position.y, z: rootMesh.position.z } : { x: 0, y: 0, z: 0 } }
  localStorage.setItem('M3_CAMERA_V1', JSON.stringify(config)); ElMessage.success('配置已保存')
}
const handleResize = () => { if (engine) engine.resize() }

const initScene = async () => {
  await nextTick()
  const canvas = canvasRef.value; if (!canvas) return
  engine = new BABYLON.Engine(canvas, true, { preserveDrawingBuffer: true, stencil: true, antialias: true })
  scene = new BABYLON.Scene(engine); scene.clearColor = new BABYLON.Color4(0, 0, 0, 0)
  const savedConfig = savedCameraConfig
  const initAlpha = savedConfig?.alpha ?? 3.2, initBeta = savedConfig?.beta ?? 0.9, initRadius = savedConfig?.radius ?? 120
  const initTarget = savedConfig?.target ? new BABYLON.Vector3(savedConfig.target.x, savedConfig.target.y, savedConfig.target.z) : BABYLON.Vector3.Zero()
  camera = new BABYLON.ArcRotateCamera('m3-camera', initAlpha, initBeta, initRadius, initTarget, scene)
  camera.lowerRadiusLimit = 5; camera.upperRadiusLimit = 600; camera.wheelDeltaPercentage = 0.008
  if (savedConfig?.position) camera.setPosition(new BABYLON.Vector3(savedConfig.position.x, savedConfig.position.y, savedConfig.position.z))
  applyCameraLock()
  camera.onViewMatrixChangedObservable.add(() => syncSliders())
  // 灯光
  const ambientLight = new BABYLON.HemisphericLight('ambient', BABYLON.Vector3.Up(), scene); ambientLight.intensity = 0.3
  const mainLight = new BABYLON.DirectionalLight('main', new BABYLON.Vector3(-1, -1.5, 0.5), scene); mainLight.position = new BABYLON.Vector3(8, 15, 10); mainLight.intensity = 0.8
  const fillLight = new BABYLON.DirectionalLight('fill', new BABYLON.Vector3(0.5, -0.2, -1), scene); fillLight.position = new BABYLON.Vector3(-5, 8, -12); fillLight.intensity = 0.2
  const bottomLight = new BABYLON.DirectionalLight('bottom', new BABYLON.Vector3(0, 1, 0), scene); bottomLight.position = new BABYLON.Vector3(0, -5, 0); bottomLight.intensity = 0.15
  const hdr = BABYLON.CubeTexture.CreateFromPrefilteredData('/environmentSpecular.env', scene); scene.environmentTexture = hdr
  BABYLON.SceneLoader.ImportMesh('', '/', '高效沉淀池.max1024.glb', scene, (meshes) => {
    importedMeshes = meshes; meshes.forEach(m => { m.receiveShadows = true; if (m.material) m.material.backFaceCulling = false })
    rootMesh = meshes.find(m => m.name === '__root__')
    if (rootMesh) { rootMesh.scaling = new BABYLON.Vector3(-1, 1, 1); rootMesh.position.x = sliderModelX.value; rootMesh.position.y = sliderModelY.value; rootMesh.position.z = sliderModelZ.value }
    if (camera && meshes.length) { let minV = null, maxV = null; meshes.forEach(m => { if (!m.getBoundingInfo) return; const b = m.getBoundingInfo().boundingBox; minV = minV ? BABYLON.Vector3.Minimize(minV, b.minimumWorld) : b.minimumWorld.clone(); maxV = maxV ? BABYLON.Vector3.Maximize(maxV, b.maximumWorld) : b.maximumWorld.clone() }); if (minV && maxV) { const size = Math.max(maxV.x - minV.x, maxV.y - minV.y, maxV.z - minV.z, 1); camera.lowerRadiusLimit = Math.max(size * 0.2, 5); camera.upperRadiusLimit = Math.max(size * 5, 600) } }
    for (const [key, keyword] of Object.entries(STIRRER_MAP)) {
      const mesh = meshes.find(m => m.name.includes(keyword))
      if (mesh) stirrerMeshes[key] = mesh
    }
    modelLoaded.value = true; syncDeviceColors(); syncSliders(); console.log('[M3] 模型已加载，网格:', meshes.map(m => m.name).join(', '))
  }, (evt) => { if (evt.lengthComputable) loadingProgress.value = Math.min(95, Math.round((evt.loaded / evt.total) * 100)) }, () => { loadingProgress.value = 100; setTimeout(() => { modelLoaded.value = true }, 100) })
  engine.runRenderLoop(() => {
    for (const [key, mesh] of Object.entries(stirrerMeshes)) {
      const d = deviceData.value[key]
      if (d && d.run && !d.fault) mesh.rotation.y += 0.05
    }
    if (scene) scene.render()
  })
}

onMounted(() => {
  checkMobile()
  if (!isMobileDevice.value) initScene()
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
  window.addEventListener('resize', handleResize)
  startPolling()
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
  window.removeEventListener('resize', handleResize)
  if (pollTimer) clearInterval(pollTimer)
  if (camera) camera.detachControl()
  if (scene) { scene.dispose(); scene = null }
  if (engine) { engine.dispose(); engine = null }
  camera = null
})
</script>

<style scoped>
.m3-page {
  position: relative;
  height: calc(100vh - var(--nav-height, 113px));
  display: flex;
  flex-direction: column;
  padding: 12px 14px 14px;
  background: linear-gradient(180deg, #0F172A 0%, #0F172A 50%, #0F172A 100%);
  color: #F2F3F5;
  overflow: hidden;
}

/* 3D 模型全页背景 */
.scene-background {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

.stage-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  touch-action: none;
  outline: none;
  pointer-events: none;
}

.scene-bg-shade {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 50%, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.08)),
    linear-gradient(180deg, rgba(15, 23, 42, 0.03), rgba(15, 23, 42, 0.06));
  pointer-events: none;
  z-index: 1;
}

/* ==================== 页面装饰 ==================== */
.page-aurora,
.page-noise,
.page-vignette,
.page-gridlines,
.page-flare,
.hud-node {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.page-aurora { opacity: 0.52; }
.aurora-left { background: radial-gradient(circle at 14% 26%, rgba(43, 229, 255, 0.12), transparent 26%); }
.aurora-right { background: radial-gradient(circle at 88% 18%, rgba(104, 112, 255, 0.12), transparent 24%); }

.page-noise {
  opacity: 0.08;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, rgba(255, 255, 255, 0.8), transparent 90%);
}

.page-vignette {
  background:
    radial-gradient(circle at 50% 48%, transparent 0%, rgba(16, 56, 88, 0.04) 54%, rgba(16, 56, 88, 0.10) 100%),
    linear-gradient(90deg, rgba(11, 42, 68, 0.14), transparent 16%, transparent 84%, rgba(11, 42, 68, 0.14));
  z-index: 0;
}

.page-gridlines {
  opacity: 0.16;
  background-image:
    linear-gradient(rgba(111, 221, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(111, 221, 255, 0.045) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(circle at 50% 52%, rgba(255, 255, 255, 0.36), transparent 92%);
  animation: gridDrift 24s linear infinite;
  z-index: 0;
}

.page-flare {
  inset: auto 10% auto;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(103, 230, 255, 0.7), transparent);
  box-shadow: 0 0 18px rgba(103, 230, 255, 0.2), 0 0 40px rgba(53, 148, 255, 0.12);
  opacity: 0.7;
  z-index: 0;
}

.flare-top { top: 14px; animation: horizonPulse 7s ease-in-out infinite; }
.flare-bottom { bottom: 12px; opacity: 0.45; animation: horizonPulse 9s ease-in-out infinite reverse; }

.hud-node {
  inset: auto;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px solid rgba(88, 219, 255, 0.1);
  background:
    radial-gradient(circle at center, rgba(116, 236, 255, 0.14), transparent 42%),
    conic-gradient(from 0deg, rgba(116, 236, 255, 0), rgba(116, 236, 255, 0.48), rgba(116, 236, 255, 0));
  box-shadow: 0 0 18px rgba(71, 200, 255, 0.14), inset 0 0 18px rgba(71, 200, 255, 0.08);
  opacity: 0.52;
  animation: hudNodePulse 5.8s ease-in-out infinite;
  z-index: 0;
}

.hud-node::before,
.hud-node::after {
  content: '';
  position: absolute;
  inset: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
}

.hud-node::before {
  width: 8px;
  height: 8px;
  background: #7ae8ff;
  box-shadow: 0 0 16px rgba(122, 232, 255, 0.7);
}

.hud-node::after {
  width: 100%;
  height: 100%;
  border: 1px solid rgba(122, 232, 255, 0.18);
  animation: radarPing 4.2s ease-out infinite;
}

.node-top-left { left: 18px; top: 18px; }
.node-top-right { right: 18px; top: 18px; animation-delay: 1.2s; }
.node-bottom-left { left: 18px; bottom: 18px; animation-delay: 2.2s; }
.node-bottom-right { right: 18px; bottom: 18px; animation-delay: 3.1s; }

/* ==================== 主布局 ==================== */
.command-deck {
  position: relative;
  flex: 1;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) 300px;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
  z-index: 2;
}

.side-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  z-index: 2;
  isolation: isolate;
}

.left-panel::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 14px;
  right: 2px;
  width: 1px;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(85, 220, 255, 0.44), transparent);
  box-shadow: 0 0 16px rgba(85, 220, 255, 0.18);
}

.right-panel::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 2px;
  width: 1px;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(85, 220, 255, 0.44), transparent);
  box-shadow: 0 0 16px rgba(85, 220, 255, 0.18);
}

.right-panel {
  overflow: hidden;
}

.stage-panel {
  position: relative;
  min-width: 0;
  z-index: 2;
}

.panel-block,
.stage-frame {
  position: relative;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background: rgba(15, 23, 42, 0.88);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    inset 0 8px 18px rgba(120, 214, 255, 0.02),
    0 8px 20px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(147, 228, 255, 0.03),
    0 0 22px rgba(91, 182, 255, 0.03);
  backdrop-filter: saturate(118%);
  overflow: hidden;
  clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px));
}

.panel-block::before,
.stage-frame::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(170, 240, 255, 0.10), transparent);
  transform: translateX(-100%);
  animation: sweep 14s linear infinite;
}

.panel-block::after,
.stage-frame::after {
  content: '';
  position: absolute;
  inset: 1px;
  pointer-events: none;
  border: 1px solid rgba(168, 240, 255, 0.14);
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
}

.panel-block::before { animation-duration: 10s; }

.panel-block:hover,
.stage-frame:hover {
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.06),
    inset 0 12px 24px rgba(33, 127, 214, 0.04),
    0 12px 24px rgba(0, 0, 0, 0.10),
    0 0 0 1px rgba(74, 219, 255, 0.08),
    0 0 26px rgba(44, 126, 255, 0.06);
}

.panel-block::after {
  box-shadow: inset 0 0 22px rgba(87, 194, 255, 0.04), inset 0 1px 0 rgba(143, 228, 255, 0.08);
}

.panel-title {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 14px 16px 0;
  padding-right: 8px;
  font-size: 1.125rem;
  font-weight: 700;
  color: #d8f7ff;
  letter-spacing: 0.04em;
  text-shadow: 0 0 14px rgba(74, 210, 255, 0.18), 0 0 22px rgba(74, 210, 255, 0.05);
}

.panel-title::before {
  content: '';
  width: 26px;
  height: 3px;
  background: linear-gradient(90deg, #2ce1ff, #86f8c8 55%, rgba(134, 248, 200, 0.2));
  box-shadow: 0 0 14px rgba(44, 225, 255, 0.46);
}

/* ==================== 工艺数据面板 ==================== */
.process-data-block {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.monitor-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 12px 12px;
  overflow-y: auto;
}

.monitor-item {
  padding: 10px 12px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background: rgba(15, 23, 42, 0.6);
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.monitor-head {
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: flex-start;
}

.monitor-name {
  min-width: 0;
  flex: 1;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.35;
  color: #e8fbff;
}

.monitor-val {
  flex-shrink: 0;
  font-size: 1rem;
  font-weight: 700;
  font-family: var(--font-display);
  color: #8ceeff;
  white-space: nowrap;
}

/* ==================== Hero Stage ==================== */
.hero-stage {
  height: 100%;
}

.stage-frame {
  height: 100%;
  background: transparent;
  backdrop-filter: none;
  border-color: rgba(0, 128, 255, 0.10);
  box-shadow: none;
  display: flex;
  flex-direction: column;
}

.stage-frame::before,
.stage-frame::after { display: none; }
.stage-frame:hover { box-shadow: none; background: transparent; }

.stage-canopy { display: none; }

.stage-orbit {
  position: absolute;
  pointer-events: none;
  z-index: 2;
  border-radius: 50%;
  border: 1px dashed rgba(85, 220, 255, 0.12);
  box-shadow: inset 0 0 30px rgba(85, 220, 255, 0.03);
}

.orbit-left { left: -72px; bottom: 48px; width: 220px; height: 220px; }
.orbit-right { right: -84px; top: 92px; width: 300px; height: 300px; }

.stage-hud-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(92, 229, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(92, 229, 255, 0.04) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: radial-gradient(circle at 50% 52%, rgba(255, 255, 255, 0.5), transparent 88%);
  animation: gridDrift 28s linear infinite;
  z-index: 1;
}

.stage-badges {
  position: absolute;
  left: 18px;
  top: 18px;
  z-index: 4;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stage-badge {
  min-width: 156px;
  padding: 10px 12px;
  border: 1px solid rgba(0, 128, 255, 0.28);
  background:
    linear-gradient(90deg, rgba(0, 128, 255, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.12), rgba(30, 41, 59, 0.05));
  backdrop-filter: saturate(135%);
  box-shadow:
    0 6px 14px rgba(0, 0, 0, 0.06),
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    inset 0 0 18px rgba(74, 198, 255, 0.04);
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
}

.stage-badge span { display: block; font-size: 0.875rem; color: #84b6d1; }
.stage-badge strong { display: block; margin-top: 6px; font-size: 1rem; color: #F2F3F5; }

.frame-corner {
  position: absolute;
  width: 34px;
  height: 34px;
  border: 2px solid rgba(79, 218, 255, 0.68);
  z-index: 5;
  opacity: 0.58;
  box-shadow: 0 0 10px rgba(79, 218, 255, 0.12);
}

.frame-corner.top-left { top: 12px; left: 12px; border-right: none; border-bottom: none; }
.frame-corner.top-right { top: 12px; right: 12px; border-left: none; border-bottom: none; }
.frame-corner.bottom-left { bottom: 12px; left: 12px; border-right: none; border-top: none; }
.frame-corner.bottom-right { right: 12px; bottom: 12px; border-left: none; border-top: none; }

/* ==================== Pin 标签 ==================== */
.stage-pin-layer {
  position: absolute;
  inset: 0;
  z-index: 6;
  pointer-events: none;
}

.stage-pin {
  position: absolute;
  z-index: 6;
  min-width: 0;
  padding: 4px 8px 5px 7px;
  border: 1px solid rgba(92, 211, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(10, 36, 56, 0.14), transparent 40%),
    linear-gradient(180deg, rgba(5, 17, 29, 0.88), rgba(3, 12, 23, 0.8));
  color: #effcff;
  transform: translate(-50%, -50%);
  cursor: pointer;
  transition: 0.2s ease;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: none;
  pointer-events: auto;
}

.stage-pin:hover,
.stage-pin.active {
  border-color: rgba(117, 236, 255, 0.42);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.18), 0 0 18px rgba(56, 200, 255, 0.12);
}

.stage-pin.fault {
  border-color: rgba(255, 126, 126, 0.65);
}

.stage-pin::after {
  content: '';
  position: absolute;
  left: 14px;
  bottom: -14px;
  width: 1px;
  height: 14px;
  background: linear-gradient(180deg, rgba(117, 236, 255, 0.75), transparent);
}

.pin-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  flex: none;
  background: #4bb9ff;
  box-shadow: 0 0 10px rgba(75, 185, 255, 0.55);
}

.pin-status-dot.running {
  background: #30e6ad;
  box-shadow: 0 0 10px rgba(48, 230, 173, 0.6);
}

.pin-status-dot.standby {
  background: #4bb9ff;
  box-shadow: 0 0 10px rgba(75, 185, 255, 0.5);
}

.pin-status-dot.fault {
  background: #ff7a7a;
  box-shadow: 0 0 10px rgba(255, 122, 122, 0.62);
}

.pin-status-dot.offline {
  background: #4a5a6a;
}

.pin-name {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.04em;
  color: #F2F3F5;
  white-space: nowrap;
}

.pin-divider {
  width: 1px;
  height: 12px;
  background: linear-gradient(180deg, rgba(102, 223, 255, 0.15), rgba(102, 223, 255, 0.75), rgba(102, 223, 255, 0.15));
  flex: none;
}

.pin-value {
  display: block;
  font-size: 0.75rem;
  line-height: 1;
  color: rgba(127, 205, 234, 0.88);
  white-space: nowrap;
}

/* ==================== 设备控制面板 ==================== */
.device-control-block {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.device-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 12px 12px;
  overflow-y: auto;
}

.device-card {
  position: relative;
  border: 1px solid rgba(0, 128, 255, 0.22);
  background: rgba(15, 23, 42, 0.6);
  padding: 0;
  display: flex;
  flex-direction: column;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  transition: all 0.25s ease;
}

.device-card:hover {
  border-color: rgba(0, 200, 255, 0.38);
  background: rgba(15, 23, 42, 0.8);
}

.device-card.fault {
  border-color: rgba(255, 80, 80, 0.45);
}

.device-card.running {
  border-color: rgba(48, 230, 173, 0.68);
  background: rgba(48, 230, 173, 0.06);
  box-shadow: inset 0 0 0 1px rgba(48, 230, 173, 0.32), 0 0 14px rgba(48, 230, 173, 0.26), 0 0 26px rgba(48, 230, 173, 0.12);
}

/* 第一行：名称 + 旋钮 */
.card-row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px 8px;
  border-bottom: 1px solid rgba(0, 128, 255, 0.1);
}

.card-device-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: #e8fbff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 0 10px rgba(61, 184, 255, 0.12);
}

.card-unread {
  font-size: 0.875rem;
  color: #ff5050;
  font-weight: 700;
  flex-shrink: 0;
  opacity: 0.8;
  line-height: 1;
}

/* 就地/远程旋钮开关 */
.knob-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  user-select: none;
}

.knob-label-left,
.knob-label-right {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #4a5a6a;
  letter-spacing: 0.04em;
  transition: all 0.3s ease;
}

.knob-label-left.active {
  color: #ffd68f;
  text-shadow: 0 0 8px rgba(255, 214, 143, 0.6), 0 0 16px rgba(255, 190, 90, 0.3);
}

.knob-label-right.active {
  color: #7dffd1;
  text-shadow: 0 0 8px rgba(125, 255, 209, 0.6), 0 0 16px rgba(48, 230, 173, 0.3);
}

.knob-dial {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(102, 153, 180, 0.3);
  background: radial-gradient(circle at 40% 35%, #3a4a5a, #1a2a3a);
  position: relative;
  transition: all 0.4s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3), inset 0 1px 2px rgba(255, 255, 255, 0.05);
}

.knob-dial.remote {
  border-color: rgba(48, 230, 173, 0.4);
  box-shadow: 0 0 10px rgba(48, 230, 173, 0.15), 0 2px 6px rgba(0, 0, 0, 0.3);
}

.knob-pointer {
  position: absolute;
  width: 3px;
  height: 10px;
  background: #c0d0e0;
  border-radius: 1px;
  top: 3px;
  left: 50%;
  transform: translateX(-50%) rotate(-45deg);
  transform-origin: center 11px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 0 4px rgba(192, 208, 224, 0.5);
}

.knob-dial.remote .knob-pointer {
  transform: translateX(-50%) rotate(45deg);
  background: #7dffd1;
  box-shadow: 0 0 6px rgba(125, 255, 209, 0.6);
}

/* 频率显示 */
.card-freq-block { padding: 6px 12px 0; display: flex; flex-direction: column; gap: 4px; }
.card-freq-row { display: flex; align-items: center; gap: 6px; }
.freq-label { font-size: 0.6875rem; color: #8a9ab0; flex-shrink: 0; }
.freq-val { font-size: 0.75rem; font-family: var(--font-display); color: #8ceeff; flex-shrink: 0; min-width: 50px; }
.card-freq-btn { padding: 2px 8px; font-size: 0.6875rem; font-weight: 600; border: 1px solid rgba(0,128,255,0.3); background: rgba(20,60,90,0.5); color: #c0eaff; cursor: pointer; border-radius: 3px; transition: all 0.2s ease; }
.card-freq-btn:hover { border-color: rgba(0,200,255,0.6); color: #fff; }

/* 阀门到位状态 */
.card-valve-status {
  display: flex;
  gap: 12px;
  padding: 6px 12px 0;
}

.valve-pos {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #4a5a6a;
  padding: 2px 8px;
  border: 1px solid rgba(74,90,106,0.3);
  border-radius: 3px;
  transition: all 0.3s ease;
}

.valve-pos.on {
  color: #7dffd1;
  border-color: rgba(48,230,173,0.4);
  background: rgba(48,230,173,0.08);
  text-shadow: 0 0 6px rgba(48,230,173,0.4);
}

/* 第二行：按钮 + 状态 */
.card-row-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px 10px;
}

.card-btn {
  padding: 4px 10px;
  font-size: 0.75rem;
  font-weight: 600;
  border: 1px solid rgba(0, 128, 255, 0.25);
  background: rgba(20, 50, 70, 0.5);
  color: #c0eaff;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.card-btn:hover:not(:disabled) {
  border-color: rgba(0, 200, 255, 0.5);
  background: rgba(30, 70, 100, 0.6);
  color: #fff;
}

.card-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.card-btn.mode-btn {
  min-width: 40px;
}

.card-btn.mode-btn.manual {
  border-color: rgba(255, 190, 90, 0.4);
  color: #ffd68f;
  background: rgba(60, 45, 15, 0.5);
}

.card-btn.start-btn {
  border-color: rgba(48, 230, 173, 0.3);
  color: #7dffd1;
}

.card-btn.start-btn:hover:not(:disabled) {
  border-color: rgba(48, 230, 173, 0.6);
  background: rgba(20, 60, 40, 0.5);
}

.card-btn.stop-btn {
  border-color: rgba(255, 100, 100, 0.3);
  color: #ffaeae;
}

.card-btn.stop-btn:hover:not(:disabled) {
  border-color: rgba(255, 100, 100, 0.6);
  background: rgba(60, 20, 20, 0.5);
}

/* 状态指示 */
.card-status-group {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-left: auto;
}

.card-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.card-status-dot.running {
  background: #30e6ad;
  box-shadow: 0 0 8px rgba(48, 230, 173, 0.6);
}

.card-status-dot.standby {
  background: #4bb9ff;
  box-shadow: 0 0 8px rgba(75, 185, 255, 0.5);
}

.card-status-dot.fault {
  background: #ff7a7a;
  box-shadow: 0 0 8px rgba(255, 122, 122, 0.6);
}

.card-status-dot.offline {
  background: #4a5a6a;
}

.card-status-text {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.card-status-text.running { color: #7dffd1; }
.card-status-text.standby { color: #8ad9ff; }
.card-status-text.fault { color: #ffaeae; }
.card-status-text.offline { color: #6a7a8a; }

/* ==================== 动画 ==================== */
@keyframes gridDrift {
  0% { transform: translate3d(0, 0, 0); }
  100% { transform: translate3d(18px, 10px, 0); }
}

@keyframes horizonPulse {
  0%, 100% { opacity: 0.35; transform: scaleX(0.94); }
  50% { opacity: 0.82; transform: scaleX(1); }
}

@keyframes hudNodePulse {
  0%, 100% { opacity: 0.28; transform: scale(0.96); }
  50% { opacity: 0.62; transform: scale(1.02); }
}

@keyframes radarPing {
  0% { transform: translate(-50%, -50%) scale(0.68); opacity: 0.56; }
  80% { opacity: 0.12; }
  100% { transform: translate(-50%, -50%) scale(1.42); opacity: 0; }
}

@keyframes sweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* ==================== 滚动条 ==================== */
.device-list::-webkit-scrollbar { width: 5px; }
.device-list::-webkit-scrollbar-track { background: rgba(10, 30, 48, 0.4); }
.device-list::-webkit-scrollbar-thumb { background: rgba(80, 200, 255, 0.25); border-radius: 999px; }

.monitor-list::-webkit-scrollbar { width: 5px; }
.monitor-list::-webkit-scrollbar-track { background: rgba(10, 30, 48, 0.4); }
.monitor-list::-webkit-scrollbar-thumb { background: rgba(80, 200, 255, 0.25); border-radius: 999px; }

/* ==================== 响应式 ==================== */
@media (max-width: 1400px) {
  .command-deck {
    grid-template-columns: 260px minmax(0, 1fr) 260px;
  }
}

@media (max-width: 1024px) {
  .m3-page {
    height: auto;
    min-height: calc(100vh - var(--nav-height, 113px));
    overflow: auto;
  }

  .command-deck {
    flex: none;
    display: flex;
    flex-direction: column;
    overflow: visible;
  }

  .stage-panel {
    display: flex;
    flex-direction: column;
  }

  .hero-stage {
    min-height: 400px;
  }
}
.model-control-toggle { position: fixed; top: 80px; right: 16px; z-index: 100; padding: 6px 14px; border: 1px solid rgba(0,128,255,0.3); background: rgba(15,23,42,0.7); color: #8ceeff; font-size: 0.9375rem; cursor: pointer; border-radius: 6px; letter-spacing: 0.04em; transition: all 0.25s ease; }
.model-control-toggle:hover { border-color: rgba(0,128,255,0.6); background: rgba(15,23,42,0.9); color: #fff; }
.model-control-panel { position: fixed; top: 116px; right: 16px; z-index: 99; width: 300px; padding: 16px; border: 1px solid rgba(0,128,255,0.22); background: rgba(8,24,40,0.92); border-radius: 10px; box-shadow: inset 0 1px 0 rgba(168,240,255,0.06), 0 12px 32px rgba(0,0,0,0.3); max-height: calc(100vh - 160px); overflow-y: auto; }
.control-title { font-size: 1rem; font-weight: 700; color: #d8f7ff; margin-bottom: 10px; letter-spacing: 0.04em; }
.control-divider-title { font-size: 0.8125rem; font-weight: 600; color: #5cb8d6; margin: 10px 0 6px; padding-bottom: 4px; border-bottom: 1px solid rgba(0,128,255,0.15); letter-spacing: 0.04em; }
.control-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.control-row label { width: 56px; font-size: 0.8125rem; color: #C9CDD4; flex-shrink: 0; }
.control-row input[type="range"] { flex: 1; height: 4px; -webkit-appearance: none; appearance: none; background: rgba(56,217,255,0.15); border-radius: 2px; outline: none; }
.control-row input[type="range"]::-webkit-slider-thumb { -webkit-appearance: none; width: 14px; height: 14px; border-radius: 50%; background: #38d9ff; box-shadow: 0 0 8px rgba(56,217,255,0.6); cursor: pointer; }
.slider-val { width: 40px; text-align: right; font-size: 0.8125rem; font-family: var(--font-data), monospace; color: #8ceeff; }
.control-actions { display: flex; gap: 8px; margin-top: 12px; }
.ctrl-btn { flex: 1; padding: 6px 10px; border: 1px solid rgba(0,128,255,0.2); background: rgba(20,60,90,0.4); color: #c0eaff; font-size: 0.875rem; cursor: pointer; border-radius: 6px; transition: all 0.2s ease; }
.ctrl-btn:hover { border-color: rgba(0,128,255,0.5); background: rgba(20,60,90,0.7); color: #fff; }
.ctrl-btn.save { border-color: rgba(48,230,173,0.3); color: #7dffd1; }
.ctrl-btn.save:hover { border-color: rgba(48,230,173,0.6); background: rgba(48,230,173,0.12); }
.ctrl-btn.reset { border-color: rgba(255,190,90,0.3); color: #ffd68f; }
.ctrl-btn.reset:hover { border-color: rgba(255,190,90,0.6); background: rgba(255,190,90,0.12); }
.ctrl-btn.locked { border-color: rgba(255,80,80,0.5); color: #ff8080; background: rgba(255,80,80,0.1); }
.ctrl-btn.locked:hover { border-color: rgba(255,80,80,0.7); background: rgba(255,80,80,0.18); }
.panel-slide-enter-active, .panel-slide-leave-active { transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1); }
.panel-slide-enter-from, .panel-slide-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
