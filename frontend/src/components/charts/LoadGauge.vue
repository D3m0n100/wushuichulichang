<template>
  <div class="load-gauge-card">
    
    <div class="gauge-wrap">
      <canvas ref="gaugeCanvas" class="gauge-canvas"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

// ============ 可配置参数 ============

// 仪表盘开合角度（单位：Math.PI 的倍数）
const START_ANGLE = 0.75   // 左侧起始：0.75 * PI = 135度
const END_ANGLE = 2.25     // 右侧结束：2.25 * PI = 405度
// 常用组合：
// 半圆(开口向上):  0.75 ~ 2.25
// 3/4圆:           1.0  ~ 2.5
// 满圆:            0    ~ 2

// 圆环半径（像素，相对于画布）
const RADIUS = 90

// 圆环粗细
const TRACK_WIDTH = 12     // 背景轨道粗细
const BAR_WIDTH = 12       // 彩色进度条粗细

// 刻度数
const TOTAL_TICKS = 60           // 总刻度线数量
const MAJOR_TICK_EVERY = 10      // 每N个刻度标一个主刻度
const TICK_LENGTH_MAJOR = 18     // 主刻度长度
const TICK_LENGTH_MINOR = 12     // 小刻度长度

// 颜色方案（三段式渐变）
const COLOR_LOW = ['#00e8ff', '#00e8ff']    // <50%: [起始色, 结束色]
const COLOR_MID = ['#00e8ff', '#ffaa00']    // 50-80%: [起始色, 结束色]
const COLOR_HIGH = ['#ffaa00', '#ff4444']   // >80%: [起始色, 结束色]

// 文字配置
const FONT_PERCENT = 'bold 32px Microsoft YaHei'          // 百分比字体
const FONT_LABEL = '500 13px Rajdhani'             // "处理负荷"字体
const FONT_STATUS = '600 12px Microsoft YaHei'            // 状态文字字体
const LABEL_COLOR = 'rgba(0, 232, 255, 0.7)'       // 标签颜色

// 阈值与状态文字
const THRESHOLDS = [
  { max: 50, text: '正常', color: '#00e8ff' },
  { max: 80, text: '高负荷', color: '#ffaa00' },
  { max: 100, text: '过载', color: '#ff4444' },
]

// 动画时长（毫秒）
const ANIMATION_DURATION = 1000

// 发光效果
const GLOW_BLUR = 15         // 发光范围（0 = 关闭发光）

// ============ 组件逻辑 ============

const props = defineProps({
  value: { type: Number, default: 68 }
})

const gaugeCanvas = ref(null)
const displayValue = ref(0)

// 缓动函数
const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3)

// 动画数值
const animateValue = (from, to) => {
  const startTime = performance.now()
  const diff = to - from

  const step = (now) => {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / ANIMATION_DURATION, 1)
    displayValue.value = Math.round(from + diff * easeOutCubic(progress))
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

// 绘制仪表盘
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

  // 1. 外圈发光环（可删除）
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

  // 3. 刻度线（可删除整块 if 不需要）
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
  const val = Math.max(0, Math.min(100, displayValue.value))
  const valueAngle = startAngle + (endAngle - startAngle) * (val / 100)

  let colors = COLOR_LOW
  if (val >= 80) colors = COLOR_HIGH
  else if (val >= 50) colors = COLOR_MID

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

  // 5. 内圈虚线装饰（可删除）
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius - 25, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(0, 232, 255, 0.1)'
  ctx.lineWidth = 1
  ctx.setLineDash([4, 4])
  ctx.stroke()
  ctx.setLineDash([])

  // 6. 中心百分比数字
  ctx.fillStyle = '#ffffff'
  ctx.font = FONT_PERCENT
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  if (GLOW_BLUR > 0) {
    ctx.shadowBlur = 10
    ctx.shadowColor = colors[0]
  }
  ctx.fillText(`${displayValue.value}%`, centerX, centerY - 5)
  ctx.shadowBlur = 0

  // 7. "处理负荷"标签
  ctx.fillStyle = LABEL_COLOR
  ctx.font = FONT_LABEL
  ctx.fillText('处理负荷', centerX, centerY + 22)

  // 8. 状态文字
  const status = THRESHOLDS.find(t => val <= t.max) || THRESHOLDS[THRESHOLDS.length - 1]
  ctx.fillStyle = status.color
  ctx.font = FONT_STATUS
  ctx.fillText(status.text, centerX, centerY + 42)
}

// 监听数值变化
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
.load-gauge-card {
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

/* 顶部光带 */
.load-gauge-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #00e8ff 30%, #00e8ff 70%, transparent 100%);
  box-shadow: 0 0 12px rgba(0, 232, 255, 0.6);
}

/* 右下角折角装饰 */
.load-gauge-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  border-right: 2px solid #00e8ff;
  border-bottom: 2px solid #00e8ff;
  opacity: 0.6;
}

/* 左上角折角 */
.corner-tl::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 12px;
  height: 12px;
  border-top: 2px solid #00e8ff;
  border-left: 2px solid #00e8ff;
  opacity: 0.7;
  z-index: 5;
}

/* 科幻标题 */
.scifi-title {
  font-family: 'Microsoft YaHei', 'Rajdhani', 'Microsoft YaHei', sans-serif;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #00e8ff;
  letter-spacing: 2px;
  text-transform: uppercase;
  text-shadow: 0 0 10px rgba(0, 232, 255, 0.5);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px 8px;
  position: relative;
}

.scifi-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, #00e8ff, #00e8ff);
  border-radius: 2px;
  box-shadow: 0 0 6px rgba(0, 232, 255, 0.8);
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
