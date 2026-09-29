<template>
  <el-table
    :data="data"
    style="width: 100%"
    stripe
    
    :header-cell-style="{ color: '#7ec8e0' }"  >
  >
    <el-table-column prop="point" label="监测点" />
    <el-table-column prop="ph" label="pH" />
    <el-table-column prop="do" label="DO (mg/L)" />
    <el-table-column prop="turbidity" label="浊度 (NTU)" />
  </el-table>
</template>

<script setup>
defineProps({
  data: {
    type: Array,
    required: true,
    default: () => []
  }
})
</script>

<style scoped>
/* 全局表格字体缩小 */
:deep(.el-table) {
  font-size: 0.75rem !important;
  background-color: transparent;
}

/* 表头字体 */
:deep(.el-table th) {
  font-size: 0.75rem !important;
}

/* 数据行字体 */
:deep(.el-table td) {
  font-size: 0.75rem !important;
}

/* 关键：缩小单元格内容的内边距和行高 */
:deep(.el-table .cell) {
  padding: 0 8px !important;
  line-height: 15px !important;
}

/* 可选：缩小单元格上下内边距（若需进一步紧凑） */
:deep(.el-table td),
:deep(.el-table th) {
  padding: 2px 0 !important;
}

/* 数据行文字白色 */
:deep(.el-table__row td) {
  color: #ffffff !important;
}

/* 斑马纹行背景微调 */
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02);
}

/* 移除底部边框（保持原有） */
:deep(.el-table),
:deep(.el-table__body-wrapper .el-table__row:last-child td) {
  border-bottom: none !important;
}
:deep(.el-table::after) {
  border-bottom: none !important;
}
:deep(.el-table__inner-wrapper::before) {
  display: none !important;
}
</style>