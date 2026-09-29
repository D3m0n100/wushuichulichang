<template>
  <BaseChart :options="chartOptions" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  xAxis: { type: Array, default: () => [] },
  seriesName: { type: String, default: '数值' },
  height: { type: String, default: '300px' }
})

const chartOptions = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: props.xAxis, axisLabel: { color: '#aaa' } },
  yAxis: { type: 'value', axisLabel: { color: '#aaa' } },
  series: [{
    name: props.seriesName,
    type: 'bar',
    data: props.data,
    itemStyle: { color: '#7ec8e0' }
  }]
}))
</script>