<template>
  <div ref="chartRef" :style="{ width, height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  options: { type: Object, required: true },
  width: { type: String, default: '100%' },
  height: { type: String, default: '300px' }
})

const chartRef = ref(null)
let chartInstance = null

// 初始化图表
const initChart = () => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption(props.options)
  }
}

// 窗口resize防抖
const resizeHandler = () => {
  if (chartInstance) chartInstance.resize()
}

onMounted(() => {
  nextTick(() => {
    initChart()
    window.addEventListener('resize', resizeHandler)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  if (chartInstance) chartInstance.dispose()
})

// 监听options变化，更新图表
watch(() => props.options, (newOpt) => {
  if (chartInstance) chartInstance.setOption(newOpt)
}, { deep: true })
</script>