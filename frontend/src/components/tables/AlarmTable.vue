<template>
  <el-table
    :data="data"
    style="width: 100%"
    stripe
    height="150"
    :header-cell-style="{ background: 'transparent', color: '#f56c6c' }"
    :cell-style="{ background: 'transparent', color: '#fff' }"
  >
    <el-table-column prop="level" label="级别" width="80">
      <template #default="{ row }">
        <el-tag :type="row.level === '严重' ? 'danger' : (row.level === '警告' ? 'warning' : 'info')" size="small">
          {{ row.level }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="deviceName" label="设备" />
    <el-table-column prop="message" label="警告内容" show-overflow-tooltip />
    <el-table-column prop="time" label="发生时间" width="160" />
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
:deep(.el-table__inner-wrapper::before) {
  display: none !important;
}
/* 移除所有表格底部边框（最稳妥） */
:deep(.el-table),
:deep(.el-table__body-wrapper .el-table__row:last-child td) {
  border-bottom: none !important;
}
/* 同时去除表格容器底部伪元素 */
:deep(.el-table::after) {
  border-bottom: none !important;
}
:deep(.el-table) {
  background-color: transparent;
}
:deep(.el-table__row) {
  background-color: transparent !important;
}
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02);
}
</style>