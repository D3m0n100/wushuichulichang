<template>
  <div class="b1-page">
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
            <div class="stage-hud-grid"></div>
            <div class="stage-badges">
              <div class="stage-badge"><span>工艺段</span><strong>脱水车间 / 污泥料仓 / 压滤机</strong></div>
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

      <aside class="side-panel right-panel">
        <section class="panel-block device-control-block">
          <div class="panel-title">设备控制</div>
          <div class="device-list">
            <div v-for="item in deviceEntries" :key="item.key" class="device-card" :class="{ fault: item.data.fault, running: isDeviceRunning(item.data) }">
              <div class="card-row-top">
                <span class="card-device-name">{{ item.data.name }}</span>
                <span v-if="shouldShowUnread(item.key)" class="card-unread" title="未读取到数据">✕</span>
                <div v-if="!item.data.readOnly" class="knob-switch">
                  <span class="knob-label-left" :class="{ active: !item.data.remote }">就地</span>
                  <div class="knob-dial" :class="{ remote: item.data.remote }"><div class="knob-pointer"></div></div>
                  <span class="knob-label-right" :class="{ active: item.data.remote }">远程</span>
                </div>
                <span v-else-if="frequencyControls[item.key]" class="frequency-adjustable-tag">频率可调</span>
                <span v-else class="read-only-tag">只读</span>
              </div>
              <div v-if="item.data.type === 'valve'" class="card-valve-status">
                <span v-if="item.data.run !== undefined && !item.data.readOnly" class="valve-pos" :class="{ on: item.data.run }">运行中</span>
                <span class="valve-pos" :class="{ on: item.data.open }">开到位</span>
                <span class="valve-pos" :class="{ on: item.data.close }">关到位</span>
              </div>
              <div class="card-row-bottom">
                <template v-if="!item.data.readOnly">
                  <button class="card-btn mode-btn" :class="{ manual: item.data.manualMode }" @click="toggleManualMode(item.key, item.data.manualMode)">{{ item.data.manualMode ? '手动' : '自动' }}</button>
                  <template v-if="item.data.type === 'valve'">
                    <button class="card-btn start-btn" :disabled="!canControl(item.data)" @click="openValve(item.key)">开阀</button>
                    <button class="card-btn stop-btn" :disabled="!canControl(item.data)" @click="closeValve(item.key)">关阀</button>
                  </template>
                  <template v-else>
                    <button class="card-btn start-btn" :disabled="!canControl(item.data)" @click="startMotor(item.key)">启动</button>
                    <button class="card-btn stop-btn" :disabled="!canControl(item.data)" @click="stopMotor(item.key)">停止</button>
                  </template>
                </template>
                <div class="card-status-group">
                  <span class="card-status-dot" :class="statusDotClass(item.data)"></span>
                  <span class="card-status-text" :class="statusDotClass(item.data)">{{ deviceStateText(item.data) }}</span>
                </div>
              </div>
              <div v-if="item.key === 'D1_JLMOTOR'" class="frequency-feedback-row">
                <span class="frequency-feedback-label">进料电机频率显示</span>
                <strong class="frequency-feedback-value">{{ formatFrequencyValue(monitorData.D1_DOSING_FREQ.value) }} Hz</strong>
              </div>
              <div v-if="frequencyControls[item.key]" class="frequency-set-row">
                <span class="frequency-set-label">{{ frequencyControls[item.key].name }}</span>
                <div class="frequency-set-editor">
                  <input
                    v-model.number="frequencyControls[item.key].value"
                    class="frequency-set-input"
                    type="number"
                    step="0.1"
                    :aria-label="frequencyControls[item.key].name"
                    @focus="frequencyControls[item.key].editing = true"
                    @blur="frequencyControls[item.key].editing = false"
                  />
                  <span class="frequency-set-unit">Hz</span>
                  <button class="card-btn frequency-set-button" :disabled="role !== 'admin'" @click="writeFrequency(item.key)">写入</button>
                </div>
              </div>
              <!-- 压滤机一键启停 -->
              <div v-if="item.key === 'D1_ZJ1'" class="onekey-row">
                <span class="onekey-label">1#压滤机</span>
                <button class="card-btn start-btn" @click="sendOneKey('BFP_1_ONE_KEY_START')">一键启动</button>
                <button class="card-btn stop-btn" @click="sendOneKey('BFP_1_ONE_KEY_STOP')">一键停止</button>
              </div>
              <div v-if="item.key === 'D1_ZJ2'" class="onekey-row">
                <span class="onekey-label">2#压滤机</span>
                <button class="card-btn start-btn" @click="sendOneKey('BFP_2_ONE_KEY_START')">一键启动</button>
                <button class="card-btn stop-btn" @click="sendOneKey('BFP_2_ONE_KEY_STOP')">一键停止</button>
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
const savedCameraConfig = safeRead('D1_CAMERA_V1', null)
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

// 前端设备编码 → 后端设备编码映射（百科加药）
const backendCodeMap = {
  D1_JSDCF: 'BJD_INLET_VALVE',
  D1_JLMOTOR: 'BJD_FEED_MOTOR',
  D1_JB1: 'BJD_MIXER_1',
  D1_JB2: 'BJD_MIXER_2',
  D1_JB3: 'BJD_MIXER_3',
  D1_LCZD: 'BJD_HOPPER_VIBRATOR',
  D1_JRQ: 'BJD_HEATER',
  D1_JYB1: 'BJD_DOSING_PUMP_1',
  D1_JYB2: 'BJD_DOSING_PUMP_2',
}
// 反向映射：后端编码 → 前端编码
const frontendCodeMap = Object.fromEntries(Object.entries(backendCodeMap).map(([k, v]) => [v, k]))

// 后端参数编码 → 前端仪表编码映射（百科加药）
const monitorCodeMap = {
  BJD_LEVEL_DISPLAY: 'D1_DOSING_LEVEL',
  BJD_FEED_MOTOR_FREQ_DISPLAY: 'D1_DOSING_FREQ',
  BJD_INLET_FLOW_INSTANT: 'D1_DOSING_FLOW',
}

// 压滤机前端设备编码 → 后端设备编码映射
const filterBackendCodeMap = {
  D1_ZJ1: 'BFP_1_MAIN_MOTOR', D1_NS1: 'BFP_1_THICKENER_MOTOR', D1_CC1: 'BFP_1_BACKWASH_PUMP',
  D1_KY1: 'BFP_1_AIR_COMPRESSOR', D1_DF1: 'BFP_1_VALVE', D1_JN1: 'BFP_1_SLUDGE_PUMP', D1_XN1: 'BFP_1_FLOCC_MIXER',
  D1_ZJ2: 'BFP_2_MAIN_MOTOR', D1_NS2: 'BFP_2_THICKENER_MOTOR', D1_CC2: 'BFP_2_BACKWASH_PUMP',
  D1_KY2: 'BFP_2_AIR_COMPRESSOR', D1_DF2: 'BFP_2_VALVE', D1_JN2: 'BFP_2_SLUDGE_PUMP', D1_XN2: 'BFP_2_FLOCC_MIXER',
}
const filterFrontendCodeMap = Object.fromEntries(Object.entries(filterBackendCodeMap).map(([k, v]) => [v, k]))
const filterMonitorCodeMap = {
  BFP_1_MAIN_FREQ_DISPLAY: 'D1_FREQ_1_MAIN',
  BFP_1_THICKENER_FREQ_DISPLAY: 'D1_FREQ_1_THICK',
  BFP_2_MAIN_FREQ_DISPLAY: 'D1_FREQ_2_MAIN',
  BFP_2_THICKENER_FREQ_DISPLAY: 'D1_FREQ_2_THICK',
}
const frequencyControls = ref({
  D1_JLMOTOR: { code: 'BJD_FEED_MOTOR_FREQ_SET', name: '进料电机频率给定', value: 0, editing: false, read: false, apiBase: '/plc/baike-dosing' },
  D1_ZJ1: { code: 'BFP_1_MAIN_FREQ_SET', name: '1#带机主机频率给定', value: 0, editing: false, read: false },
  D1_NS1: { code: 'BFP_1_THICKENER_FREQ_SET', name: '1#带机浓缩机频率给定', value: 0, editing: false, read: false },
  D1_ZJ2: { code: 'BFP_2_MAIN_FREQ_SET', name: '2#带机主机频率给定', value: 0, editing: false, read: false },
  D1_NS2: { code: 'BFP_2_THICKENER_FREQ_SET', name: '2#带机浓缩机频率给定', value: 0, editing: false, read: false },
})

// 全部设备：加药机 + 污泥料仓 + 转子泵 + 压滤机
const defaultDeviceData = {
  // 加药机（PLC SR30 192.168.0.14）
  D1_JSDCF: { name: '进水电磁阀', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JLMOTOR: { name: '进料电机', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JB1: { name: '1#搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JB2: { name: '2#搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JB3: { name: '3#搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_LCZD: { name: '料仓振动', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JRQ: { name: '加热器', type: 'motor', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JYB1: { name: '1#加药泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  D1_JYB2: { name: '2#加药泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false, plc: 'dosing' },
  // 污泥料仓设备（后端 SludgeSiloPlcService）
  SLUDGE_GATE: { name: '插板阀', type: 'valve', open: false, close: true, fault: false, run: false, readOnly: true, plc: 'silo' },
  SLUDGE_VIBRATOR: { name: '震动器', type: 'motor', run: false, fault: false, readOnly: true, plc: 'silo' },
  // 转子泵设备（后端 RotorPumpPlcService）
  ROTOR_PUMP: { name: '转子泵', type: 'motor', run: false, fault: false, readOnly: true, plc: 'rotor' },
  CONVEYOR_1: { name: '输送机1', type: 'motor', run: false, fault: false, remote: false, readOnly: true, plc: 'rotor' },
  CONVEYOR_2: { name: '输送机2', type: 'motor', run: false, fault: false, remote: false, readOnly: true, plc: 'rotor' },
  // 压滤机 - 1#带机（PLC SR40 192.168.0.15）— 只读
  D1_ZJ1: { name: '1#带机主电机', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_NS1: { name: '1#带机浓缩电机', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_CC1: { name: '1#带机反冲泵', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_KY1: { name: '1#带机空压机', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_DF1: { name: '1#带机电动阀', type: 'valve', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_JN1: { name: '1#带机进泥泵', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_XN1: { name: '1#带机絮凝搅拌', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_ZJ2: { name: '2#带机主电机', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_NS2: { name: '2#带机浓缩电机', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_CC2: { name: '2#带机反冲泵', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_KY2: { name: '2#带机空压机', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_DF2: { name: '2#带机电动阀', type: 'valve', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_JN2: { name: '2#带机进泥泵', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
  D1_XN2: { name: '2#带机絮凝搅拌', type: 'motor', run: false, fault: false, readOnly: true, plc: 'filter' },
}

// 全部仪表：加药机 + 污泥料仓 + 压滤机
const defaultMonitorData = {
  D1_DOSING_LEVEL: { name: '加药机液位', value: 0, unit: 'm' },
  D1_DOSING_FREQ: { name: '进料电机频率显示', value: 0, unit: 'Hz' },
  D1_DOSING_FLOW: { name: '进水瞬时流量', value: 0, unit: 'm³/h' },
  SLUDGE_LEVEL: { name: '仓料位', value: 0, unit: 'mm' },
  D1_FREQ_1_MAIN: { name: '1#带机主机频率显示', value: 0, unit: 'Hz' },
  D1_FREQ_1_THICK: { name: '1#带机浓缩机频率显示', value: 0, unit: 'Hz' },
  D1_FREQ_2_MAIN: { name: '2#带机主机频率显示', value: 0, unit: 'Hz' },
  D1_FREQ_2_THICK: { name: '2#带机浓缩机频率显示', value: 0, unit: 'Hz' },
}

const storageKeys = {
  device: 'D1_DEVICE_V5', monitor: 'D1_MONITOR_V5',
}

const deviceData = ref({ ...defaultDeviceData, ...safeRead(storageKeys.device, {}) })
const monitorData = ref({ ...defaultMonitorData, ...safeRead(storageKeys.monitor, {}) })

const deviceEntries = computed(() => Object.entries(deviceData.value).map(([key, data]) => ({ key, data })))
const monitorEntries = computed(() => Object.entries(monitorData.value).map(([key, data]) => ({ key, data })))
const formatMonitorValue = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(3) : value
}
const formatFrequencyValue = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(3) : value
}
const isDeviceRunning = (d) => { if (!d) return false; if (d.type === 'valve' && !d.readOnly) return d.open; return d.run }
const deviceStateText = (d) => { if (!d) return '离线'; if (d.fault) return '故障'; if (d.type === 'valve' && !d.readOnly) return d.open ? '已开启' : '已关闭'; return d.run ? '运行' : '待机' }
const statusDotClass = (d) => { if (!d) return 'offline'; if (d.fault) return 'fault'; if (d.type === 'valve' && !d.readOnly) return d.open ? 'running' : 'standby'; return d.run ? 'running' : 'standby' }
const canControl = (d) => {
  if (role !== 'admin') return false
  // 加药机不需要remote为1即可控制
  if (d?.plc !== 'dosing' && !d?.remote) return false
  return !!d?.manualMode
}

const plcConnected = ref(false)
const plcIpDosing = ref('192.168.0.14')
const plcIpSilo = ref('192.168.0.28')
const plcIpFilter = ref('192.168.0.15')
const deviceReadStatus = ref(Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])))
let pollTimer = null
const plcDeviceKeys = Object.keys(defaultDeviceData).reduce((groups, code) => {
  const plcKey = defaultDeviceData[code].plc
  if (!groups[plcKey]) groups[plcKey] = []
  groups[plcKey].push(code)
  return groups
}, {})
const plcConnectionStatus = { dosing: false, silo: false, rotor: false, filter: false }
const plcRequests = { dosing: null, silo: null, rotor: null, filter: null }

function shouldShowUnread(code) {
  return !deviceReadStatus.value[code]
}

// 获取后端设备编码
const getBackendCode = (frontendCode) => backendCodeMap[frontendCode] || frontendCode

// 获取 API 基础路径（百科加药）
const getApiBase = () => '/plc/baike-dosing'

const markPlcDevicesUnread = (plcKey) => {
  ;(plcDeviceKeys[plcKey] || []).forEach(code => { deviceReadStatus.value[code] = false })
}

const updateCombinedPlcStatus = () => {
  plcConnected.value = Object.values(plcConnectionStatus).some(Boolean)
}

const runPlcRequest = (plcKey, task) => {
  if (plcRequests[plcKey]) return plcRequests[plcKey]
  const requestPromise = task().finally(() => {
    if (plcRequests[plcKey] === requestPromise) plcRequests[plcKey] = null
  })
  plcRequests[plcKey] = requestPromise
  return requestPromise
}

const performDosingOverview = async () => {
  try {
    const dosingRes = await request.get('/plc/baike-dosing/overview')
    markPlcDevicesUnread('dosing')
    let readCount = 0
    if (dosingRes?.data?.success) {
      if (dosingRes.data.data.plc?.ip) plcIpDosing.value = dosingRes.data.data.plc.ip
      const { devices, parameters } = dosingRes.data.data
      if (devices && devices.length > 0) {
        devices.forEach(d => {
          const frontendCode = frontendCodeMap[d.code]
          if (!frontendCode) return
          const local = deviceData.value[frontendCode]
          if (!local) return
          deviceReadStatus.value[frontendCode] = true
          readCount++
          local.fault = !!d.fault
          local.run = !!d.running
          local.remote = !!d.remoteBit
          local.manualMode = !d.manualModeBit
        })
      }
      if (parameters) {
        parameters.forEach(p => {
          const frontendCode = monitorCodeMap[p.code]
          if (frontendCode) {
            const local = monitorData.value[frontendCode]
            if (local) local.value = p.value ?? 0
          }
          const frequencyControl = Object.values(frequencyControls.value).find(control => control.code === p.code)
          if (!frequencyControl) return
          const value = Number(p.value)
          if (!frequencyControl.editing && Number.isFinite(value)) frequencyControl.value = value
          frequencyControl.read = true
        })
      }
    }
    plcConnectionStatus.dosing = readCount > 0
  } catch (e) {
    markPlcDevicesUnread('dosing')
    plcConnectionStatus.dosing = false
    console.error('[D1] 加药机 PLC 数据读取失败:', e.message)
  } finally {
    updateCombinedPlcStatus()
  }
}

const performSiloOverview = async () => {
  try {
    const siloRes = await request.get('/plc/sludge-silo/overview')
    markPlcDevicesUnread('silo')
    let readCount = 0
    if (siloRes?.data?.success) {
      if (siloRes.data.data.plc?.ip) plcIpSilo.value = siloRes.data.data.plc.ip
      const { devices, parameters } = siloRes.data.data
      if (devices && devices.length > 0) {
        devices.forEach(d => {
          const local = deviceData.value[d.code]; if (!local || local.plc !== 'silo') return
          deviceReadStatus.value[d.code] = true
          readCount++
          local.fault = !!d.fault; local.run = !!d.running
        })
      }
      if (parameters) {
        const gate = deviceData.value['SLUDGE_GATE']
        const vibrator = deviceData.value['SLUDGE_VIBRATOR']
        parameters.forEach(p => {
          if (p.code === 'SLUDGE_GATE_OPEN_FEEDBACK' && gate) gate.open = !!p.value
          else if (p.code === 'SLUDGE_GATE_CLOSE_FEEDBACK' && gate) gate.close = !!p.value
          else if (p.code === 'SLUDGE_GATE_RUNNING_FEEDBACK' && gate) gate.run = !!p.value
          else if (p.code === 'SLUDGE_VIBRATION_RUNNING_FEEDBACK' && vibrator) vibrator.run = !!p.value
          else { const local = monitorData.value[p.code]; if (local) local.value = p.value ?? 0 }
        })
      }
    }
    plcConnectionStatus.silo = readCount > 0
  } catch (e) {
    markPlcDevicesUnread('silo')
    plcConnectionStatus.silo = false
    console.error('[D1] 污泥料仓 PLC 数据读取失败:', e.message)
  } finally {
    updateCombinedPlcStatus()
  }
}

const performRotorOverview = async () => {
  try {
    const rotorRes = await request.get('/plc/rotor-pump/overview')
    markPlcDevicesUnread('rotor')
    let readCount = 0
    if (rotorRes?.data?.success) {
      const { devices } = rotorRes.data.data
      if (devices && devices.length > 0) {
        devices.forEach(d => {
          const local = deviceData.value[d.code]
          if (!local || local.plc !== 'rotor') return
          deviceReadStatus.value[d.code] = true
          readCount++
          local.run = !!d.running
          local.fault = !!d.fault
          if (d.remote !== undefined) local.remote = !!d.remote
        })
      }
    }
    plcConnectionStatus.rotor = readCount > 0
  } catch (e) {
    markPlcDevicesUnread('rotor')
    plcConnectionStatus.rotor = false
    console.error('[D1] 转子泵 PLC 数据读取失败:', e.message)
  } finally {
    updateCombinedPlcStatus()
  }
}

const performFilterOverview = async () => {
  try {
    const filterRes = await request.get('/plc/baike-filter-press/overview')
    markPlcDevicesUnread('filter')
    let readCount = 0
    if (filterRes?.data?.success) {
      if (filterRes.data.data.plc?.ip) plcIpFilter.value = filterRes.data.data.plc.ip
      const { devices, parameters } = filterRes.data.data
      if (devices && devices.length > 0) {
        devices.forEach(d => {
          const frontendCode = filterFrontendCodeMap[d.code]
          if (!frontendCode) return
          const local = deviceData.value[frontendCode]
          if (!local) return
          deviceReadStatus.value[frontendCode] = true
          readCount++
          local.fault = !!d.fault
          local.run = !!d.running
        })
      }
      if (parameters) {
        parameters.forEach(p => {
          const frequencyControl = Object.values(frequencyControls.value).find(control => control.code === p.code)
          if (frequencyControl) {
            const value = Number(p.value)
            if (!frequencyControl.editing && Number.isFinite(value)) frequencyControl.value = value
            frequencyControl.read = true
            return
          }
          const frontendCode = filterMonitorCodeMap[p.code]
          if (!frontendCode) return
          const local = monitorData.value[frontendCode]
          if (!local) return
          local.value = p.value ?? 0
        })
      }
    }
    plcConnectionStatus.filter = readCount > 0
  } catch (e) {
    markPlcDevicesUnread('filter')
    plcConnectionStatus.filter = false
    console.error('[D1] 压滤机 PLC 数据读取失败:', e.message)
  } finally {
    updateCombinedPlcStatus()
  }
}

const fetchDosingOverview = () => runPlcRequest('dosing', performDosingOverview)
const fetchSiloOverview = () => runPlcRequest('silo', performSiloOverview)
const fetchRotorOverview = () => runPlcRequest('rotor', performRotorOverview)
const fetchFilterOverview = () => runPlcRequest('filter', performFilterOverview)
const fetchOverview = () => Promise.allSettled([
  fetchDosingOverview(),
  fetchSiloOverview(),
  fetchRotorOverview(),
  fetchFilterOverview(),
])
const pollOverview = () => { fetchDosingOverview(); fetchSiloOverview(); fetchRotorOverview(); fetchFilterOverview() }
const startPolling = () => { pollOverview(); pollTimer = setInterval(pollOverview, 3000) }

// 压滤机一键启停
const sendOneKey = async (paramCode) => {
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  const label = paramCode.includes('START') ? '一键启动' : '一键停止'
  const name = paramCode.startsWith('BFP_1') ? '1#带机' : '2#带机'
  await ElMessageBox.confirm(`确认${label}「${name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/baike-filter-press/parameters/${paramCode}`, null, { params: { value: 'true' } })
    ElMessage.success(`${label}指令已发送`)
    fetchOverview()
  } catch (e) { ElMessage.error(`${label}失败`) }
}

const writeFrequency = async (key) => {
  const control = frequencyControls.value[key]
  if (!control) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  const value = Number(control.value)
  if (!Number.isFinite(value)) return ElMessage.warning('请输入有效的频率给定值')
  await ElMessageBox.confirm(`确认将「${control.name}」写入 ${value.toFixed(1)} Hz？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    const apiBase = control.apiBase || '/plc/baike-filter-press'
    await request.post(`${apiBase}/parameters/${control.code}`, null, { params: { value: String(value) } })
    control.editing = false
    ElMessage.success(`${control.name}已写入`)
    setTimeout(fetchOverview, 500)
  } catch (e) { ElMessage.error(`${control.name}写入失败`) }
}

const selectDevice = (key) => { if (deviceData.value[key]) selectedDeviceKey.value = key }

const toggleManualMode = async (key, enabled) => {
  const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (it.plc !== 'dosing' && !it.remote) return ElMessage.warning('该设备未授权远程控制')
  const base = getApiBase(key)
  const backendCode = getBackendCode(key)
  try {
    await request.post(`${base}/devices/${backendCode}/manual-mode`, null, { params: { enabled } })
    ElMessage.success('手自动已切换'); fetchOverview()
  } catch (e) { ElMessage.error('手自动切换失败') }
}
const startMotor = async (key) => {
  const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (it.plc !== 'dosing' && !it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认启动「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  const base = getApiBase(key)
  const backendCode = getBackendCode(key)
  try {
    if (it.plc === 'dosing') { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { value: '1' } }) }
    else { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { pulseMs: 300 } }) }
    ElMessage.success('启动指令已发送'); fetchOverview()
  } catch (e) { ElMessage.error('启动失败') }
}
const stopMotor = async (key) => {
  const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (it.plc !== 'dosing' && !it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认停止「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  const base = getApiBase(key)
  const backendCode = getBackendCode(key)
  try {
    if (it.plc === 'dosing') { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { value: '0' } }) }
    else { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { pulseMs: 300 } }) }
    ElMessage.success('停止指令已发送'); fetchOverview()
  } catch (e) { ElMessage.error('停止失败') }
}
const openValve = async (key) => {
  const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (it.plc !== 'dosing' && !it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认开启「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  const base = getApiBase(key)
  const backendCode = getBackendCode(key)
  try {
    if (it.plc === 'dosing') { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { value: '1' } }) }
    else { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { pulseMs: 300 } }) }
    ElMessage.success('开阀指令已发送'); fetchOverview()
  } catch (e) { ElMessage.error('开阀失败') }
}
const closeValve = async (key) => {
  const it = deviceData.value[key]; if (!it) return; if (role !== 'admin') return ElMessage.warning('仅管理员可操作'); if (it.plc !== 'dosing' && !it.remote) return ElMessage.warning('该设备未授权远程控制'); if (!it.manualMode) return ElMessage.warning('请切换到手动模式')
  await ElMessageBox.confirm(`确认关闭「${it.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  const base = getApiBase(key)
  const backendCode = getBackendCode(key)
  try {
    if (it.plc === 'dosing') { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { value: '0' } }) }
    else { await request.post(`${base}/devices/${backendCode}/command`, null, { params: { pulseMs: 300 } }) }
    ElMessage.success('关阀指令已发送'); fetchOverview()
  } catch (e) { ElMessage.error('关阀失败') }
}

watch(deviceData, () => { localStorage.setItem(storageKeys.device, JSON.stringify(deviceData.value)) }, { deep: true })
watch(monitorData, () => { localStorage.setItem(storageKeys.monitor, JSON.stringify(monitorData.value)) }, { deep: true })

// ── 设备颜色映射 ──
const DEVICE_COLOR_MAP = [
  { key: 'D1_JB1', keyword: '1号加药搅拌机' },
  { key: 'D1_JB2', keyword: '2号加药搅拌机' },
  { key: 'D1_JB3', keyword: '3号加药搅拌机' },
  { key: 'D1_JYB1', keyword: '2号加药泵' },
  { key: 'D1_JYB2', keyword: '1号加药泵' },
  { key: 'D1_KY1', keyword: '1号空气压缩机' },
  { key: 'D1_KY2', keyword: '2号空气压缩机' },
  { key: 'D1_CC1', keyword: '1号反洗泵' },
  { key: 'D1_CC2', keyword: '2号反洗泵' },
  { key: 'D1_JN1', keyword: '1号螺杆泵' },
  { key: 'D1_JN2', keyword: '2号螺杆泵' },
  { key: 'D1_ZJ1', keyword: '1号压滤机1号电机' },
  { key: 'D1_NS1', keyword: '1号压滤机2号电机' },
  { key: 'D1_ZJ2', keyword: '2号压滤机1号电机' },
  { key: 'D1_NS2', keyword: '2号压滤机2号电机' },
  { key: 'D1_XN1', keyword: '1号搅拌机' },
  { key: 'D1_XN2', keyword: '2号搅拌机' },
  { key: 'CONVEYOR_1', keyword: '1号输送机电机' },
  { key: 'CONVEYOR_2', keyword: '2号输送机电机' },
  { key: 'ROTOR_PUMP', keyword: '螺杆泵' },
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
  localStorage.setItem('D1_CAMERA_V1', JSON.stringify(config)); ElMessage.success('配置已保存')
}
const handleResize = () => { if (engine) engine.resize() }

const initScene = async () => {
  await nextTick()
  const canvas = canvasRef.value; if (!canvas) return
  engine = new BABYLON.Engine(canvas, true, { preserveDrawingBuffer: true, stencil: true, antialias: true })
  scene = new BABYLON.Scene(engine); scene.clearColor = new BABYLON.Color4(0, 0, 0, 0)
  const savedConfig = safeRead('D1_CAMERA_V1', null)
  const initAlpha = savedConfig?.alpha ?? 3.2, initBeta = savedConfig?.beta ?? 0.9, initRadius = savedConfig?.radius ?? 120
  const initTarget = savedConfig?.target ? new BABYLON.Vector3(savedConfig.target.x, savedConfig.target.y, savedConfig.target.z) : BABYLON.Vector3.Zero()
  camera = new BABYLON.ArcRotateCamera('d1-camera', initAlpha, initBeta, initRadius, initTarget, scene)
  camera.lowerRadiusLimit = 5; camera.upperRadiusLimit = 600; camera.wheelDeltaPercentage = 0.008
  if (savedConfig?.position) camera.setPosition(new BABYLON.Vector3(savedConfig.position.x, savedConfig.position.y, savedConfig.position.z))
  if (cameraLock.value) { camera.detachControl(); canvas.style.pointerEvents = 'none' } else { camera.attachControl(canvas, true); canvas.style.pointerEvents = 'auto' }
  camera.onViewMatrixChangedObservable.add(() => syncSliders())
  const ambientLight = new BABYLON.HemisphericLight('ambient', BABYLON.Vector3.Up(), scene); ambientLight.intensity = 0.3
  const mainLight = new BABYLON.DirectionalLight('main', new BABYLON.Vector3(-1, -1.5, 0.5), scene); mainLight.position = new BABYLON.Vector3(8, 15, 10); mainLight.intensity = 0.8
  const fillLight = new BABYLON.DirectionalLight('fill', new BABYLON.Vector3(0.5, -0.2, -1), scene); fillLight.position = new BABYLON.Vector3(-5, 8, -12); fillLight.intensity = 0.2
  const bottomLight = new BABYLON.DirectionalLight('bottom', new BABYLON.Vector3(0, 1, 0), scene); bottomLight.position = new BABYLON.Vector3(0, -5, 0); bottomLight.intensity = 0.15
  const hdr = BABYLON.CubeTexture.CreateFromPrefilteredData('/environmentSpecular.env', scene); scene.environmentTexture = hdr
  BABYLON.SceneLoader.ImportMesh('', '/', '脱水车间及污泥料仓.max1024.glb', scene, (meshes) => {
    importedMeshes = meshes; meshes.forEach(m => { m.receiveShadows = true; if (m.material) m.material.backFaceCulling = false })
    rootMesh = meshes.find(m => m.name === '__root__')
    if (rootMesh) { rootMesh.scaling = new BABYLON.Vector3(-1, 1, 1); rootMesh.position.x = sliderModelX.value; rootMesh.position.y = sliderModelY.value; rootMesh.position.z = sliderModelZ.value }
    if (camera && meshes.length) { let minV = null, maxV = null; meshes.forEach(m => { if (!m.getBoundingInfo) return; const b = m.getBoundingInfo().boundingBox; minV = minV ? BABYLON.Vector3.Minimize(minV, b.minimumWorld) : b.minimumWorld.clone(); maxV = maxV ? BABYLON.Vector3.Maximize(maxV, b.maximumWorld) : b.maximumWorld.clone() }); if (minV && maxV) { const size = Math.max(maxV.x - minV.x, maxV.y - minV.y, maxV.z - minV.z, 1); camera.lowerRadiusLimit = Math.max(size * 0.2, 5); camera.upperRadiusLimit = Math.max(size * 5, 600) } }
    modelLoaded.value = true; syncDeviceColors(); syncSliders(); console.log('[D1] 模型已加载，网格:', meshes.map(m => m.name).join(', '))
  }, (evt) => { if (evt.lengthComputable) loadingProgress.value = Math.min(95, Math.round((evt.loaded / evt.total) * 100)) }, () => { loadingProgress.value = 100; setTimeout(() => { modelLoaded.value = true }, 100) })
  engine.runRenderLoop(() => { if (scene) scene.render() })
}

onMounted(() => { checkMobile(); if (!isMobileDevice.value) initScene(); window.addEventListener('resize', handleResize); startPolling() })
onUnmounted(() => { window.removeEventListener('resize', handleResize); if (pollTimer) clearInterval(pollTimer); if (scene) { scene.dispose(); scene = null }; if (engine) { engine.dispose(); engine = null }; camera = null })
</script>

<style scoped>
.b1-page { position: relative; height: calc(100vh - var(--nav-height, 113px)); display: flex; flex-direction: column; padding: 12px 14px 14px; background: linear-gradient(180deg, #0F172A 0%, #0F172A 50%, #0F172A 100%); color: #F2F3F5; overflow: hidden; }
.scene-background { position: absolute; inset: 0; z-index: 0; overflow: hidden; }
.stage-canvas { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 0; touch-action: none; outline: none; pointer-events: none; }
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
.card-valve-status { display: flex; gap: 12px; padding: 6px 12px 0; }
.valve-pos { font-size: 0.6875rem; font-weight: 600; color: #4a5a6a; padding: 2px 8px; border: 1px solid rgba(74,90,106,0.3); border-radius: 3px; transition: all 0.3s ease; }
.valve-pos.on { color: #7dffd1; border-color: rgba(48,230,173,0.4); background: rgba(48,230,173,0.08); text-shadow: 0 0 6px rgba(48,230,173,0.4); }
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
.read-only-tag { font-size: 0.6875rem; font-weight: 600; color: #84b6d1; padding: 2px 8px; border: 1px solid rgba(132,182,209,0.3); border-radius: 3px; letter-spacing: 0.04em; }
.frequency-adjustable-tag { font-size: 0.6875rem; font-weight: 600; color: #7dffd1; padding: 2px 8px; border: 1px solid rgba(48,230,173,0.35); border-radius: 3px; white-space: nowrap; }
.frequency-feedback-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 8px 12px; border-top: 1px solid rgba(0,128,255,0.1); }
.frequency-feedback-label { min-width: 0; font-size: 0.75rem; font-weight: 600; color: #84b6d1; }
.frequency-feedback-value { flex-shrink: 0; font-size: 0.75rem; font-family: var(--font-data), monospace; color: #7dffd1; }
.frequency-set-row { display: flex; flex-direction: column; gap: 6px; padding: 8px 12px 10px; border-top: 1px solid rgba(0,128,255,0.1); }
.frequency-set-label { min-width: 0; font-size: 0.75rem; font-weight: 600; line-height: 1.35; color: #8ceeff; }
.frequency-set-editor { display: flex; align-items: center; justify-content: flex-end; gap: 6px; }
.frequency-set-input { width: 74px; min-width: 0; box-sizing: border-box; padding: 4px 6px; border: 1px solid rgba(56,217,255,0.3); border-radius: 3px; outline: none; background: rgba(8,28,44,0.72); color: #e8fbff; font-size: 0.75rem; font-family: var(--font-data), monospace; }
.frequency-set-input:focus { border-color: rgba(56,217,255,0.7); box-shadow: 0 0 0 1px rgba(56,217,255,0.18); }
.frequency-set-unit { font-size: 0.6875rem; color: #84b6d1; }
.frequency-set-button { border-color: rgba(48,230,173,0.35); color: #7dffd1; }
.device-list::-webkit-scrollbar { width: 5px; }
.device-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.device-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
.onekey-row { display: flex; align-items: center; gap: 6px; padding: 6px 12px 8px; border-top: 1px solid rgba(0,128,255,0.1); }
.onekey-label { font-size: 0.75rem; font-weight: 700; color: #8ceeff; white-space: nowrap; }
.monitor-list::-webkit-scrollbar { width: 5px; }
.monitor-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.monitor-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
@keyframes gridDrift { 0% { transform: translate3d(0,0,0); } 100% { transform: translate3d(18px,10px,0); } }
@keyframes horizonPulse { 0%, 100% { opacity: 0.35; transform: scaleX(0.94); } 50% { opacity: 0.82; transform: scaleX(1); } }
@keyframes hudNodePulse { 0%, 100% { opacity: 0.28; transform: scale(0.96); } 50% { opacity: 0.62; transform: scale(1.02); } }
@keyframes radarPing { 0% { transform: translate(-50%,-50%) scale(0.68); opacity: 0.56; } 80% { opacity: 0.12; } 100% { transform: translate(-50%,-50%) scale(1.42); opacity: 0; } }
@keyframes sweep { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }
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

@media (max-width: 1400px) { .command-deck { grid-template-columns: 260px minmax(0, 1fr) 260px; } }
@media (max-width: 1024px) { .b1-page { height: auto; min-height: calc(100vh - var(--nav-height, 113px)); overflow: auto; } .command-deck { flex: none; display: flex; flex-direction: column; overflow: visible; } .stage-panel { display: flex; flex-direction: column; } .hero-stage { min-height: 400px; } }
</style>
