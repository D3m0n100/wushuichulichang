<template>
  <div class="t3-page">
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
              <div class="stage-badge"><span>工艺段</span><strong>污泥浓缩池</strong></div>
            </div>
            <div class="frame-corner top-left"></div>
            <div class="frame-corner top-right"></div>
            <div class="frame-corner bottom-left"></div>
            <div class="frame-corner bottom-right"></div>
          </div>
        </section>
      </main>

      <aside class="side-panel right-panel">
        <section class="panel-block device-control-block">
          <div class="panel-title">设备控制</div>
          <div class="device-list">
            <div v-for="item in deviceEntries" :key="item.key" class="device-card" :class="{ fault: item.data.fault, running: isDeviceRunning(item.data) }">
              <div class="card-row-top">
                <span class="card-device-name">{{ item.data.name }}</span>
                <span v-if="!deviceReadStatus[item.key]" class="card-unread" title="未读取到数据">✕</span>
                <div class="knob-switch">
                  <span class="knob-label-left" :class="{ active: !item.data.remote }">就地</span>
                  <div class="knob-dial" :class="{ remote: item.data.remote }"><div class="knob-pointer"></div></div>
                  <span class="knob-label-right" :class="{ active: item.data.remote }">远程</span>
                </div>
              </div>
              <div class="card-row-bottom">
                <button class="card-btn mode-btn" :class="{ manual: item.data.manualMode }" @click="toggleManualMode(item.key, item.data.manualMode)">{{ item.data.manualMode ? '手动' : '自动' }}</button>
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

const safeRead = (key, fallback) => { try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback } catch { return fallback } }

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
const savedCameraConfig = safeRead('T3_CAMERA_V1', null)
const sliderModelX = ref(savedCameraConfig?.model?.x ?? 0)
const sliderModelY = ref(savedCameraConfig?.model?.y ?? 0)
const sliderModelZ = ref(savedCameraConfig?.model?.z ?? 0)

const token = localStorage.getItem('token')
let role = 'user'
try { role = jwt_decode(token || '{}')?.role || 'user' } catch { role = 'user' }

const checkMobile = () => {
  const ua = navigator.userAgent
  isMobileDevice.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua) || window.innerWidth < 1024
}

// T3点位表设备：1个刮泥机
const defaultDeviceData = {
  T3_HN1: { name: '污泥浓缩池刮泥机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
}

// T3点位表仪表：1个污泥界面仪
const defaultMonitorData = {
  T3_RIT01: { name: '污泥界面仪', value: 0, unit: 'm' },
}

const defaultPinPositions = {
  T3_HN1: { left: '40%', top: '40%' },
}

const storageKeys = {
  device: 'T3_DEVICE_V5', monitor: 'T3_MONITOR_V5',
  pinLock: 'T3_PIN_LOCK_V2', pins: 'T3_PINS_V5'
}

const deviceData = ref({ ...defaultDeviceData, ...safeRead(storageKeys.device, {}) })
const monitorData = ref({ ...defaultMonitorData, ...safeRead(storageKeys.monitor, {}) })
const pinPositions = ref({ ...defaultPinPositions, ...safeRead(storageKeys.pins, {}) })
pinLock.value = localStorage.getItem(storageKeys.pinLock) !== 'false'

const deviceEntries = computed(() => Object.entries(deviceData.value).map(([key, data]) => ({ key, data })))
const monitorEntries = computed(() => Object.entries(monitorData.value).map(([key, data]) => ({ key, data })))
const formatMonitorValue = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(3) : value
}
const stagePins = computed(() => deviceEntries.value.filter(({ key }) => pinPositions.value[key]).map(({ key, data }) => ({ key, label: data.name, left: pinPositions.value[key]?.left || '50%', top: pinPositions.value[key]?.top || '50%', data, value: deviceStateText(data), statusClass: statusDotClass(data) })))
const isDeviceRunning = (d) => { if (!d) return false; if (d.type === 'valve') return d.open; return d.run }
const deviceStateText = (d) => { if (!d) return '离线'; if (d.fault) return '故障'; if (d.type === 'valve') return d.open ? '已开启' : '已关闭'; return d.run ? '运行' : '待机' }
const statusDotClass = (d) => { if (!d) return 'offline'; if (d.fault) return 'fault'; if (d.type === 'valve') return d.open ? 'running' : 'standby'; return d.run ? 'running' : 'standby' }
const canControl = (d) => role === 'admin' && d?.remote && d?.manualMode

const plcConnected = ref(false)
const plcIp = ref('192.168.0.106')
const deviceReadStatus = ref(Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])))
let pollTimer = null
let consecutiveFailures = 0

const fetchOverview = async () => {
  try {
    const res = await request.get('/plc/b1/overview')
    if (!res.data?.success) return
    const { devices, instruments } = res.data.data
    const readCodes = {}
    if (devices && devices.length > 0) {
      devices.forEach(d => {
        const local = deviceData.value[d.code]
        if (!local) return
        readCodes[d.code] = true
        if (d.code && d.code.startsWith('T3_') && !plcConnected.value) { consecutiveFailures = 0; plcConnected.value = true }
        local.remote = !!d.remote; local.fault = !!d.fault
        if (local.type === 'valve') { local.open = !!d.openPosition; local.close = !!d.closePosition } else { local.run = !!d.running }
        // PLC: 手动=0, 自动=1, 取反后前端 manualMode=true 表示手动
        local.manualMode = !d.manualModeBit
      })
    }
    if (instruments) { instruments.forEach(inst => { const local = monitorData.value[inst.code]; if (!local) return; local.value = inst.output ?? 0 }) }
    deviceReadStatus.value = readCodes
  } catch (e) { consecutiveFailures++; if (consecutiveFailures >= 3) { plcConnected.value = false; deviceReadStatus.value = Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])) }; console.error(`T3 overview 获取失败 (连续${consecutiveFailures}次):`, e.message) }
}

const startPolling = () => { fetchOverview(); pollTimer = setInterval(fetchOverview, 3000) }
const selectDevice = (key) => { if (deviceData.value[key]) selectedDeviceKey.value = key }
const togglePinLock = () => { pinLock.value = !pinLock.value; savePinLock(); ElMessage.success(pinLock.value ? '标签位置已锁定' : '标签位置已解锁') }

const dragTarget = ref(null)
let dragOffset = { x: 0, y: 0 }
const startDrag = (e, key) => { if (pinLock.value) return; dragTarget.value = key; const frame = e.currentTarget.closest('.stage-frame'); if (!frame) return; const rect = frame.getBoundingClientRect(); const pinLeft = parseFloat(pinPositions.value[key]?.left || '50') / 100 * rect.width; const pinTop = parseFloat(pinPositions.value[key]?.top || '50') / 100 * rect.height; dragOffset = { x: e.clientX - rect.left - pinLeft, y: e.clientY - rect.top - pinTop } }
const onDrag = (e) => { if (!dragTarget.value) return; const frame = document.querySelector('.stage-frame'); if (!frame) return; const rect = frame.getBoundingClientRect(); const x = e.clientX - rect.left - dragOffset.x; const y = e.clientY - rect.top - dragOffset.y; pinPositions.value[dragTarget.value] = { left: `${Math.min(95, Math.max(5, (x / rect.width) * 100)).toFixed(2)}%`, top: `${Math.min(88, Math.max(10, (y / rect.height) * 100)).toFixed(2)}%` } }
const stopDrag = () => { dragTarget.value = null }

const toggleRemote = async () => { ElMessage.info('就地/远程状态由现场旋钮控制，只读显示') }
// T3 PLC (b1后端): 手动=0, 自动=1, enabled 直接写入 PLC 位值
const toggleManualMode = async (key, currentMode) => { const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (!it.remote) return ElMessage.warning('该设备未授权远程控制'); const enabled = currentMode; try { await request.post(`/plc/b1/devices/${key}/manual-mode`, null, { params: { enabled } }); ElMessage.success('手自动已切换'); setTimeout(fetchOverview, 500); setTimeout(fetchOverview, 1500) } catch (e) { ElMessage.error('手自动切换失败') } }
const startMotor = async (key) => { const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (!it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式'); await ElMessageBox.confirm(`确认启动「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }); try { await request.post(`/plc/b1/devices/${key}/command`, null, { params: { action: 'START', pulseMs: 300 } }); ElMessage.success('启动指令已发送'); fetchOverview() } catch (e) { ElMessage.error('启动失败') } }
const stopMotor = async (key) => { const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (!it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式'); await ElMessageBox.confirm(`确认停止「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }); try { await request.post(`/plc/b1/devices/${key}/command`, null, { params: { action: 'STOP', pulseMs: 300 } }); ElMessage.success('停止指令已发送'); fetchOverview() } catch (e) { ElMessage.error('停止失败') } }
const openValve = async (key) => { const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (!it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式'); await ElMessageBox.confirm(`确认开启「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }); try { await request.post(`/plc/b1/devices/${key}/command`, null, { params: { action: 'OPEN', pulseMs: 300 } }); ElMessage.success('开阀指令已发送'); fetchOverview() } catch (e) { ElMessage.error('开阀失败') } }
const closeValve = async (key) => { const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (!it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式'); await ElMessageBox.confirm(`确认关闭「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }); try { await request.post(`/plc/b1/devices/${key}/command`, null, { params: { action: 'CLOSE', pulseMs: 300 } }); ElMessage.success('关阀指令已发送'); fetchOverview() } catch (e) { ElMessage.error('关阀失败') } }

const savePinPositions = () => localStorage.setItem(storageKeys.pins, JSON.stringify(pinPositions.value))
const savePinLock = () => localStorage.setItem(storageKeys.pinLock, String(pinLock.value))

watch(deviceData, () => { localStorage.setItem(storageKeys.device, JSON.stringify(deviceData.value)) }, { deep: true })
watch(monitorData, () => { localStorage.setItem(storageKeys.monitor, JSON.stringify(monitorData.value)) }, { deep: true })
watch(pinPositions, () => { localStorage.setItem(storageKeys.pins, JSON.stringify(pinPositions.value)) }, { deep: true })

// ── Babylon.js 3D 场景 ──
let engine = null, scene = null, camera = null, importedMeshes = [], rootMesh = null
const STIRRER_MAP = { T3_HN1: '刮泥机' }
const MOTOR_COLOR_MAP = { T3_HN1: '刮泥机电机' }
let stirrerMeshes = {}
let motorColorMeshes = {}
const originalModelMaterials = new Map()
const statusMaterialCache = new Map()

const bindT3ModelMeshes = () => {
  const issues = []
  stirrerMeshes = {}
  motorColorMeshes = {}
  for (const [key, meshName] of Object.entries(STIRRER_MAP)) {
    const matches = importedMeshes.filter(mesh => mesh.name === meshName)
    if (matches.length !== 1) issues.push(`${key} 旋转 mesh ${meshName}: 匹配到 ${matches.length} 个`)
    if (matches[0]) stirrerMeshes[key] = matches[0]
  }
  for (const [key, meshName] of Object.entries(MOTOR_COLOR_MAP)) {
    const matches = importedMeshes.filter(mesh => mesh.material && mesh.name === meshName)
    if (matches.length !== 1) issues.push(`${key} 着色 mesh ${meshName}: 匹配到 ${matches.length} 个`)
    if (matches[0]) {
      motorColorMeshes[key] = matches[0]
      originalModelMaterials.set(matches[0].uniqueId, matches[0].material)
    }
  }
  if (issues.length) console.error('[T3] 设备模型映射审计失败:', issues)
  else console.info('[T3] 设备模型映射审计通过：刮泥机旋转与电机着色 mesh 均唯一')
}

const getMotorStatusMaterial = (mesh, status) => {
  const cacheKey = `${mesh.uniqueId}:${status}`
  if (statusMaterialCache.has(cacheKey)) return statusMaterialCache.get(cacheKey)
  const material = new BABYLON.StandardMaterial(`${mesh.name}-${status}`, scene)
  const color = status === 'running'
    ? new BABYLON.Color3(0, 1, 0)
    : new BABYLON.Color3(1, 0.08, 0.08)
  material.diffuseColor = color.clone()
  material.emissiveColor = color.scale(status === 'running' ? 0.35 : 0.45)
  material.specularColor = BABYLON.Color3.Black()
  material.backFaceCulling = false
  statusMaterialCache.set(cacheKey, material)
  return material
}

const syncMotorColor = () => {
  for (const [key, mesh] of Object.entries(motorColorMeshes)) {
    const device = deviceData.value[key]
    const originalMaterial = originalModelMaterials.get(mesh.uniqueId)
    if (!device || deviceReadStatus.value[key] !== true) mesh.material = originalMaterial
    else if (device.fault) mesh.material = getMotorStatusMaterial(mesh, 'fault')
    else if (device.run) mesh.material = getMotorStatusMaterial(mesh, 'running')
    else mesh.material = originalMaterial
  }
}

watch(deviceData, syncMotorColor, { deep: true })
watch(deviceReadStatus, syncMotorColor, { deep: true })

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
  localStorage.setItem('T3_CAMERA_V1', JSON.stringify(config)); ElMessage.success('配置已保存')
}
const handleResize = () => { if (engine) engine.resize() }

const initScene = async () => {
  await nextTick()
  const canvas = canvasRef.value; if (!canvas) return
  engine = new BABYLON.Engine(canvas, true, { preserveDrawingBuffer: true, stencil: true, antialias: true })
  scene = new BABYLON.Scene(engine); scene.clearColor = new BABYLON.Color4(0, 0, 0, 0)
  const savedConfig = safeRead('T3_CAMERA_V1', null)
  const initAlpha = savedConfig?.alpha ?? 3.2, initBeta = savedConfig?.beta ?? 0.9, initRadius = savedConfig?.radius ?? 120
  const initTarget = savedConfig?.target ? new BABYLON.Vector3(savedConfig.target.x, savedConfig.target.y, savedConfig.target.z) : BABYLON.Vector3.Zero()
  camera = new BABYLON.ArcRotateCamera('t3-camera', initAlpha, initBeta, initRadius, initTarget, scene)
  camera.lowerRadiusLimit = 5; camera.upperRadiusLimit = 600; camera.wheelDeltaPercentage = 0.008
  if (savedConfig?.position) camera.setPosition(new BABYLON.Vector3(savedConfig.position.x, savedConfig.position.y, savedConfig.position.z))
  if (cameraLock.value) { camera.detachControl(); canvas.style.pointerEvents = 'none' } else { camera.attachControl(canvas, true); canvas.style.pointerEvents = 'auto' }
  camera.onViewMatrixChangedObservable.add(() => syncSliders())
  const ambientLight = new BABYLON.HemisphericLight('ambient', BABYLON.Vector3.Up(), scene); ambientLight.intensity = 0.3
  const mainLight = new BABYLON.DirectionalLight('main', new BABYLON.Vector3(-1, -1.5, 0.5), scene); mainLight.position = new BABYLON.Vector3(8, 15, 10); mainLight.intensity = 0.8
  const fillLight = new BABYLON.DirectionalLight('fill', new BABYLON.Vector3(0.5, -0.2, -1), scene); fillLight.position = new BABYLON.Vector3(-5, 8, -12); fillLight.intensity = 0.2
  const bottomLight = new BABYLON.DirectionalLight('bottom', new BABYLON.Vector3(0, 1, 0), scene); bottomLight.position = new BABYLON.Vector3(0, -5, 0); bottomLight.intensity = 0.15
  const hdr = BABYLON.CubeTexture.CreateFromPrefilteredData('/environmentSpecular.env', scene); scene.environmentTexture = hdr
  BABYLON.SceneLoader.ImportMesh('', '/', '污泥浓缩池及储泥池.max1024.glb', scene, (meshes) => {
    importedMeshes = meshes; meshes.forEach(m => { m.receiveShadows = true; if (m.material) m.material.backFaceCulling = false })
    rootMesh = meshes.find(m => m.name === '__root__')
    if (rootMesh) { rootMesh.scaling = new BABYLON.Vector3(-1, 1, 1); rootMesh.position.x = sliderModelX.value; rootMesh.position.y = sliderModelY.value; rootMesh.position.z = sliderModelZ.value }
    if (camera && meshes.length) { let minV = null, maxV = null; meshes.forEach(m => { if (!m.getBoundingInfo) return; const b = m.getBoundingInfo().boundingBox; minV = minV ? BABYLON.Vector3.Minimize(minV, b.minimumWorld) : b.minimumWorld.clone(); maxV = maxV ? BABYLON.Vector3.Maximize(maxV, b.maximumWorld) : b.maximumWorld.clone() }); if (minV && maxV) { const size = Math.max(maxV.x - minV.x, maxV.y - minV.y, maxV.z - minV.z, 1); camera.lowerRadiusLimit = Math.max(size * 0.2, 5); camera.upperRadiusLimit = Math.max(size * 5, 600) } }
    bindT3ModelMeshes()
    modelLoaded.value = true; syncMotorColor(); syncSliders(); console.log('[T3] 模型已加载，网格:', meshes.map(m => m.name).join(', '))
  }, (evt) => { if (evt.lengthComputable) loadingProgress.value = Math.min(95, Math.round((evt.loaded / evt.total) * 100)) }, () => { loadingProgress.value = 100; setTimeout(() => { modelLoaded.value = true }, 100) })
  engine.runRenderLoop(() => {
    for (const [key, mesh] of Object.entries(stirrerMeshes)) {
      const d = deviceData.value[key]
      if (d && d.run && !d.fault) mesh.rotation.y += 0.05
    }
    if (scene) scene.render()
  })
}

onMounted(() => { checkMobile(); if (!isMobileDevice.value) initScene(); window.addEventListener('mousemove', onDrag); window.addEventListener('mouseup', stopDrag); window.addEventListener('resize', handleResize); startPolling() })
onUnmounted(() => { window.removeEventListener('mousemove', onDrag); window.removeEventListener('mouseup', stopDrag); window.removeEventListener('resize', handleResize); if (pollTimer) clearInterval(pollTimer); if (scene) { scene.dispose(); scene = null }; if (engine) { engine.dispose(); engine = null }; camera = null })
</script>

<style scoped>
.t3-page { position: relative; height: calc(100vh - var(--nav-height, 113px)); display: flex; flex-direction: column; padding: 12px 14px 14px; background: linear-gradient(180deg, #0F172A 0%, #0F172A 50%, #0F172A 100%); color: #F2F3F5; overflow: hidden; }
.scene-background { position: absolute; inset: 0; z-index: 0; overflow: hidden; }
.stage-canvas { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 0; touch-action: none; outline: none; pointer-events: none; }
.scene-bg-shade { position: absolute; inset: 0; background: radial-gradient(circle at 50% 50%, rgba(15,23,42,0.02), rgba(15,23,42,0.08)), linear-gradient(180deg, rgba(15,23,42,0.03), rgba(15,23,42,0.06)); pointer-events: none; z-index: 1; }
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
.command-deck { position: relative; flex: 1; display: grid; grid-template-columns: 300px minmax(0, 1fr) 300px; gap: 14px; min-height: 0; overflow: hidden; z-index: 2; }
.side-panel { position: relative; display: flex; flex-direction: column; gap: 14px; overflow: hidden; z-index: 2; isolation: isolate; }
.left-panel::before { content: ''; position: absolute; top: 14px; bottom: 14px; right: 2px; width: 1px; pointer-events: none; background: linear-gradient(180deg, transparent, rgba(85,220,255,0.44), transparent); box-shadow: 0 0 16px rgba(85,220,255,0.18); }
.right-panel::before { content: ''; position: absolute; top: 14px; bottom: 14px; left: 2px; width: 1px; pointer-events: none; background: linear-gradient(180deg, transparent, rgba(85,220,255,0.44), transparent); box-shadow: 0 0 16px rgba(85,220,255,0.18); }
.right-panel { overflow: hidden; }
.stage-panel { position: relative; min-width: 0; z-index: 2; }
.panel-block, .stage-frame { position: relative; border: 1px solid rgba(0,128,255,0.18); background: rgba(15,23,42,0.88); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.04), inset 0 8px 18px rgba(120,214,255,0.02), 0 8px 20px rgba(0,0,0,0.06), 0 0 0 1px rgba(147,228,255,0.03), 0 0 22px rgba(91,182,255,0.03); backdrop-filter: saturate(118%); overflow: hidden; clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px)); }
.panel-block::before, .stage-frame::before { content: ''; position: absolute; inset: 0; pointer-events: none; background: linear-gradient(90deg, transparent, rgba(170,240,255,0.10), transparent); transform: translateX(-100%); animation: sweep 14s linear infinite; }
.panel-block::after, .stage-frame::after { content: ''; position: absolute; inset: 1px; pointer-events: none; border: 1px solid rgba(168,240,255,0.14); clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px)); }
.panel-block::before { animation-duration: 10s; }
.panel-block:hover, .stage-frame:hover { box-shadow: inset 0 0 0 1px rgba(255,255,255,0.06), inset 0 12px 24px rgba(33,127,214,0.04), 0 12px 24px rgba(0,0,0,0.10), 0 0 0 1px rgba(74,219,255,0.08), 0 0 26px rgba(44,126,255,0.06); }
.panel-block::after { box-shadow: inset 0 0 22px rgba(87,194,255,0.04), inset 0 1px 0 rgba(143,228,255,0.08); }
.panel-title { position: relative; z-index: 2; display: inline-flex; align-items: center; gap: 10px; margin: 14px 16px 0; padding-right: 8px; font-size: 1.125rem; font-weight: 700; color: #d8f7ff; letter-spacing: 0.04em; text-shadow: 0 0 14px rgba(74,210,255,0.18), 0 0 22px rgba(74,210,255,0.05); }
.panel-title::before { content: ''; width: 26px; height: 3px; background: linear-gradient(90deg, #2ce1ff, #86f8c8 55%, rgba(134,248,200,0.2)); box-shadow: 0 0 14px rgba(44,225,255,0.46); }
.process-data-block { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.monitor-list { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 6px; padding: 8px 12px 12px; overflow-y: auto; }
.monitor-item { padding: 10px 12px; border: 1px solid rgba(0,128,255,0.18); background: rgba(15,23,42,0.6); clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); }
.monitor-head { display: flex; justify-content: space-between; align-items: center; }
.monitor-name { font-size: 0.875rem; font-weight: 600; color: #e8fbff; }
.monitor-val { font-size: 1rem; font-weight: 700; font-family: var(--font-display); color: #8ceeff; }
.hero-stage { height: 100%; }
.stage-frame { height: 100%; background: transparent; backdrop-filter: none; border-color: rgba(0,128,255,0.10); box-shadow: none; display: flex; flex-direction: column; }
.stage-frame::before, .stage-frame::after { display: none; }
.stage-frame:hover { box-shadow: none; background: transparent; }
.stage-canopy { display: none; }
.stage-orbit { position: absolute; pointer-events: none; z-index: 2; border-radius: 50%; border: 1px dashed rgba(85,220,255,0.12); box-shadow: inset 0 0 30px rgba(85,220,255,0.03); }
.orbit-left { left: -72px; bottom: 48px; width: 220px; height: 220px; }
.orbit-right { right: -84px; top: 92px; width: 300px; height: 300px; }
.stage-hud-grid { position: absolute; inset: 0; pointer-events: none; background-image: linear-gradient(rgba(92,229,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(92,229,255,0.04) 1px, transparent 1px); background-size: 72px 72px; mask-image: radial-gradient(circle at 50% 52%, rgba(255,255,255,0.5), transparent 88%); animation: gridDrift 28s linear infinite; z-index: 1; }
.stage-badges { position: absolute; left: 18px; top: 18px; z-index: 4; display: flex; gap: 10px; flex-wrap: wrap; }
.stage-badge { min-width: 156px; padding: 10px 12px; border: 1px solid rgba(0,128,255,0.28); background: linear-gradient(90deg, rgba(0,128,255,0.08), transparent 42%), linear-gradient(180deg, rgba(30,41,59,0.12), rgba(30,41,59,0.05)); backdrop-filter: saturate(135%); box-shadow: 0 6px 14px rgba(0,0,0,0.06), inset 0 0 0 1px rgba(255,255,255,0.02), inset 0 0 18px rgba(74,198,255,0.04); clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px)); }
.stage-badge span { display: block; font-size: 0.875rem; color: #84b6d1; }
.stage-badge strong { display: block; margin-top: 6px; font-size: 1rem; color: #F2F3F5; }
.frame-corner { position: absolute; width: 34px; height: 34px; border: 2px solid rgba(79,218,255,0.68); z-index: 5; opacity: 0.58; box-shadow: 0 0 10px rgba(79,218,255,0.12); }
.frame-corner.top-left { top: 12px; left: 12px; border-right: none; border-bottom: none; }
.frame-corner.top-right { top: 12px; right: 12px; border-left: none; border-bottom: none; }
.frame-corner.bottom-left { bottom: 12px; left: 12px; border-right: none; border-top: none; }
.frame-corner.bottom-right { right: 12px; bottom: 12px; border-left: none; border-top: none; }
.stage-pin-layer { position: absolute; inset: 0; z-index: 6; pointer-events: none; }
.stage-pin { position: absolute; z-index: 6; min-width: 0; padding: 4px 8px 5px 7px; border: 1px solid rgba(92,211,255,0.18); background: linear-gradient(180deg, rgba(10,36,56,0.14), transparent 40%), linear-gradient(180deg, rgba(5,17,29,0.88), rgba(3,12,23,0.8)); color: #effcff; transform: translate(-50%, -50%); cursor: pointer; transition: 0.2s ease; clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); display: inline-flex; align-items: center; gap: 6px; max-width: none; pointer-events: auto; }
.stage-pin:hover, .stage-pin.active { border-color: rgba(117,236,255,0.42); box-shadow: 0 8px 18px rgba(0,0,0,0.18), 0 0 18px rgba(56,200,255,0.12); }
.stage-pin.fault { border-color: rgba(255,126,126,0.65); }
.stage-pin::after { content: ''; position: absolute; left: 14px; bottom: -14px; width: 1px; height: 14px; background: linear-gradient(180deg, rgba(117,236,255,0.75), transparent); }
.pin-name { display: block; font-size: 0.75rem; font-weight: 700; line-height: 1; letter-spacing: 0.04em; color: #F2F3F5; white-space: nowrap; }
.device-control-block { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.device-list { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 6px; padding: 8px 12px 12px; overflow-y: auto; }
.device-card { position: relative; border: 1px solid rgba(0,128,255,0.22); background: rgba(15,23,42,0.6); padding: 0; display: flex; flex-direction: column; clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); transition: all 0.25s ease; }
.device-card:hover { border-color: rgba(0,200,255,0.38); background: rgba(15,23,42,0.8); }
.device-card.fault { border-color: rgba(255,80,80,0.45); }
.device-card.running {
  border-color: rgba(48,230,173,0.68);
  background: rgba(48,230,173,0.06);
  box-shadow: inset 0 0 0 1px rgba(48,230,173,0.32), 0 0 14px rgba(48,230,173,0.26), 0 0 26px rgba(48,230,173,0.12);
}
.card-row-top { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px 8px; border-bottom: 1px solid rgba(0,128,255,0.1); }
.card-device-name { font-size: 0.875rem; font-weight: 700; color: #e8fbff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-shadow: 0 0 10px rgba(61,184,255,0.12); }
.card-unread { font-size: 0.875rem; color: #ff5050; font-weight: 700; flex-shrink: 0; opacity: 0.8; line-height: 1; }
.knob-switch { display: flex; align-items: center; gap: 6px; user-select: none; }
.knob-label-left, .knob-label-right { font-size: 0.6875rem; font-weight: 600; color: #4a5a6a; letter-spacing: 0.04em; transition: all 0.3s ease; }
.knob-label-left.active { color: #ffd68f; text-shadow: 0 0 8px rgba(255,214,143,0.6), 0 0 16px rgba(255,190,90,0.3); }
.knob-label-right.active { color: #7dffd1; text-shadow: 0 0 8px rgba(125,255,209,0.6), 0 0 16px rgba(48,230,173,0.3); }
.knob-dial { width: 28px; height: 28px; border-radius: 50%; border: 2px solid rgba(102,153,180,0.3); background: radial-gradient(circle at 40% 35%, #3a4a5a, #1a2a3a); position: relative; transition: all 0.4s ease; box-shadow: 0 2px 6px rgba(0,0,0,0.3), inset 0 1px 2px rgba(255,255,255,0.05); }
.knob-dial.remote { border-color: rgba(48,230,173,0.4); box-shadow: 0 0 10px rgba(48,230,173,0.15), 0 2px 6px rgba(0,0,0,0.3); }
.knob-pointer { position: absolute; width: 3px; height: 10px; background: #c0d0e0; border-radius: 1px; top: 3px; left: 50%; transform: translateX(-50%) rotate(-45deg); transform-origin: center 11px; transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); box-shadow: 0 0 4px rgba(192,208,224,0.5); }
.knob-dial.remote .knob-pointer { transform: translateX(-50%) rotate(45deg); background: #7dffd1; box-shadow: 0 0 6px rgba(125,255,209,0.6); }
.card-row-bottom { display: flex; align-items: center; gap: 6px; padding: 8px 12px 10px; }
.card-btn { padding: 4px 10px; font-size: 0.75rem; font-weight: 600; border: 1px solid rgba(0,128,255,0.25); background: rgba(20,50,70,0.5); color: #c0eaff; cursor: pointer; border-radius: 4px; transition: all 0.2s ease; white-space: nowrap; }
.card-btn:hover:not(:disabled) { border-color: rgba(0,200,255,0.5); background: rgba(30,70,100,0.6); color: #fff; }
.card-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.card-btn.mode-btn { min-width: 40px; }
.card-btn.mode-btn.manual { border-color: rgba(255,190,90,0.4); color: #ffd68f; background: rgba(60,45,15,0.5); }
.card-btn.start-btn { border-color: rgba(48,230,173,0.3); color: #7dffd1; }
.card-btn.start-btn:hover:not(:disabled) { border-color: rgba(48,230,173,0.6); background: rgba(20,60,40,0.5); }
.card-btn.stop-btn { border-color: rgba(255,100,100,0.3); color: #ffaeae; }
.card-btn.stop-btn:hover:not(:disabled) { border-color: rgba(255,100,100,0.6); background: rgba(60,20,20,0.5); }
.card-status-group { display: flex; align-items: center; gap: 5px; margin-left: auto; }
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
.device-list::-webkit-scrollbar { width: 5px; }
.device-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.device-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
.monitor-list::-webkit-scrollbar { width: 5px; }
.monitor-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.monitor-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
@keyframes gridDrift { 0% { transform: translate3d(0,0,0); } 100% { transform: translate3d(18px,10px,0); } }
@keyframes horizonPulse { 0%, 100% { opacity: 0.35; transform: scaleX(0.94); } 50% { opacity: 0.82; transform: scaleX(1); } }
@keyframes hudNodePulse { 0%, 100% { opacity: 0.28; transform: scale(0.96); } 50% { opacity: 0.62; transform: scale(1.02); } }
@keyframes radarPing { 0% { transform: translate(-50%,-50%) scale(0.68); opacity: 0.56; } 80% { opacity: 0.12; } 100% { transform: translate(-50%,-50%) scale(1.42); opacity: 0; } }
@keyframes sweep { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }
@media (max-width: 1400px) { .command-deck { grid-template-columns: 260px minmax(0, 1fr) 260px; } }
@media (max-width: 1024px) { .t3-page { height: auto; min-height: calc(100vh - var(--nav-height, 113px)); overflow: auto; } .command-deck { flex: none; display: flex; flex-direction: column; overflow: visible; } .stage-panel { display: flex; flex-direction: column; } .hero-stage { min-height: 400px; } }
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
