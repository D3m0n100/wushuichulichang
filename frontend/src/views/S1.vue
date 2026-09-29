<template>
  <div class="s1-page">
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
      <!-- 左侧：工艺数据 -->
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

      <!-- 中间：工艺流程图 -->
      <main class="stage-panel">
        <section class="hero-stage">
          <div class="stage-frame">
            <div class="stage-hud-grid"></div>
            <div class="stage-badges">
              <div class="stage-badge"><span>工艺段</span><strong>生物除臭系统</strong></div>
            </div>
            <div class="frame-corner top-left"></div>
            <div class="frame-corner top-right"></div>
            <div class="frame-corner bottom-left"></div>
            <div class="frame-corner bottom-right"></div>
            <canvas ref="canvasRef" class="stage-canvas"></canvas>
            <ModelLoader :model-loaded="modelLoaded" :loading-progress="loadingProgress" />
          </div>
        </section>
      </main>

      <!-- 右侧：系统控制及设备状态 -->
      <aside class="side-panel right-panel">
        <section class="panel-block device-control-block">
          <div class="panel-title">系统控制</div>
          <div class="system-control-card">
            <div class="system-control-head">
              <span class="system-control-name">除臭系统一键启停</span>
              <span class="system-control-state" :class="systemOneKeyActive ? 'running' : 'stopped'">
                {{ systemOneKeyActive ? '绿色运行状态' : '红色停止状态' }}
              </span>
            </div>
            <div class="system-mode-row">
              <button
                class="system-mode-btn"
                :class="systemAutoMode ? 'auto' : 'manual'"
                :disabled="modeSwitchBusy"
                @click="toggleSystemMode"
              >
                {{ systemAutoMode ? '自动模式' : '手动模式' }}
              </button>
              <button
                class="system-estop-btn"
                :class="{ active: estopIndicatorActive }"
                :disabled="estopSwitchBusy"
                @click="toggleTouchscreenEstop"
              >
                {{ touchscreenEstopActive ? '解除触摸屏急停' : '触摸屏急停' }}
              </button>
            </div>
            <div class="estop-indicator" :class="{ active: estopIndicatorActive }">
              <span class="estop-indicator-dot"></span>
              <span>急停灯</span>
              <strong>{{ estopIndicatorActive ? '急停' : '正常' }}</strong>
            </div>
            <button
              class="system-one-key-btn"
              :class="systemOneKeyActive ? 'running' : 'stopped'"
              :disabled="systemToggleBusy || (!systemOneKeyActive && (!systemAutoMode || estopIndicatorActive))"
              @click="toggleOneKeySystem"
            >
              <span>{{ systemOneKeyActive ? '系统正在自动运行' : '系统当前已停止' }}</span>
              <strong>{{ systemOneKeyActive ? '一键停止' : '一键启动' }}</strong>
            </button>
            <div v-if="!systemAutoMode" class="system-control-hint">手动模式下禁止一键自动启停</div>
            <div v-else-if="estopIndicatorActive" class="system-control-hint danger">急停状态下禁止一键启动</div>
            <div class="frequency-control">
              <div class="frequency-readback">
                <span>风机频率反馈</span>
                <strong>{{ formatMonitorValue(fanFrequencyFeedback) }} Hz</strong>
              </div>
              <div class="frequency-write-row">
                <el-input-number
                  v-model="fanFrequencySet"
                  :min="DEO_FAN_FREQUENCY_MIN"
                  :max="DEO_FAN_FREQUENCY_MAX"
                  :step="0.1"
                  :precision="1"
                  controls-position="right"
                  class="frequency-input"
                  @focus="fanFrequencyEditing = true"
                  @blur="fanFrequencyEditing = false"
                />
                <button class="card-btn frequency-write-btn" :disabled="frequencyWriteBusy" @click="writeFanFrequency">写入</button>
              </div>
            </div>
          </div>

          <div class="panel-title device-status-title">设备状态</div>
          <div class="device-list">
            <div v-for="item in deviceEntries" :key="item.key" class="device-card" :class="{ fault: item.data.fault, running: isDeviceRunning(item.data) }">
              <div class="card-row-top">
                <span class="card-device-name">{{ item.data.name }}</span>
                <span v-if="!deviceReadStatus[item.key]" class="card-unread" title="未读取到数据">✕</span>
                <div class="card-status-group">
                  <span class="card-status-dot" :class="statusDotClass(item.data)"></span>
                  <span class="card-status-text" :class="statusDotClass(item.data)">{{ deviceStateText(item.data) }}</span>
                </div>
              </div>
              <div v-if="item.data.type === 'valve'" class="card-valve-status">
                <span class="valve-pos" :class="{ on: item.data.open }">开到位</span>
                <span class="valve-pos" :class="{ on: item.data.close }">关到位</span>
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
import request from '@/api/request'
import ModelLoader from '@/components/ModelLoader.vue'
import {
  DEO_FAN_FREQUENCY_MAX,
  DEO_FAN_FREQUENCY_MIN,
  isDeodorizationFanFrequencyInRange,
} from '@/utils/deodorizationFrequency'

const safeRead = (key, fallback) => { try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback } catch { return fallback } }

const isMobileDevice = ref(false)
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
const savedCameraConfig = safeRead('S1_CAMERA_V1', null)
const sliderModelX = ref(savedCameraConfig?.model?.x ?? 0)
const sliderModelY = ref(savedCameraConfig?.model?.y ?? 0)
const sliderModelZ = ref(savedCameraConfig?.model?.z ?? 0)

const checkMobile = () => {
  const ua = navigator.userAgent
  isMobileDevice.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua) || window.innerWidth < 1024
}

// 除臭设备状态（后端 DeodorizationPlcService 注册的只读设备编码）
const defaultDeviceData = {
  DEO_FAN: { name: '除臭风机', type: 'motor', run: false, fault: false },
  DEO_PREWASH_PUMP_1: { name: '预洗一泵', type: 'motor', run: false, fault: false },
  DEO_PREWASH_PUMP_2: { name: '预洗二泵', type: 'motor', run: false, fault: false },
  DEO_BIO_PUMP: { name: '生物泵(加湿泵)', type: 'motor', run: false, fault: false },
  DEO_PREWASH_HEATER: { name: '预洗电加热', type: 'motor', run: false, fault: false },
  DEO_BIO_HEATER: { name: '生物电加热', type: 'motor', run: false, fault: false },
  DEO_PREWASH_FILL_VALVE: { name: '预洗补水阀', type: 'valve', open: false, close: true, run: false, fault: false },
  DEO_PREWASH_DRAIN_VALVE: { name: '预洗排水阀', type: 'valve', open: false, close: true, run: false, fault: false },
  DEO_BIO_FILL_VALVE: { name: '生物补水阀', type: 'valve', open: false, close: true, run: false, fault: false },
  DEO_BIO_DRAIN_VALVE: { name: '生物排水阀', type: 'valve', open: false, close: true, run: false, fault: false },
}

// 除臭参数（后端 DeodorizationPlcService 注册的参数编码）
const defaultMonitorData = {
  DEO_PREWASH_PH_VD300: { name: '预洗PH值', value: 0, unit: '' },
  DEO_BIO_PH_VD304: { name: '生物PH值', value: 0, unit: '' },
  DEO_PREWASH_LEVEL_VD308: { name: '预洗液位', value: 0, unit: 'm' },
  DEO_BIO_LEVEL_VD312: { name: '生物液位', value: 0, unit: 'm' },
  DEO_PREWASH_TEMP_VD316: { name: '预洗温度', value: 0, unit: '℃' },
  DEO_BIO_TEMP_VD320: { name: '生物温度', value: 0, unit: '℃' },
  DEO_WIND_PRESSURE_VD324: { name: '风压值', value: 0, unit: 'kPa' },
  DEO_FAN_FREQ_VD328: { name: '风机频率', value: 0, unit: 'Hz' },
  DEO_H2S_VD332: { name: '硫化氢', value: 0, unit: 'ppm' },
  DEO_NH3_VD336: { name: '氨气', value: 0, unit: 'ppm' },
  DEO_PREWASH_FLOW_VD340: { name: '预洗流量', value: 0, unit: 'm³/h' },
  DEO_BIO_FLOW_VD344: { name: '生物流量', value: 0, unit: 'm³/h' },
}

const storageKeys = { device: 'S1_DEVICE_V2', monitor: 'S1_MONITOR_V8' }

const savedDeviceData = safeRead(storageKeys.device, {})
const deviceData = ref(Object.fromEntries(
  Object.entries(defaultDeviceData).map(([key, defaults]) => [key, { ...(savedDeviceData[key] || {}), ...defaults }]),
))
const monitorData = ref({ ...defaultMonitorData, ...safeRead(storageKeys.monitor, {}) })
const fanFrequencySet = ref(0)
const fanFrequencyEditing = ref(false)
const frequencyWriteBusy = ref(false)
const systemOneKeyActive = ref(false)
const systemAutoMode = ref(false)
const touchscreenEstopActive = ref(false)
const estopIndicatorActive = ref(false)
const systemToggleBusy = ref(false)
const modeSwitchBusy = ref(false)
const estopSwitchBusy = ref(false)
const fanFrequencyFeedback = computed(() => monitorData.value.DEO_FAN_FREQ_VD328?.value ?? 0)

const deviceEntries = computed(() => {
  const entries = Object.entries(deviceData.value).map(([key, data]) => ({ key, data }))
  console.log('[S1] deviceEntries:', entries.map(e => ({ key: e.key, name: e.data.name, manualParam: e.data.manualParam })))
  return entries
})
const monitorEntries = computed(() => Object.entries(monitorData.value).map(([key, data]) => ({ key, data })))
const formatMonitorValue = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(3) : value
}
const asPlcBoolean = (value) => value === true || value === 1 || value === '1' || String(value).toLowerCase() === 'true'

const plcConnected = ref(false)
const plcIp = ref('192.168.0.29')
const deviceReadStatus = ref(Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])))
let pollTimer = null
let consecutiveFailures = 0

const fetchOverview = async () => {
  try {
    const res = await request.get('/plc/deodorization/overview')
    if (!res.data?.success) {
      console.warn('[S1] 除臭 overview 返回失败:', res.data?.message)
      return
    }
    const { devices, parameters } = res.data.data
    const readCodes = {}
    if (devices && devices.length > 0) {
      consecutiveFailures = 0
      plcConnected.value = true
      devices.forEach(d => {
        readCodes[d.code] = true
        const local = deviceData.value[d.code]
        if (!local) {
          console.warn('[S1] 未知设备编码:', d.code)
          return
        }
        local.fault = !!d.fault
        if (local.type === 'valve') {
          local.open = !!d.openPosition
          local.close = !!d.closePosition
        } else {
          local.run = !!d.running
        }
      })
    } else {
      console.warn('[S1] 未获取到设备数据')
    }
    if (parameters) {
      parameters.forEach(p => {
        if (p.code === 'DEO_FAN_FREQ_SET_VD472' && !fanFrequencyEditing.value) {
          const setValue = Number(p.value)
          if (Number.isFinite(setValue)) fanFrequencySet.value = setValue
        }
        if (p.code === 'DEO_SYSTEM_ONE_KEY_START_STOP_V2_5') systemOneKeyActive.value = asPlcBoolean(p.value)
        if (p.code === 'DEO_SYSTEM_MODE_SWITCH_V0_2') systemAutoMode.value = asPlcBoolean(p.value)
        if (p.code === 'DEO_TOUCHSCREEN_ESTOP_V0_1') touchscreenEstopActive.value = asPlcBoolean(p.value)
        if (p.code === 'DEO_ESTOP_INDICATOR_V0_0') estopIndicatorActive.value = asPlcBoolean(p.value)
        // 更新仪表数据
        const local = monitorData.value[p.code]
        if (local) local.value = p.value ?? 0
        // 从参数中读取阀门关到位状态
        if (p.code.endsWith('_CLOSE_FB')) {
          const deviceCode = p.code.replace('_CLOSE_FB', '')
          const device = deviceData.value[deviceCode]
          if (device && device.type === 'valve') {
            device.close = !!p.value
          }
        }
      })
    }
    deviceReadStatus.value = readCodes
  } catch (e) {
    consecutiveFailures++
    if (consecutiveFailures >= 3) { plcConnected.value = false }
    console.error(`[S1] 除臭 overview 获取失败 (连续${consecutiveFailures}次):`, e.message)
  }
}

const isDeviceRunning = (d) => { if (!d) return false; if (d.type === 'valve') return d.open; return d.run }
const deviceStateText = (d) => { if (!d) return '离线'; if (d.fault) return '故障'; if (d.type === 'valve') return d.open ? '已开启' : '已关闭'; return d.run ? '运行' : '待机' }
const statusDotClass = (d) => { if (!d) return 'offline'; if (d.fault) return 'fault'; if (d.type === 'valve') return d.open ? 'running' : 'standby'; return d.run ? 'running' : 'standby' }

const writeBooleanParameter = async (code, enabled) => {
  await request.post(`/plc/deodorization/parameters/${code}`, null, { params: { value: enabled ? 1 : 0 } })
}

const toggleOneKeySystem = async () => {
  if (!systemOneKeyActive.value && !systemAutoMode.value) return ElMessage.warning('请先切换到自动模式')
  if (!systemOneKeyActive.value && estopIndicatorActive.value) return ElMessage.warning('急停状态下不能一键启动')
  const actionText = systemOneKeyActive.value ? '停止' : '启动'
  try {
    await ElMessageBox.confirm(`确认一键${actionText}除臭系统？动作顺序由PLC执行。`, '系统操作确认', {
      confirmButtonText: `确认${actionText}`,
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  systemToggleBusy.value = true
  try {
    await writeBooleanParameter('DEO_SYSTEM_ONE_KEY_START_STOP_V2_5', !systemOneKeyActive.value)
    ElMessage.success(`除臭系统一键${actionText}指令已发送`)
    setTimeout(fetchOverview, 500)
  } catch (e) {
    console.error(`[S1] 除臭系统一键${actionText}失败:`, e)
    ElMessage.error(`除臭系统一键${actionText}失败`)
  } finally {
    systemToggleBusy.value = false
  }
}

const toggleSystemMode = async () => {
  const targetMode = systemAutoMode.value ? '手动模式' : '自动模式'
  try {
    await ElMessageBox.confirm(`确认切换除臭系统到${targetMode}？`, '模式切换确认', {
      confirmButtonText: '确认切换',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  modeSwitchBusy.value = true
  try {
    await writeBooleanParameter('DEO_SYSTEM_MODE_SWITCH_V0_2', !systemAutoMode.value)
    ElMessage.success(`除臭系统${targetMode}切换指令已发送`)
    setTimeout(fetchOverview, 500)
  } catch (e) {
    console.error('[S1] 除臭系统模式切换失败:', e)
    ElMessage.error('除臭系统模式切换失败')
  } finally {
    modeSwitchBusy.value = false
  }
}

const toggleTouchscreenEstop = async () => {
  const actionText = touchscreenEstopActive.value ? '解除' : '触发'
  try {
    await ElMessageBox.confirm(`确认${actionText}触摸屏急停？`, '急停操作确认', {
      confirmButtonText: `确认${actionText}`,
      cancelButtonText: '取消',
      type: 'error',
    })
  } catch {
    return
  }

  estopSwitchBusy.value = true
  try {
    await writeBooleanParameter('DEO_TOUCHSCREEN_ESTOP_V0_1', !touchscreenEstopActive.value)
    ElMessage.success(`触摸屏急停${actionText}指令已发送`)
    setTimeout(fetchOverview, 500)
  } catch (e) {
    console.error(`[S1] 触摸屏急停${actionText}失败:`, e)
    ElMessage.error(`触摸屏急停${actionText}失败`)
  } finally {
    estopSwitchBusy.value = false
  }
}

const writeFanFrequency = async () => {
  const value = Number(fanFrequencySet.value)
  if (!isDeodorizationFanFrequencyInRange(value)) {
    return ElMessage.warning(`除臭风机频率给定范围为 ${DEO_FAN_FREQUENCY_MIN}-${DEO_FAN_FREQUENCY_MAX}Hz`)
  }
  try {
    await ElMessageBox.confirm(`确认将风机频率设定写入为 ${value.toFixed(1)} Hz？`, '参数写入确认', {
      confirmButtonText: '确认写入',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  frequencyWriteBusy.value = true
  try {
    await request.post('/plc/deodorization/parameters/DEO_FAN_FREQ_SET_VD472', null, { params: { value } })
    ElMessage.success('风机频率已下发')
    fanFrequencyEditing.value = false
    setTimeout(fetchOverview, 500)
  } catch (e) {
    console.error('[S1] 风机频率写入失败:', e)
    ElMessage.error('风机频率下发失败')
  } finally {
    frequencyWriteBusy.value = false
  }
}

watch(deviceData, () => { localStorage.setItem(storageKeys.device, JSON.stringify(deviceData.value)) }, { deep: true })
watch(monitorData, () => { localStorage.setItem(storageKeys.monitor, JSON.stringify(monitorData.value)) }, { deep: true })

// ── 设备颜色映射 ──
const DEVICE_COLOR_MAP = [
  { key: 'DEO_PREWASH_PUMP_1', keyword: '1号循环泵' },
  { key: 'DEO_PREWASH_PUMP_2', keyword: '2号循环泵' },
  { key: 'DEO_BIO_PUMP', keyword: '3号循环泵' },
]

// ── Babylon.js 3D 场景 ──
let engine = null, scene = null, camera = null, importedMeshes = [], rootMesh = null
const originalMaterials = new Map()
const findMeshesByName = (meshes, keyword) => meshes.filter(m => m.name.includes(keyword))
const ensureUniqueMaterial = (mesh) => {
  if (!mesh.material) return
  if (!originalMaterials.has(mesh.name)) {
    const cloned = mesh.material.clone(mesh.name + '_mat')
    originalMaterials.set(mesh.name, { material: cloned, origColor: mesh.material.albedoColor.clone() })
    mesh.material = cloned
  }
}
const syncDeviceColors = () => {
  for (const { key, keyword } of DEVICE_COLOR_MAP) {
    const meshes = findMeshesByName(importedMeshes, keyword)
    if (!meshes.length) continue
    const d = deviceData.value[key]
    if (!d) continue
    meshes.forEach(mesh => {
      ensureUniqueMaterial(mesh)
      if (!mesh.material) return
      if (d.fault) { mesh.material.albedoColor = new BABYLON.Color3(1, 0.15, 0.15); mesh.material.emissiveColor = new BABYLON.Color3(0.8, 0, 0) }
      else if (d.run) { mesh.material.albedoColor = new BABYLON.Color3(0, 1, 0); mesh.material.emissiveColor = new BABYLON.Color3(0, 0, 0) }
      else { const saved = originalMaterials.get(mesh.name); if (saved) mesh.material.albedoColor = saved.origColor.clone(); mesh.material.emissiveColor = new BABYLON.Color3(0, 0, 0) }
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
const toggleCameraLock = () => {
  cameraLock.value = !cameraLock.value
  if (!camera) return
  if (cameraLock.value) { camera.detachControl(); if (canvasRef.value) canvasRef.value.style.pointerEvents = 'none' }
  else { camera.attachControl(canvasRef.value, true); if (canvasRef.value) canvasRef.value.style.pointerEvents = 'auto' }
  ElMessage.success(cameraLock.value ? '视角已锁定' : '视角已解锁')
}
const resetCamera = () => { if (!camera) return; camera.alpha = 3.2; camera.beta = 0.9; camera.radius = 120; camera.setTarget(BABYLON.Vector3.Zero()); if (rootMesh) rootMesh.position = new BABYLON.Vector3(0, 0, 0); syncSliders() }
const onSaveConfig = () => {
  if (!camera) return
  const config = { alpha: camera.alpha, beta: camera.beta, radius: camera.radius, target: { x: camera.target.x, y: camera.target.y, z: camera.target.z }, position: { x: camera.position.x, y: camera.position.y, z: camera.position.z }, model: rootMesh ? { x: rootMesh.position.x, y: rootMesh.position.y, z: rootMesh.position.z } : { x: 0, y: 0, z: 0 } }
  localStorage.setItem('S1_CAMERA_V1', JSON.stringify(config)); ElMessage.success('配置已保存')
}
const handleResize = () => { if (engine) engine.resize() }

const initScene = async () => {
  await nextTick()
  const canvas = canvasRef.value; if (!canvas) return
  engine = new BABYLON.Engine(canvas, true, { preserveDrawingBuffer: true, stencil: true, antialias: true })
  scene = new BABYLON.Scene(engine); scene.clearColor = new BABYLON.Color4(0, 0, 0, 0)
  const savedConfig = safeRead('S1_CAMERA_V1', null)
  const initAlpha = savedConfig?.alpha ?? 3.2, initBeta = savedConfig?.beta ?? 0.9, initRadius = savedConfig?.radius ?? 120
  const initTarget = savedConfig?.target ? new BABYLON.Vector3(savedConfig.target.x, savedConfig.target.y, savedConfig.target.z) : BABYLON.Vector3.Zero()
  camera = new BABYLON.ArcRotateCamera('s1-camera', initAlpha, initBeta, initRadius, initTarget, scene)
  camera.lowerRadiusLimit = 5; camera.upperRadiusLimit = 600; camera.wheelDeltaPercentage = 0.008
  if (savedConfig?.position) camera.setPosition(new BABYLON.Vector3(savedConfig.position.x, savedConfig.position.y, savedConfig.position.z))
  if (cameraLock.value) { camera.detachControl(); canvas.style.pointerEvents = 'none' } else { camera.attachControl(canvas, true); canvas.style.pointerEvents = 'auto' }
  camera.onViewMatrixChangedObservable.add(() => syncSliders())
  const ambientLight = new BABYLON.HemisphericLight('ambient', BABYLON.Vector3.Up(), scene); ambientLight.intensity = 0.3
  const mainLight = new BABYLON.DirectionalLight('main', new BABYLON.Vector3(-1, -1.5, 0.5), scene); mainLight.position = new BABYLON.Vector3(8, 15, 10); mainLight.intensity = 0.8
  const fillLight = new BABYLON.DirectionalLight('fill', new BABYLON.Vector3(0.5, -0.2, -1), scene); fillLight.position = new BABYLON.Vector3(-5, 8, -12); fillLight.intensity = 0.2
  const bottomLight = new BABYLON.DirectionalLight('bottom', new BABYLON.Vector3(0, 1, 0), scene); bottomLight.position = new BABYLON.Vector3(0, -5, 0); bottomLight.intensity = 0.15
  const hdr = BABYLON.CubeTexture.CreateFromPrefilteredData('/environmentSpecular.env', scene); scene.environmentTexture = hdr
  BABYLON.SceneLoader.ImportMesh('', '/', '生物除臭系统.max1024.glb', scene, (meshes) => {
    importedMeshes = meshes; meshes.forEach(m => { m.receiveShadows = true; if (m.material) m.material.backFaceCulling = false })
    rootMesh = meshes.find(m => m.name === '__root__')
    if (rootMesh) { rootMesh.scaling = new BABYLON.Vector3(-1, 1, 1); rootMesh.position.x = sliderModelX.value; rootMesh.position.y = sliderModelY.value; rootMesh.position.z = sliderModelZ.value }
    if (camera && meshes.length) { let minV = null, maxV = null; meshes.forEach(m => { if (!m.getBoundingInfo) return; const b = m.getBoundingInfo().boundingBox; minV = minV ? BABYLON.Vector3.Minimize(minV, b.minimumWorld) : b.minimumWorld.clone(); maxV = maxV ? BABYLON.Vector3.Maximize(maxV, b.maximumWorld) : b.maximumWorld.clone() }); if (minV && maxV) { const size = Math.max(maxV.x - minV.x, maxV.y - minV.y, maxV.z - minV.z, 1); camera.lowerRadiusLimit = Math.max(size * 0.2, 5); camera.upperRadiusLimit = Math.max(size * 5, 600) } }
    modelLoaded.value = true; syncDeviceColors(); syncSliders(); console.log('[S1] 模型已加载，网格:', meshes.map(m => m.name).join(', '))
  }, (evt) => { if (evt.lengthComputable) loadingProgress.value = Math.min(95, Math.round((evt.loaded / evt.total) * 100)) }, () => { loadingProgress.value = 100; setTimeout(() => { modelLoaded.value = true }, 100) })
  engine.runRenderLoop(() => { if (scene) scene.render() })
}

onMounted(() => { checkMobile(); if (!isMobileDevice.value) initScene(); window.addEventListener('resize', handleResize); fetchOverview(); pollTimer = setInterval(fetchOverview, 3000) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); if (pollTimer) clearInterval(pollTimer); if (scene) { scene.dispose(); scene = null }; if (engine) { engine.dispose(); engine = null }; camera = null })
</script>

<style scoped>
.s1-page { position: relative; height: calc(100vh - var(--nav-height, 113px)); display: flex; flex-direction: column; padding: 12px 14px 14px; background: linear-gradient(180deg, #0F172A 0%, #0F172A 50%, #0F172A 100%); color: #F2F3F5; overflow: hidden; }
.scene-background { position: absolute; inset: 0; z-index: 0; overflow: hidden; }
.stage-canvas { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 0; touch-action: none; outline: none; pointer-events: none; }
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
.page-aurora, .page-noise, .page-vignette, .page-gridlines, .page-flare, .hud-node { position: absolute; inset: 0; pointer-events: none; }
.page-aurora { opacity: 0.52; }
.aurora-left { background: radial-gradient(circle at 14% 26%, rgba(43,229,255,0.12), transparent 26%); }
.aurora-right { background: radial-gradient(circle at 88% 18%, rgba(104,112,255,0.12), transparent 24%); }
.page-noise { opacity: 0.08; background-image: linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px); background-size: 42px 42px; mask-image: linear-gradient(180deg, rgba(255,255,255,0.8), transparent 90%); }
.page-vignette { background: radial-gradient(circle at 50% 48%, transparent 0%, rgba(16,56,88,0.04) 54%, rgba(16,56,88,0.10) 100%), linear-gradient(90deg, rgba(11,42,68,0.14), transparent 16%, transparent 84%, rgba(11,42,68,0.14)); z-index: 0; }
.page-gridlines { opacity: 0.16; background-image: linear-gradient(rgba(111,221,255,0.045) 1px, transparent 1px), linear-gradient(90deg, rgba(111,221,255,0.045) 1px, transparent 1px); background-size: 64px 64px; mask-image: radial-gradient(circle at 50% 52%, rgba(255,255,255,0.36), transparent 92%); animation: gridDrift 24s linear infinite; z-index: 0; }
.page-flare { inset: auto 10% auto; height: 2px; background: linear-gradient(90deg, transparent, rgba(103,230,255,0.7), transparent); box-shadow: 0 0 18px rgba(103,230,255,0.2), 0 0 40px rgba(53,148,255,0.12); opacity: 0.7; z-index: 0; }
.flare-top { top: 14px; animation: horizonPulse 7s ease-in-out infinite; }
.flare-bottom { bottom: 12px; opacity: 0.45; animation: horizonPulse 9s ease-in-out infinite reverse; }
.hud-node { inset: auto; width: 72px; height: 72px; border-radius: 50%; border: 1px solid rgba(88,219,255,0.1); background: radial-gradient(circle at center, rgba(116,236,255,0.14), transparent 42%), conic-gradient(from 0deg, rgba(116,236,255,0), rgba(116,236,255,0.48), rgba(116,236,255,0)); box-shadow: 0 0 18px rgba(71,200,255,0.14), inset 0 0 18px rgba(71,200,255,0.08); opacity: 0.52; animation: hudNodePulse 5.8s ease-in-out infinite; z-index: 0; }
.hud-node::before, .hud-node::after { content: ''; position: absolute; inset: 50%; transform: translate(-50%, -50%); border-radius: 50%; }
.hud-node::before { width: 8px; height: 8px; background: #7ae8ff; box-shadow: 0 0 16px rgba(122,232,255,0.7); }
.hud-node::after { width: 100%; height: 100%; border: 1px solid rgba(122,232,255,0.18); animation: radarPing 4.2s ease-out infinite; }
.node-top-left { left: 18px; top: 18px; }
.node-top-right { right: 18px; top: 18px; animation-delay: 1.2s; }
.node-bottom-left { left: 18px; bottom: 18px; animation-delay: 2.2s; }
.node-bottom-right { right: 18px; bottom: 18px; animation-delay: 3.1s; }
.command-deck { position: relative; flex: 1; display: grid; grid-template-columns: 300px minmax(0, 1fr) 300px; gap: 14px; min-height: 0; z-index: 2; }
.side-panel { position: relative; display: flex; flex-direction: column; gap: 14px; min-height: 0; z-index: 2; isolation: isolate; }
.left-panel::before { content: ''; position: absolute; top: 14px; bottom: 14px; right: 2px; width: 1px; pointer-events: none; background: linear-gradient(180deg, transparent, rgba(85,220,255,0.44), transparent); box-shadow: 0 0 16px rgba(85,220,255,0.18); }
.right-panel::before { content: ''; position: absolute; top: 14px; bottom: 14px; left: 2px; width: 1px; pointer-events: none; background: linear-gradient(180deg, transparent, rgba(85,220,255,0.44), transparent); box-shadow: 0 0 16px rgba(85,220,255,0.18); }
.right-panel { overflow: auto; }
.device-control-block { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: auto; }
.system-control-card { position: relative; z-index: 2; margin: 10px 12px 4px; padding: 12px; border: 1px solid rgba(55,203,255,0.34); background: rgba(9,28,46,0.78); clip-path: polygon(0 0, calc(100% - 9px) 0, 100% 9px, 100% 100%, 9px 100%, 0 calc(100% - 9px)); }
.system-control-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.system-control-name { color: #e8fbff; font-size: 0.875rem; font-weight: 700; }
.system-control-state { font-size: 0.75rem; font-weight: 700; }
.system-control-state.running { color: #7dffd1; }
.system-control-state.stopped { color: #ff9f9f; }
.system-mode-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 10px; }
.system-mode-btn, .system-estop-btn { min-width: 0; min-height: 32px; padding: 5px 8px; border-radius: 4px; border: 1px solid; font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease; }
.system-mode-btn.manual { color: #9fddff; border-color: rgba(75,185,255,0.58); background: rgba(28,112,170,0.24); }
.system-mode-btn.auto { color: #92ffd5; border-color: rgba(48,230,173,0.58); background: rgba(28,150,102,0.24); }
.system-estop-btn { color: #ffb0b0; border-color: rgba(255,100,100,0.45); background: rgba(126,30,30,0.2); }
.system-estop-btn.active { color: #fff; border-color: #ff6767; background: rgba(198,43,43,0.72); box-shadow: 0 0 14px rgba(255,74,74,0.36); }
.estop-indicator { display: grid; grid-template-columns: 8px 1fr auto; align-items: center; gap: 7px; margin-top: 8px; padding: 5px 8px; border: 1px solid rgba(75,185,255,0.2); color: #9fc7dc; font-size: 0.6875rem; }
.estop-indicator-dot { width: 8px; height: 8px; border-radius: 50%; background: #4bb9ff; box-shadow: 0 0 7px rgba(75,185,255,0.5); }
.estop-indicator strong { color: #8ad9ff; }
.estop-indicator.active { border-color: rgba(255,100,100,0.55); background: rgba(146,32,32,0.22); }
.estop-indicator.active .estop-indicator-dot { background: #ff6767; box-shadow: 0 0 9px rgba(255,74,74,0.72); }
.estop-indicator.active strong { color: #ffaeae; }
.system-one-key-btn { width: 100%; min-height: 50px; margin-top: 9px; padding: 7px 10px; border: 1px solid; border-radius: 4px; cursor: pointer; transition: all 0.2s ease; }
.system-one-key-btn span, .system-one-key-btn strong { display: block; }
.system-one-key-btn span { font-size: 0.6875rem; opacity: 0.84; }
.system-one-key-btn strong { margin-top: 3px; font-size: 0.875rem; }
.system-one-key-btn.running { color: #eafff7; border-color: rgba(48,230,173,0.74); background: rgba(27,151,100,0.52); box-shadow: 0 0 14px rgba(48,230,173,0.22); }
.system-one-key-btn.stopped { color: #fff1f1; border-color: rgba(255,100,100,0.68); background: rgba(157,45,45,0.48); box-shadow: 0 0 14px rgba(255,91,91,0.16); }
.system-one-key-btn:disabled, .system-mode-btn:disabled, .system-estop-btn:disabled { cursor: not-allowed; opacity: 0.48; box-shadow: none; }
.system-control-hint { margin-top: 6px; color: #8ad9ff; font-size: 0.6875rem; text-align: center; }
.system-control-hint.danger { color: #ffaeae; }
.frequency-control { margin-top: 11px; padding-top: 10px; border-top: 1px solid rgba(85,220,255,0.16); }
.frequency-readback { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; color: #9fc7dc; font-size: 0.75rem; }
.frequency-readback strong { color: #8ceeff; font-size: 0.875rem; }
.frequency-write-row { display: grid; grid-template-columns: minmax(0, 1fr) 58px; gap: 8px; margin-top: 8px; }
.frequency-input { width: 100%; }
.frequency-write-btn { color: #8ceeff; border-color: rgba(100,180,255,0.4); }
.device-status-title { margin-top: 12px; font-size: 0.9375rem; }
.device-list { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 6px; padding: 8px 12px 12px; overflow-y: auto; }
.device-card { position: relative; border: 1px solid rgba(0,128,255,0.22); background: rgba(15,23,42,0.6); padding: 10px 12px; clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); transition: all 0.25s ease; }
.device-card:hover { border-color: rgba(0,200,255,0.38); background: rgba(15,23,42,0.8); }
.device-card.fault { border-color: rgba(255,80,80,0.45); }
.device-card.running {
  border-color: rgba(48,230,173,0.68);
  background: rgba(48,230,173,0.06);
  box-shadow: inset 0 0 0 1px rgba(48,230,173,0.32), 0 0 14px rgba(48,230,173,0.26), 0 0 26px rgba(48,230,173,0.12);
}
.card-row-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.card-device-name { font-size: 0.875rem; font-weight: 700; color: #e8fbff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-shadow: 0 0 10px rgba(61,184,255,0.12); flex: 1; }
.card-unread { font-size: 0.875rem; color: #ff5050; font-weight: 700; flex-shrink: 0; opacity: 0.8; line-height: 1; }
.card-status-group { display: flex; align-items: center; gap: 5px; flex-shrink: 0; }
.card-status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.card-status-dot.running { background: #30e6ad; box-shadow: 0 0 8px rgba(48,230,173,0.6); }
.card-status-dot.standby { background: #4bb9ff; box-shadow: 0 0 8px rgba(75,185,255,0.5); }
.card-status-dot.fault { background: #ff7a7a; box-shadow: 0 0 8px rgba(255,122,122,0.6); }
.card-status-dot.offline { background: #4a5a6a; }
.card-status-text { font-size: 0.6875rem; font-weight: 600; letter-spacing: 0.04em; }
.card-status-text.running { color: #7dffd1; }
.card-status-text.standby { color: #8ad9ff; }
.card-status-text.fault { color: #ffaeae; }
.card-status-text.offline { color: #6a7a8a; }
.card-valve-status { display: flex; gap: 12px; padding-top: 6px; }
.valve-pos { font-size: 0.6875rem; font-weight: 600; color: #4a5a6a; padding: 2px 8px; border: 1px solid rgba(74,90,106,0.3); border-radius: 3px; transition: all 0.3s ease; }
.valve-pos.on { color: #7dffd1; border-color: rgba(48,230,173,0.4); background: rgba(48,230,173,0.08); text-shadow: 0 0 6px rgba(48,230,173,0.4); }
.card-row-bottom { display: flex; align-items: center; gap: 6px; padding-top: 8px; }
.card-btn { padding: 4px 10px; font-size: 0.75rem; font-weight: 600; border: 1px solid rgba(0,128,255,0.25); background: rgba(20,50,70,0.5); color: #c0eaff; cursor: pointer; border-radius: 4px; transition: all 0.2s ease; white-space: nowrap; }
.card-btn:hover { border-color: rgba(0,200,255,0.5); background: rgba(30,70,100,0.6); color: #fff; }
.card-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.card-btn.start-btn { border-color: rgba(48,230,173,0.3); color: #7dffd1; }
.card-btn.start-btn:hover { border-color: rgba(48,230,173,0.6); background: rgba(20,60,40,0.5); }
.card-btn.stop-btn { border-color: rgba(255,100,100,0.3); color: #ffaeae; }
.card-btn.stop-btn:hover { border-color: rgba(255,100,100,0.6); background: rgba(60,20,20,0.5); }
.card-btn.mode-btn { border-color: rgba(255,190,90,0.3); color: #ffd68f; }
.card-btn.mode-btn:hover { border-color: rgba(255,190,90,0.6); background: rgba(60,45,15,0.5); }
.card-btn.reset-btn { border-color: rgba(100,180,255,0.3); color: #8ceeff; }
.card-btn.reset-btn:hover { border-color: rgba(100,180,255,0.6); background: rgba(20,50,80,0.5); }
.device-list::-webkit-scrollbar { width: 5px; }
.device-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.device-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
.stage-panel { position: relative; min-width: 0; min-height: 0; z-index: 2; }
.panel-block, .stage-frame { position: relative; border: 1px solid rgba(0,128,255,0.18); background: rgba(15,23,42,0.88); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.04), inset 0 8px 18px rgba(120,214,255,0.02), 0 8px 20px rgba(0,0,0,0.06), 0 0 0 1px rgba(147,228,255,0.03), 0 0 22px rgba(91,182,255,0.03); backdrop-filter: saturate(118%); overflow: hidden; clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px)); }
.panel-block::before, .stage-frame::before { content: ''; position: absolute; inset: 0; pointer-events: none; background: linear-gradient(90deg, transparent, rgba(170,240,255,0.10), transparent); transform: translateX(-100%); animation: sweep 14s linear infinite; }
.panel-block::after, .stage-frame::after { content: ''; position: absolute; inset: 1px; pointer-events: none; border: 1px solid rgba(168,240,255,0.14); clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px)); }
.panel-block::before { animation-duration: 10s; }
.panel-block:hover, .stage-frame:hover { box-shadow: inset 0 0 0 1px rgba(255,255,255,0.06), inset 0 12px 24px rgba(33,127,214,0.04), 0 12px 24px rgba(0,0,0,0.10), 0 0 0 1px rgba(74,219,255,0.08), 0 0 26px rgba(44,126,255,0.06); }
.panel-block::after { box-shadow: inset 0 0 22px rgba(87,194,255,0.04), inset 0 1px 0 rgba(143,228,255,0.08); }
.panel-title { position: relative; z-index: 2; display: inline-flex; align-items: center; gap: 10px; margin: 14px 16px 0; padding-right: 8px; font-size: 1.125rem; font-weight: 700; color: #d8f7ff; letter-spacing: 0.04em; text-shadow: 0 0 14px rgba(74,210,255,0.18), 0 0 22px rgba(74,210,255,0.05); }
.panel-title::before { content: ''; width: 26px; height: 3px; background: linear-gradient(90deg, #2ce1ff, #86f8c8 55%, rgba(134,248,200,0.2)); box-shadow: 0 0 14px rgba(44,225,255,0.46); }
.process-data-block { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.monitor-list { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 6px; padding: 8px 12px 12px; overflow-y: auto; }
.monitor-item { padding: 10px 12px; border: 1px solid rgba(0,128,255,0.18); background: rgba(15,23,42,0.6); clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); }
.monitor-head { display: flex; justify-content: space-between; align-items: center; }
.monitor-name { font-size: 0.875rem; font-weight: 600; color: #e8fbff; }
.monitor-val { font-size: 1rem; font-weight: 700; font-family: var(--font-display); color: #8ceeff; }
.hero-stage { height: 100%; min-height: 0; }
.stage-frame { height: 100%; background: transparent; backdrop-filter: none; border-color: rgba(0,128,255,0.10); box-shadow: none; display: flex; flex-direction: column; }
.stage-frame::before, .stage-frame::after { display: none; }
.stage-frame:hover { box-shadow: none; background: transparent; }
.stage-canopy { display: none; }
.stage-orbit { position: absolute; pointer-events: none; z-index: 2; border-radius: 50%; border: 1px dashed rgba(85,220,255,0.12); box-shadow: inset 0 0 30px rgba(85,220,255,0.03); }
.orbit-left { left: -72px; bottom: 48px; width: 220px; height: 220px; }
.orbit-right { right: -84px; top: 92px; width: 300px; height: 300px; }
.stage-hud-grid { position: absolute; inset: 0; pointer-events: none; background-image: linear-gradient(rgba(92,229,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(92,229,255,0.04) 1px, transparent 1px); background-size: 72px 72px; mask-image: radial-gradient(circle at 50% 52%, rgba(255,255,255,0.5), transparent 88%); animation: gridDrift 28s linear infinite; z-index: 1; }
.stage-badges { position: absolute; left: 18px; top: 18px; z-index: 4; display: flex; gap: 10px; flex-wrap: wrap; }
.stage-badge { min-width: 156px; padding: 10px 12px; border: 1px solid rgba(0,128,255,0.28); background: linear-gradient(90deg, rgba(0,128,255,0.08), transparent 42%), linear-gradient(180deg, rgba(30,41,59,0.12), rgba(30,41,59,0.05)); backdrop-filter: saturate(135%); clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px)); }
.stage-badge span { display: block; font-size: 0.875rem; color: #84b6d1; }
.stage-badge strong { display: block; margin-top: 6px; font-size: 1rem; color: #F2F3F5; }
.frame-corner { position: absolute; width: 34px; height: 34px; border: 2px solid rgba(79,218,255,0.68); z-index: 5; opacity: 0.58; box-shadow: 0 0 10px rgba(79,218,255,0.12); }
.frame-corner.top-left { top: 12px; left: 12px; border-right: none; border-bottom: none; }
.frame-corner.top-right { top: 12px; right: 12px; border-left: none; border-bottom: none; }
.frame-corner.bottom-left { bottom: 12px; left: 12px; border-right: none; border-top: none; }
.frame-corner.bottom-right { right: 12px; bottom: 12px; border-left: none; border-top: none; }
.monitor-list::-webkit-scrollbar { width: 5px; }
.monitor-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.monitor-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
@keyframes gridDrift { 0% { transform: translate3d(0,0,0); } 100% { transform: translate3d(18px,10px,0); } }
@keyframes horizonPulse { 0%, 100% { opacity: 0.35; transform: scaleX(0.94); } 50% { opacity: 0.82; transform: scaleX(1); } }
@keyframes hudNodePulse { 0%, 100% { opacity: 0.28; transform: scale(0.96); } 50% { opacity: 0.62; transform: scale(1.02); } }
@keyframes radarPing { 0% { transform: translate(-50%,-50%) scale(0.68); opacity: 0.56; } 80% { opacity: 0.12; } 100% { transform: translate(-50%,-50%) scale(1.42); opacity: 0; } }
@keyframes sweep { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }
@media (max-width: 1400px) { .command-deck { grid-template-columns: 260px minmax(0, 1fr) 260px; } }
@media (max-width: 1024px) { .s1-page { height: auto; min-height: calc(100vh - var(--nav-height, 113px)); overflow: auto; } .command-deck { flex: none; display: flex; flex-direction: column; overflow: visible; } .stage-panel { display: flex; flex-direction: column; } .hero-stage { min-height: 400px; } }
</style>
