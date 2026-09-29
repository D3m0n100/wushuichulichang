<template>
  <div class="table-scroll">
    <table class="data-table">
      <thead>
        <tr>
          <th>设备名称</th>
          <th>位置</th>
          <th>状态</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in data" :key="row.deviceName">
          <td class="device-name">{{ row.deviceName }}</td>
          <td class="device-location">{{ row.location }}</td>
          <td>
            <span class="status-dot" :class="row.status === '运行' ? 'ok' : 'off'"></span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
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
.table-scroll {
  flex: 1;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: clamp(12px, min(1.3vw, 1.8vh), 18px);
}

.data-table th {
  text-align: left;
  padding: clamp(3px, 0.4vh, 7px) clamp(3px, 0.3vw, 6px);
  color: #00e8ff;
  font-weight: 600;
  font-size: clamp(11px, min(1.1vw, 1.5vh), 16px);
  font-family: var(--font-display);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(0, 232, 255, 0.2);
  position: sticky;
  top: 0;
  background: rgba(15, 23, 42, 0.95);
}

.data-table td {
  padding: clamp(3px, 0.4vh, 7px) clamp(3px, 0.3vw, 6px);
  border-bottom: 1px solid rgba(0, 232, 255, 0.08);
  transition: all 0.2s ease;
}

.data-table tbody tr:hover td {
  background: rgba(0, 232, 255, 0.08);
}

.device-name {
  color: #e0eaf5;
  font-weight: 600;
  font-size: clamp(12px, min(1.2vw, 1.7vh), 18px);
}

.device-location {
  color: rgba(0, 232, 255, 0.7);
  font-size: clamp(11px, min(1.1vw, 1.6vh), 17px);
}

.status-dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.status-dot.ok {
  background: var(--color-success);
  box-shadow: 0 0 4px var(--color-success);
}

.status-dot.off {
  background: rgba(0, 232, 255, 0.4);
  box-shadow: 0 0 4px rgba(0, 232, 255, 0.3);
}
</style>
