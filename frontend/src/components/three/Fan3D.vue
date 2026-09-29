<template>
  <div ref="containerRef" class="three-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as THREE from 'three'

const containerRef = ref(null)
let scene, camera, renderer, fanGroup, fanBlades
let animationId = null
let isRotating = false
let rotationSpeed = 0.2

const props = defineProps({
  kgValue: {
    type: Number,
    default: 0
  }
})

function init() {
  if (!containerRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a1a2f)
  scene.fog = new THREE.FogExp2(0x0a1a2f, 0.008)

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(4, 2.5, 6)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  containerRef.value.appendChild(renderer.domElement)

  // 灯光
  const ambientLight = new THREE.AmbientLight(0x404060)
  scene.add(ambientLight)
  const mainLight = new THREE.DirectionalLight(0xffffff, 1)
  mainLight.position.set(3, 5, 2)
  scene.add(mainLight)
  const fillLight = new THREE.PointLight(0x4466cc, 0.4)
  fillLight.position.set(-1, 1, 2)
  scene.add(fillLight)
  const rimLight = new THREE.PointLight(0xffaa66, 0.3)
  rimLight.position.set(1, 1, -2)
  scene.add(rimLight)

  // 辅助网格地面 (半透明，仅作参考)
  const gridHelper = new THREE.GridHelper(5, 20, 0x88aaff, 0x335588)
  gridHelper.position.y = -1.0
  gridHelper.material.transparent = true
  gridHelper.material.opacity = 0.2
  scene.add(gridHelper)

  // --- 创建水管模型 (水平放置，一端开口) ---
  const pipeGroup = new THREE.Group()

  // 水管主体 (圆柱，沿 X 轴方向)
  const pipeLength = 2.5
  const pipeRadius = 0.45
  const pipeGeo = new THREE.CylinderGeometry(pipeRadius, pipeRadius, pipeLength, 32)
  const pipeMat = new THREE.MeshStandardMaterial({ color: 0x88aacc, metalness: 0.7, roughness: 0.4 })
  const pipeBody = new THREE.Mesh(pipeGeo, pipeMat)
  pipeBody.rotation.z = Math.PI / 2
  pipeBody.position.x = 0
  pipeGroup.add(pipeBody)

  // 水管法兰 (末端圆环，表示接口)
  const flangeGeo = new THREE.TorusGeometry(pipeRadius + 0.08, 0.08, 16, 64)
  const flangeMat = new THREE.MeshStandardMaterial({ color: 0xccaa77, metalness: 0.6 })
  const flange = new THREE.Mesh(flangeGeo, flangeMat)
  flange.position.x = pipeLength / 2 + 0.05
  flange.rotation.y = Math.PI / 2
  pipeGroup.add(flange)

  // 水管另一端加个盖子
  const capGeo = new THREE.CylinderGeometry(pipeRadius + 0.05, pipeRadius + 0.05, 0.1, 16)
  const capMat = new THREE.MeshStandardMaterial({ color: 0xaa8866, metalness: 0.5 })
  const cap = new THREE.Mesh(capGeo, capMat)
  cap.position.x = -pipeLength / 2 - 0.05
  cap.rotation.z = Math.PI / 2
  pipeGroup.add(cap)

  scene.add(pipeGroup)

  // --- 风扇组 (安装在法兰处，即 X 正方向末端) ---
  fanGroup = new THREE.Group()
  fanGroup.position.x = pipeLength / 2 + 0.15  // 紧贴法兰
  fanGroup.position.y = 0
  fanGroup.position.z = 0

  // 中心轴 (小圆柱)
  const hubGeo = new THREE.CylinderGeometry(0.2, 0.2, 0.15, 16)
  const hubMat = new THREE.MeshStandardMaterial({ color: 0xdd8844, metalness: 0.8, roughness: 0.3 })
  const hub = new THREE.Mesh(hubGeo, hubMat)
  hub.rotation.x = Math.PI / 2
  fanGroup.add(hub)

  // 三个叶片
  const bladeMat = new THREE.MeshStandardMaterial({ color: 0xccaa66, metalness: 0.75, roughness: 0.4 })
  const bladeLength = 0.7
  const bladeWidth = 0.25
  const bladeThick = 0.08

  for (let i = 0; i < 3; i++) {
    const bladeGeo = new THREE.BoxGeometry(bladeLength, bladeWidth, bladeThick)
    const blade = new THREE.Mesh(bladeGeo, bladeMat)
    // 叶片根部偏移中心
    blade.position.set(0.35, 0, 0)
    blade.rotation.z = (i * Math.PI * 2) / 3
    // 叶片略倾斜
    blade.rotation.x = 0.2
    fanGroup.add(blade)
  }

  scene.add(fanGroup)

  // 添加一个微弱的点光源在风扇中心
  const glowLight = new THREE.PointLight(0x2266aa, 0.6, 4)
  glowLight.position.set(fanGroup.position.x, fanGroup.position.y, fanGroup.position.z)
  scene.add(glowLight)

  // 动画循环
  function animate() {
    animationId = requestAnimationFrame(animate)
    if (isRotating) {
      fanGroup.rotation.x += rotationSpeed
    }
    // 动态光源强度跟随旋转（可选）
    const time = Date.now() * 0.005
    glowLight.intensity = 0.5 + Math.sin(time) * 0.2
    renderer.render(scene, camera)
  }
  animate()

  // 窗口适配
  window.addEventListener('resize', onWindowResize)
}

function onWindowResize() {
  if (!containerRef.value) return
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

watch(() => props.kgValue, (newVal) => {
  isRotating = (newVal === 1)
  rotationSpeed = isRotating ? 0.2 : 0
})

onMounted(() => {
  init()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (renderer) renderer.dispose()
  window.removeEventListener('resize', onWindowResize)
})
</script>

<style scoped>
.three-container {
  width: 100%;
  height: 100%;
  min-height: 350px;
  outline: none;
}
</style>