<template>
  <div class="p3-page">
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
      <main class="stage-panel">
        <section class="hero-stage">
          <div class="stage-frame">
            <div class="stage-hud-grid"></div>
            <div class="stage-badges"><div class="stage-badge"><span>工艺段</span><strong>巴氏计量槽</strong></div></div>
            <div class="frame-corner top-left"></div>
            <div class="frame-corner top-right"></div>
            <div class="frame-corner bottom-left"></div>
            <div class="frame-corner bottom-right"></div>
            <canvas ref="canvasRef" class="stage-canvas"></canvas>
            <ModelLoader :model-loaded="modelLoaded" :loading-progress="loadingProgress" />
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as BABYLON from 'babylonjs'
import 'babylonjs-loaders'
import ModelLoader from '@/components/ModelLoader.vue'

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
const savedCameraConfig = safeRead('P3_CAMERA_V1', null)
const sliderModelX = ref(savedCameraConfig?.model?.x ?? 0)
const sliderModelY = ref(savedCameraConfig?.model?.y ?? 0)
const sliderModelZ = ref(savedCameraConfig?.model?.z ?? 0)

const checkMobile = () => {
  const ua = navigator.userAgent
  isMobileDevice.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua) || window.innerWidth < 1024
}

let engine = null, scene = null, camera = null, importedMeshes = [], rootMesh = null

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
  localStorage.setItem('P3_CAMERA_V1', JSON.stringify(config)); ElMessage.success('配置已保存')
}
const handleResize = () => { if (engine) engine.resize() }

const initScene = async () => {
  await nextTick()
  const canvas = canvasRef.value; if (!canvas) return
  engine = new BABYLON.Engine(canvas, true, { preserveDrawingBuffer: true, stencil: true, antialias: true })
  scene = new BABYLON.Scene(engine); scene.clearColor = new BABYLON.Color4(0, 0, 0, 0)
  const savedConfig = safeRead('P3_CAMERA_V1', null)
  const initAlpha = savedConfig?.alpha ?? 3.2, initBeta = savedConfig?.beta ?? 0.9, initRadius = savedConfig?.radius ?? 120
  const initTarget = savedConfig?.target ? new BABYLON.Vector3(savedConfig.target.x, savedConfig.target.y, savedConfig.target.z) : BABYLON.Vector3.Zero()
  camera = new BABYLON.ArcRotateCamera('p3-camera', initAlpha, initBeta, initRadius, initTarget, scene)
  camera.lowerRadiusLimit = 5; camera.upperRadiusLimit = 600; camera.wheelDeltaPercentage = 0.008
  if (savedConfig?.position) camera.setPosition(new BABYLON.Vector3(savedConfig.position.x, savedConfig.position.y, savedConfig.position.z))
  if (cameraLock.value) { camera.detachControl(); canvas.style.pointerEvents = 'none' } else { camera.attachControl(canvas, true); canvas.style.pointerEvents = 'auto' }
  camera.onViewMatrixChangedObservable.add(() => syncSliders())
  const ambientLight = new BABYLON.HemisphericLight('ambient', BABYLON.Vector3.Up(), scene); ambientLight.intensity = 0.3
  const mainLight = new BABYLON.DirectionalLight('main', new BABYLON.Vector3(-1, -1.5, 0.5), scene); mainLight.position = new BABYLON.Vector3(8, 15, 10); mainLight.intensity = 0.8
  const fillLight = new BABYLON.DirectionalLight('fill', new BABYLON.Vector3(0.5, -0.2, -1), scene); fillLight.position = new BABYLON.Vector3(-5, 8, -12); fillLight.intensity = 0.2
  const bottomLight = new BABYLON.DirectionalLight('bottom', new BABYLON.Vector3(0, 1, 0), scene); bottomLight.position = new BABYLON.Vector3(0, -5, 0); bottomLight.intensity = 0.15
  const hdr = BABYLON.CubeTexture.CreateFromPrefilteredData('/environmentSpecular.env', scene); scene.environmentTexture = hdr
  BABYLON.SceneLoader.ImportMesh('', '/', '巴氏计量槽2.glb', scene, (meshes) => {
    importedMeshes = meshes; meshes.forEach(m => { m.receiveShadows = true; if (m.material) m.material.backFaceCulling = false })
    rootMesh = meshes.find(m => m.name === '__root__')
    if (rootMesh) { rootMesh.scaling = new BABYLON.Vector3(-1, 1, 1); rootMesh.position.x = sliderModelX.value; rootMesh.position.y = sliderModelY.value; rootMesh.position.z = sliderModelZ.value }
    if (camera && meshes.length) { let minV = null, maxV = null; meshes.forEach(m => { if (!m.getBoundingInfo) return; const b = m.getBoundingInfo().boundingBox; minV = minV ? BABYLON.Vector3.Minimize(minV, b.minimumWorld) : b.minimumWorld.clone(); maxV = maxV ? BABYLON.Vector3.Maximize(maxV, b.maximumWorld) : b.maximumWorld.clone() }); if (minV && maxV) { const size = Math.max(maxV.x - minV.x, maxV.y - minV.y, maxV.z - minV.z, 1); camera.lowerRadiusLimit = Math.max(size * 0.2, 5); camera.upperRadiusLimit = Math.max(size * 5, 600) } }
    modelLoaded.value = true; syncSliders(); console.log('[P3] 模型已加载，网格:', meshes.map(m => m.name).join(', '))
  }, (evt) => { if (evt.lengthComputable) loadingProgress.value = Math.min(95, Math.round((evt.loaded / evt.total) * 100)) }, () => { loadingProgress.value = 100; setTimeout(() => { modelLoaded.value = true }, 100) })
  engine.runRenderLoop(() => { if (scene) scene.render() })
}

onMounted(() => { checkMobile(); if (!isMobileDevice.value) initScene(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); if (scene) { scene.dispose(); scene = null }; if (engine) { engine.dispose(); engine = null }; camera = null })
</script>

<style scoped>
.p3-page { position: relative; height: calc(100vh - var(--nav-height, 113px)); display: flex; flex-direction: column; padding: 12px 14px 14px; background: linear-gradient(180deg, #0F172A 0%, #0F172A 50%, #0F172A 100%); color: #F2F3F5; overflow: hidden; }
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
.control-actions { display: flex; gap: 8px; margin-top: 14px; }
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
.command-deck { position: relative; flex: 1; display: grid; grid-template-columns: minmax(0, 1fr); gap: 14px; min-height: 0; overflow: hidden; z-index: 2; }
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
@keyframes gridDrift { 0% { transform: translate3d(0,0,0); } 100% { transform: translate3d(18px,10px,0); } }
@keyframes horizonPulse { 0%, 100% { opacity: 0.35; transform: scaleX(0.94); } 50% { opacity: 0.82; transform: scaleX(1); } }
@keyframes hudNodePulse { 0%, 100% { opacity: 0.28; transform: scale(0.96); } 50% { opacity: 0.62; transform: scale(1.02); } }
@keyframes radarPing { 0% { transform: translate(-50%,-50%) scale(0.68); opacity: 0.56; } 80% { opacity: 0.12; } 100% { transform: translate(-50%,-50%) scale(1.42); opacity: 0; } }
@keyframes sweep { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }
@media (max-width: 1024px) { .p3-page { height: auto; min-height: calc(100vh - var(--nav-height, 113px)); overflow: auto; } .command-deck { flex: none; display: flex; flex-direction: column; overflow: visible; } .stage-panel { display: flex; flex-direction: column; } .hero-stage { min-height: 400px; } }
</style>
