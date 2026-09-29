<template>
  <BaseChart :options="lineOptions" height="160px" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({ data: Array })

const lineOptions = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: {
    left: '3%',      // 左侧留白减少
    right: '3%',     // 右侧留白减少
    top: '18%',       // 顶部留白减少
    bottom: '8%',    // 底部留白减少
    containLabel: true  // 防止坐标轴标签被裁剪
  },
  xAxis: { 
    type: 'category', 
    data: props.data.map(item => item.time), 
    axisLabel: { color: '#aaa' } 
  },
  yAxis: { 
    type: 'value', 
    name: 'COD (mg/L)', 
    nameTextStyle: { color: '#aaa' }, 
    axisLabel: { color: '#aaa' } 
  },
  series: [{
    data: props.data.map(item => item.value),
    type: 'line',
    smooth: true,
    areaStyle: { color: 'rgba(126,200,224,0.2)' },
    lineStyle: { color: '#7ec8e0' }
  }]
}))

</script>
