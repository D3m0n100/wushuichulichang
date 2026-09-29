<template>
  <div class="rp-page">
    <div class="scene-background">
      <canvas ref="bgCanvasRef" class="bg-canvas" v-show="!isMobileDevice"></canvas>
      <div class="scene-bg-shade"></div>
    </div>
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

    <div class="command-deck">
      <!-- 中间：3D模型区域 -->
      <main class="stage-panel">
        <section class="hero-stage">
          <div class="stage-frame">
            <div class="stage-hud-grid"></div>
                        <div class="stage-badges">
              <div class="stage-badge"><span>工艺段</span><strong>转子泵</strong></div>
            </div>
            <div class="frame-corner top-left"></div>
            <div class="frame-corner top-right"></div>
            <div class="frame-corner bottom-left"></div>
            <div class="frame-corner bottom-right"></div>
          </div>
        </section>
      </main>

      <!-- 右侧：设备控制卡片 -->
      <aside class="side-panel right-panel">
        <section class="panel-block device-control-block">
          <div class="panel-title">设备状态</div>
          <div class="device-list">
            <div v-for="item in deviceEntries" :key="item.key" class="device-card" :class="{ fault: item.data.fault, running: item.data.running }">
              <div class="card-row-top">
                <span class="card-device-name">{{ item.data.name }}</span>
                <span v-if="!deviceReadStatus[item.key]" class="card-unread" title="未读取到数据">✕</span>
                <span v-if="item.data.remote !== undefined" class="card-remote-tag" :class="{ on: item.data.remote }">{{ item.data.remote ? '远程' : '就地' }}</span>
              </div>
              <div class="card-row-bottom">
                <span class="card-status-dot" :class="{ running: item.data.running && !item.data.fault, fault: item.data.fault }"></span>
                <span class="card-status-text" :class="{ running: item.data.running && !item.data.fault, fault: item.data.fault, stopped: !item.data.running && !item.data.fault }">{{ item.data.fault ? '故障' : item.data.running ? '运行' : '停止' }}</span>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '@/api/request'

const isMobileDevice = ref(false)
const bgCanvasRef = ref(null)

const checkMobile = () => {
  const ua = navigator.userAgent
  isMobileDevice.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua) || window.innerWidth < 1024
}

// 转子泵点位表设备（后端 RotorPumpPlcService 注册的设备编码）
const defaultDeviceData = {
  ROTOR_PUMP: { name: '转子泵', type: 'motor', running: false, fault: false, readOnly: true },
  CONVEYOR_1: { name: '输送机1', type: 'motor', running: false, fault: false, remote: false, readOnly: true },
  CONVEYOR_2: { name: '输送机2', type: 'motor', running: false, fault: false, remote: false, readOnly: true },
}

const storageKeys = { device: 'RP_DEVICE_V2' }
const safeRead = (key, fallback) => { try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback } catch { return fallback } }

const deviceData = ref({ ...defaultDeviceData, ...safeRead(storageKeys.device, {}) })
const deviceEntries = computed(() => Object.entries(deviceData.value).map(([key, data]) => ({ key, data })))

const plcConnected = ref(false)
const plcIp = ref('192.168.0.30')
const deviceReadStatus = ref(Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])))
let pollTimer = null
let consecutiveFailures = 0
const fetchOverview = async () => {
  try {
    const res = await request.get('/plc/rotor-pump/overview')
    if (!res.data?.success) return
    const { devices, plc } = res.data.data
    if (plc?.ip) plcIp.value = plc.ip
    const readCodes = {}
    if (devices && devices.length > 0) {
      consecutiveFailures = 0
      plcConnected.value = true
      devices.forEach(d => {
        readCodes[d.code] = true
        const local = deviceData.value[d.code]
        if (!local) return
        local.running = !!d.running
        local.fault = !!d.fault
        if (d.remote !== undefined) local.remote = !!d.remote
      })
    }
    deviceReadStatus.value = readCodes
  } catch (e) { consecutiveFailures++; if (consecutiveFailures >= 3) { plcConnected.value = false; deviceReadStatus.value = Object.fromEntries(Object.keys(defaultDeviceData).map(k => [k, false])) }; console.error(`转子泵 overview 获取失败 (连续${consecutiveFailures}次):`, e.message) }
}

watch(deviceData, () => { localStorage.setItem(storageKeys.device, JSON.stringify(deviceData.value)) }, { deep: true })
onMounted(() => { checkMobile(); fetchOverview(); pollTimer = setInterval(fetchOverview, 3000) })
</script>

<style scoped>
.rp-page { position: relative; height: calc(100vh - var(--nav-height, 113px)); display: flex; flex-direction: column; padding: 12px 14px 14px; background: linear-gradient(180deg, #0F172A 0%, #0F172A 50%, #0F172A 100%); color: #F2F3F5; overflow: hidden; }
.scene-background { position: absolute; inset: 0; z-index: 0; overflow: hidden; }
.bg-canvas { width: 100%; height: 100%; display: block; position: relative; z-index: 0; touch-action: none; outline: none; }
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
.command-deck { position: relative; flex: 1; display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 14px; min-height: 0; overflow: hidden; z-index: 2; }
.side-panel { position: relative; display: flex; flex-direction: column; gap: 14px; overflow: hidden; z-index: 2; isolation: isolate; }
.right-panel::before { content: ''; position: absolute; top: 14px; bottom: 14px; left: 2px; width: 1px; pointer-events: none; background: linear-gradient(180deg, transparent, rgba(85,220,255,0.44), transparent); box-shadow: 0 0 16px rgba(85,220,255,0.18); }
.right-panel { overflow: hidden; }
.stage-panel { position: relative; min-width: 0; z-index: 2; }
.hero-stage { height: 100%; }
.stage-frame { height: 100%; position: relative; border: 1px solid rgba(0,128,255,0.18); background: rgba(15,23,42,0.88); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.04); overflow: hidden; clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px)); }
.stage-hud-grid { position: absolute; inset: 0; pointer-events: none; background-image: linear-gradient(rgba(92,229,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(92,229,255,0.04) 1px, transparent 1px); background-size: 72px 72px; mask-image: radial-gradient(circle at 50% 52%, rgba(255,255,255,0.5), transparent 88%); animation: gridDrift 28s linear infinite; z-index: 1; }
.stage-badges { position: absolute; left: 18px; top: 18px; z-index: 4; }
.stage-badge { min-width: 156px; padding: 10px 12px; border: 1px solid rgba(0,128,255,0.28); background: linear-gradient(90deg, rgba(0,128,255,0.08), transparent 42%), linear-gradient(180deg, rgba(30,41,59,0.12), rgba(30,41,59,0.05)); backdrop-filter: saturate(135%); clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px)); }
.stage-badge span { display: block; font-size: 0.875rem; color: #84b6d1; }
.stage-badge strong { display: block; margin-top: 6px; font-size: 1rem; color: #F2F3F5; }
.frame-corner { position: absolute; width: 34px; height: 34px; border: 2px solid rgba(79,218,255,0.68); z-index: 5; opacity: 0.58; box-shadow: 0 0 10px rgba(79,218,255,0.12); }
.frame-corner.top-left { top: 12px; left: 12px; border-right: none; border-bottom: none; }
.frame-corner.top-right { top: 12px; right: 12px; border-left: none; border-bottom: none; }
.frame-corner.bottom-left { bottom: 12px; left: 12px; border-right: none; border-top: none; }
.frame-corner.bottom-right { right: 12px; bottom: 12px; border-left: none; border-top: none; }
.panel-block { position: relative; border: 1px solid rgba(0,128,255,0.18); background: rgba(15,23,42,0.88); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.04); backdrop-filter: saturate(118%); overflow: hidden; clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px)); }
.panel-block::before { content: ''; position: absolute; inset: 0; pointer-events: none; background: linear-gradient(90deg, transparent, rgba(170,240,255,0.10), transparent); transform: translateX(-100%); animation: sweep 14s linear infinite; }
.panel-block::after { content: ''; position: absolute; inset: 1px; pointer-events: none; border: 1px solid rgba(168,240,255,0.14); clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px)); }
.panel-block::before { animation-duration: 10s; }
.panel-title { position: relative; z-index: 2; display: inline-flex; align-items: center; gap: 10px; margin: 14px 16px 0; padding-right: 8px; font-size: 1.125rem; font-weight: 700; color: #d8f7ff; letter-spacing: 0.04em; text-shadow: 0 0 14px rgba(74,210,255,0.18), 0 0 22px rgba(74,210,255,0.05); }
.panel-title::before { content: ''; width: 26px; height: 3px; background: linear-gradient(90deg, #2ce1ff, #86f8c8 55%, rgba(134,248,200,0.2)); box-shadow: 0 0 14px rgba(44,225,255,0.46); }
.device-control-block { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.device-list { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 8px; padding: 8px 12px 12px; overflow-y: auto; }
.device-card { position: relative; border: 1px solid rgba(0,128,255,0.22); background: rgba(15,23,42,0.6); padding: 10px 12px; display: flex; flex-direction: column; gap: 8px; clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px)); transition: all 0.25s ease; }
.device-card:hover { border-color: rgba(0,200,255,0.38); background: rgba(15,23,42,0.8); }
.device-card.fault { border-color: rgba(255,80,80,0.45); }
.device-card.running {
  border-color: rgba(48,230,173,0.68);
  background: rgba(48,230,173,0.06);
  box-shadow: inset 0 0 0 1px rgba(48,230,173,0.32), 0 0 14px rgba(48,230,173,0.26), 0 0 26px rgba(48,230,173,0.12);
}
.card-row-top { display: flex; align-items: center; justify-content: space-between; }
.card-device-name { font-size: 0.9375rem; font-weight: 700; color: #e8fbff; text-shadow: 0 0 10px rgba(61,184,255,0.12); }
.card-unread { font-size: 0.875rem; color: #ff5050; font-weight: 700; flex-shrink: 0; opacity: 0.8; line-height: 1; }
.card-remote-tag { font-size: 0.6875rem; font-weight: 600; padding: 2px 8px; border: 1px solid rgba(74,90,106,0.3); border-radius: 3px; color: #5a6a7a; transition: all 0.3s ease; }
.card-remote-tag.on { color: #7dffd1; border-color: rgba(48,230,173,0.4); background: rgba(48,230,173,0.1); }
.card-row-bottom { display: flex; align-items: center; gap: 8px; }
.card-status-dot { width: 10px; height: 10px; border-radius: 50%; background: #3a4a5a; transition: all 0.3s ease; }
.card-status-dot.running { background: #30e6ad; box-shadow: 0 0 10px rgba(48,230,173,0.6); }
.card-status-dot.fault { background: #ff5050; box-shadow: 0 0 10px rgba(255,80,80,0.6); }
.card-status-text { font-size: 0.875rem; font-weight: 700; letter-spacing: 0.04em; }
.card-status-text.running { color: #7dffd1; }
.card-status-text.fault { color: #ff8080; text-shadow: 0 0 6px rgba(255,80,80,0.3); }
.card-status-text.stopped { color: #6a7a8a; }
.device-list::-webkit-scrollbar { width: 5px; }
.device-list::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.device-list::-webkit-scrollbar-thumb { background: rgba(80,200,255,0.25); border-radius: 999px; }
@keyframes gridDrift { 0% { transform: translate3d(0,0,0); } 100% { transform: translate3d(18px,10px,0); } }
@keyframes horizonPulse { 0%, 100% { opacity: 0.35; transform: scaleX(0.94); } 50% { opacity: 0.82; transform: scaleX(1); } }
@keyframes hudNodePulse { 0%, 100% { opacity: 0.28; transform: scale(0.96); } 50% { opacity: 0.62; transform: scale(1.02); } }
@keyframes radarPing { 0% { transform: translate(-50%,-50%) scale(0.68); opacity: 0.56; } 80% { opacity: 0.12; } 100% { transform: translate(-50%,-50%) scale(1.42); opacity: 0; } }
@keyframes sweep { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }
@media (max-width: 1024px) { .rp-page { height: auto; min-height: calc(100vh - var(--nav-height, 113px)); overflow: auto; } .command-deck { flex: none; display: flex; flex-direction: column; overflow: visible; } .stage-panel { display: flex; flex-direction: column; } .hero-stage { min-height: 300px; } }
</style>
