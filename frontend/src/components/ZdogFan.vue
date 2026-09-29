<template>
  <div ref="canvasContainer" class="zdog-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import Zdog from 'zdog'

const canvasContainer = ref(null)
let illo = null
let fanGroup = null
let animationId = null
let isRotating = false

const props = defineProps({
  kgValue: {
    type: Number,
    default: 0
  }
})

// 初始化 Zdog 场景
function initZdog() {
  if (!canvasContainer.value) return

  // 获取容器尺寸
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight

  // 创建插图（场景）
  illo = new Zdog.Illustration({
    element: canvasContainer.value,
    zoom: 2.5,           // 缩放
    centered: true,      // 居中
    dragRotate: false,   // 禁用拖拽旋转
    resize: 'fullscreen',
  })

  // 添加辅助网格（可选，增加参考）
  new Zdog.Rect({
    addTo: illo,
    width: 80,
    height: 80,
    stroke: 0.5,
    color: '#335588',
    fill: true,
    visible: false,      // 可改为 true 显示参考网格
  })

  // --- 风扇叶片组（整体旋转）---
  fanGroup = new Zdog.Group({
    addTo: illo,
  })

  // 中心圆盘（电机部分）
  const centerDisk = new Zdog.Ellipse({
    addTo: fanGroup,
    diameter: 12,
    stroke: 2,
    color: '#CC8844',
    fill: true,
    backface: '#AA6633',
  })

  // 叶片材质颜色
  const bladeColor = '#BBAA88'
  const bladeBackface = '#998866'

  // 创建三个叶片，每个叶片是一个扁平的盒子（Box）
  const bladeLength = 22
  const bladeWidth = 8
  const bladeDepth = 2
  const offset = 8 // 叶片根部到中心的距离

  for (let i = 0; i < 3; i++) {
    // 每个叶片单独创建一个 Box，并添加到 fanGroup
    const angle = (i / 3) * Math.PI * 2
    const blade = new Zdog.Box({
      addTo: fanGroup,
      width: bladeLength,
      height: bladeWidth,
      depth: bladeDepth,
      color: bladeColor,
      backface: bladeBackface,
      leftFace: bladeColor,
      rightFace: bladeColor,
      topFace: bladeColor,
      bottomFace: bladeColor,
      translate: { x: Math.cos(angle) * offset, y: Math.sin(angle) * offset, z: 0 },
      rotate: { z: angle },               // 使叶片径向指向中心
    })
    // 给叶片添加轻微倾角（模拟真实叶片）
    blade.rotate.x = 0.3
  }

  // 可选的装饰：叶片末端的亮色小圆点
  for (let i = 0; i < 3; i++) {
    const angle = (i / 3) * Math.PI * 2
    const tipDist = offset + bladeLength / 2
    new Zdog.Shape({
      addTo: fanGroup,
      path: [{ x: Math.cos(angle) * tipDist, y: Math.sin(angle) * tipDist, z: 0 }],
      stroke: 1.5,
      color: '#FFCC88',
    })
  }

  // 添加一点环境光效果（Zdog 没有真正的光照，但颜色可以模拟）
  // 可以不做额外处理

  // 首次渲染
  illo.updateRenderGraph()
}

// 动画循环
function animate() {
  if (!illo) return
  if (isRotating && fanGroup) {
    fanGroup.rotate.z += 0.05   // 每帧旋转角度，控制速度
  }
  illo.updateRenderGraph()
  animationId = requestAnimationFrame(animate)
}

// 监听 kgValue 变化，控制旋转
watch(() => props.kgValue, (newVal) => {
  isRotating = (newVal === 1)
})

onMounted(() => {
  initZdog()
  animate()
  // 监听窗口大小变化，重新调整画布尺寸
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (illo) illo.element.remove()
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  if (!illo || !canvasContainer.value) return
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  illo.zoom = Math.min(width / 200, height / 200) * 2.5
  illo.updateRenderGraph()
}
</script>

<style scoped>
.zdog-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
  background: transparent;
  cursor: pointer;
}
</style>