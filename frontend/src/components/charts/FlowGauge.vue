<template>
  <div class="flow-gauge-card" role="img" :aria-label="`${value} ${unit}${statusText ? `，${statusText}` : ''}`">
    <div class="gauge-wrap">
      <canvas ref="gaugeCanvas" class="gauge-canvas"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

// ============ 可配置参数 ============
const START_ANGLE = 0.75
const END_ANGLE = 2.25
const RADIUS = 90
const TRACK_WIDTH = 12
const BAR_WIDTH = 12
const TOTAL_TICKS = 60
const MAJOR_TICK_EVERY = 10
const TICK_LENGTH_MAJOR = 18
const TICK_LENGTH_MINOR = 12

const COLOR_LOW = ['#00e8ff', '#00e8ff']
const COLOR_MID = ['#00e8ff', '#ffaa00']
const COLOR_HIGH = ['#ffaa00', '#ff4444']

const FONT_VALUE = 'bold 32px Microsoft YaHei'
const FONT_LABEL = '500 13px Rajdhani'
const FONT_STATUS = '600 12px Microsoft YaHei'
const LABEL_COLOR = 'rgba(0, 232, 255, 0.7)'

const THRESHOLDS = [
  { max: 40, text: '低流量', color: '#00e8ff' },
  { max: 70, text: '正常', color: '#00ff88' },
  { max: 90, text: '高流量', color: '#ffaa00' },
  { max: 100, text: '过载', color: '#ff4444' },
]

const ANIMATION_DURATION = 1000
const GLOW_BLUR = 15

// ============ 组件逻辑 ============
const props = defineProps({
  value: { type: Number, default: 0 },
  unit: { type: String, default: 'm³/h' },
  max: { type: Number, default: 500 },
  statusText: { type: String, default: '' }
})

const gaugeCanvas = ref(null)
const displayValue = ref(0)
const displayPercent = ref(0)

const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3)

const animateValue = (from, to) => {
  const startTime = performance.now()
  const diff = to - from

  const step = (now) => {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / ANIMATION_DURATION, 1)
    displayValue.value = Math.round(from + diff * easeOutCubic(progress))
    displayPercent.value = Math.round((displayValue.value / props.max) * 100)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

const draw = () => {
  const canvas = gaugeCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  const dpr = window.devicePixelRatio || 1
  const rect = canvas.getBoundingClientRect()
  canvas.width = rect.width * dpr
  canvas.height = rect.height * dpr
  ctx.scale(dpr, dpr)

  const centerX = rect.width / 2
  const centerY = rect.height / 2 + 10
  const radius = Math.min(centerX, centerY, RADIUS)

  const startAngle = Math.PI * START_ANGLE
  const endAngle = Math.PI * END_ANGLE

  ctx.clearRect(0, 0, rect.width, rect.height)

  // 1. 外圈发光环
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius + 4, startAngle, endAngle)
  ctx.strokeStyle = 'rgba(0, 232, 255, 0.1)'
  ctx.lineWidth = 2
  ctx.stroke()

  // 2. 背景轨道
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius, startAngle, endAngle)
  ctx.strokeStyle = 'rgba(0, 232, 255, 0.08)'
  ctx.lineWidth = TRACK_WIDTH
  ctx.lineCap = 'round'
  ctx.stroke()

  // 3. 刻度线
  for (let i = 0; i <= TOTAL_TICKS; i++) {
    const angle = startAngle + (endAngle - startAngle) * (i / TOTAL_TICKS)
    const isMajor = i % MAJOR_TICK_EVERY === 0
    const innerR = radius - (isMajor ? TICK_LENGTH_MAJOR : TICK_LENGTH_MINOR)
    const outerR = radius - 2
    ctx.beginPath()
    ctx.moveTo(centerX + Math.cos(angle) * innerR, centerY + Math.sin(angle) * innerR)
    ctx.lineTo(centerX + Math.cos(angle) * outerR, centerY + Math.sin(angle) * outerR)
    ctx.strokeStyle = isMajor ? 'rgba(0, 232, 255, 0.5)' : 'rgba(0, 232, 255, 0.2)'
    ctx.lineWidth = isMajor ? 2 : 1
    ctx.stroke()
  }

  // 4. 彩色进度弧
  const percent = Math.max(0, Math.min(100, displayPercent.value))
  const valueAngle = startAngle + (endAngle - startAngle) * (percent / 100)

  let colors = COLOR_LOW
  if (percent >= 80) colors = COLOR_HIGH
  else if (percent >= 50) colors = COLOR_MID

  const gradient = ctx.createLinearGradient(0, centerY - radius, 0, centerY + radius)
  gradient.addColorStop(0, colors[0])
  gradient.addColorStop(1, colors[1])

  ctx.beginPath()
  ctx.arc(centerX, centerY, radius, startAngle, valueAngle)
  ctx.strokeStyle = gradient
  ctx.lineWidth = BAR_WIDTH
  ctx.lineCap = 'round'
  if (GLOW_BLUR > 0) {
    ctx.shadowBlur = GLOW_BLUR
    ctx.shadowColor = colors[1]
  }
  ctx.stroke()
  ctx.shadowBlur = 0

  // 5. 内圈虚线装饰
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius - 25, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(0, 232, 255, 0.1)'
  ctx.lineWidth = 1
  ctx.setLineDash([4, 4])
  ctx.stroke()
  ctx.setLineDash([])

  // 6. 中心数值
  ctx.fillStyle = '#ffffff'
  ctx.font = FONT_VALUE
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  if (GLOW_BLUR > 0) {
    ctx.shadowBlur = 10
    ctx.shadowColor = colors[0]
  }
  ctx.fillText(`${displayValue.value}`, centerX, centerY - 8)
  ctx.shadowBlur = 0

  // 7. 单位
  ctx.fillStyle = LABEL_COLOR
  ctx.font = FONT_LABEL
  ctx.fillText(props.unit, centerX, centerY + 18)

  // 8. 状态文字
  const status = props.statusText
    ? { text: props.statusText, color: '#00e8ff' }
    : THRESHOLDS.find(t => percent <= t.max) || THRESHOLDS[THRESHOLDS.length - 1]
  ctx.fillStyle = status.color
  ctx.font = FONT_STATUS
  ctx.fillText(status.text, centerX, centerY + 42)
}

watch(displayValue, draw)
watch(() => props.value, (newVal) => {
  animateValue(displayValue.value, newVal)
}, { immediate: true })

onMounted(() => {
  draw()
  window.addEventListener('resize', draw)
})
</script>

<style scoped>
.flow-gauge-card {
  position: relative;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.6) 0%, rgba(15, 23, 42, 0.8) 100%);
  border: 1px solid rgba(0, 232, 255, 0.15);
  border-radius: 8px;
  overflow: hidden;
  backdrop-filter: none;
  display: flex;
  flex-direction: column;
}

.flow-gauge-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #00e8ff 30%, #00e8ff 70%, transparent 100%);
  box-shadow: 0 0 12px rgba(0, 232, 255, 0.6);
}

.gauge-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px 12px;
}

.gauge-canvas {
  width: 100%;
  height: 100%;
  max-width: 280px;
  max-height: 200px;
}
</style>
