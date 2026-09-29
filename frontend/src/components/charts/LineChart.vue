<template>
  <BaseChart :options="chartOptions" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  series: { type: Array, required: true }, // 支持多条线
  xAxis: { type: Array, required: true },
  height: { type: String, default: '300px' }
})

const chartOptions = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: props.xAxis, axisLabel: { color: '#aaa' } },
  yAxis: { type: 'value', axisLabel: { color: '#aaa' } },
  series: props.series.map(s => ({
    ...s,
    smooth: true,
    lineStyle: { color: s.color || '#7ec8e0' }
  }))
}))
</script>