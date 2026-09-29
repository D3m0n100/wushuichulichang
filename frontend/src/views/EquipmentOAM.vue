<template>
  <div class="oam-page">
    <div class="wave-overlay"></div>

    <div class="page-header">
      <div class="header-title-group">
        <span class="header-dot"></span>
        <h2 class="header-title">设备运维管理</h2>
        <span class="header-line"></span>
        <span class="header-sub">EQUIPMENT O&amp;M MANAGEMENT</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="oam-tabs">
      <!-- ==================== Tab 1: 设备台账 ==================== -->
      <el-tab-pane label="设备台账" name="ledger">
        <div class="tab-content">
          <div class="stat-row">
            <div v-for="s in summaryStats" :key="s.label" class="stat-card frame-card">
              <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
              <div class="frame-corner bl"></div><div class="frame-corner br"></div>
              <div class="stat-icon">{{ s.icon }}</div>
              <div class="stat-info">
                <div class="stat-label">{{ s.label }}</div>
                <div class="stat-value">{{ s.value }} <span class="stat-unit">{{ s.unit }}</span></div>
              </div>
            </div>
          </div>

          <!-- 工单管理 -->
          <div class="wo-stats">
            <div v-for="s in woStatCards" :key="s.label" class="wo-stat frame-card">
              <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
              <div class="frame-corner bl"></div><div class="frame-corner br"></div>
              <div class="wo-stat-val" :style="{ color: s.color }">{{ s.value }}</div>
              <div class="wo-stat-label">{{ s.label }}</div>
            </div>
          </div>

          <div class="table-card frame-card">
            <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div><div class="frame-corner br"></div>
            <div class="card-header-bar">
              <span class="pulse-dot"></span>维修工单
              <div style="flex:1"></div>
              <el-button type="success" size="small" @click="exportWorkOrders">导出 Excel</el-button>
              <el-button type="primary" size="small" @click="addWorkOrder">+ 添加</el-button>
            </div>
            <el-table :data="workOrders" stripe border class="dark-table" empty-text="暂无工单">
              <el-table-column prop="id" label="工单号" width="140">
                <template #default="{ row }">
                  <el-input v-if="row._editing" v-model="row.id" size="small" placeholder="工单号" />
                  <span v-else>{{ row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="device" label="设备" width="160">
                <template #default="{ row }">
                  <el-input v-if="row._editing" v-model="row.device" size="small" placeholder="请输入设备" />
                  <span v-else>{{ row.device }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="fault" label="故障描述">
                <template #default="{ row }">
                  <el-input v-if="row._editing" v-model="row.fault" size="small" placeholder="请输入故障描述" />
                  <span v-else>{{ row.fault }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="priority" label="优先级" width="100">
                <template #default="{ row }">
                  <el-select v-if="row._editing" v-model="row.priority" size="small" style="width:100%" @change="updatePriorityLabel(row)">
                    <el-option label="紧急" value="high" />
                    <el-option label="一般" value="mid" />
                    <el-option label="低" value="low" />
                  </el-select>
                  <span v-else class="priority-tag" :class="row.priority">{{ row.priorityLabel }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="assignee" label="维修人" width="100">
                <template #default="{ row }">
                  <el-input v-if="row._editing" v-model="row.assignee" size="small" placeholder="请输入" />
                  <span v-else>{{ row.assignee }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="160" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-select v-if="row._editing" v-model="row.status" size="small" style="width:100%" @change="updateStatusClass(row)">
                    <el-option label="待处理" value="待处理" />
                    <el-option label="维修中" value="维修中" />
                    <el-option label="已完成" value="已完成" />
                  </el-select>
                  <span v-else class="status-pill" :class="row.statusClass">{{ row.status }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row, $index }">
                  <template v-if="row._editing">
                    <el-button type="success" size="small" link @click="saveWorkOrder($index)">保存</el-button>
                    <el-button type="danger" size="small" link @click="cancelWorkOrder($index)">取消</el-button>
                  </template>
                  <template v-else>
                    <el-button type="primary" size="small" link @click="editWorkOrder($index)">编辑</el-button>
                    <el-button type="danger" size="small" link @click="deleteWorkOrder($index)">删除</el-button>
                  </template>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- ==================== Tab 2: 维护保养 ==================== -->
      <el-tab-pane label="维护保养" name="maintenance">
        <div class="tab-content">
          <div class="plan-row">
            <div class="plan-card frame-card">
              <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
              <div class="frame-corner bl"></div><div class="frame-corner br"></div>
              <div class="card-header-bar">
                <span class="pulse-dot"></span>保养计划
                <div style="flex:1"></div>
                <el-button type="success" size="small" @click="exportPlans">导出 Excel</el-button>
                <el-button type="primary" size="small" @click="addPlan">+ 添加</el-button>
              </div>
              <el-table :data="maintenancePlans" stripe border class="dark-table" empty-text="暂无计划">
                <el-table-column type="index" label="序号" width="55" />
                <el-table-column prop="device" label="设备" width="160">
                  <template #default="{ row, $index }">
                    <el-input v-if="row._editing" v-model="row.device" size="small" placeholder="请输入设备" />
                    <span v-else>{{ row.device }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="content" label="保养内容">
                  <template #default="{ row }">
                    <el-input v-if="row._editing" v-model="row.content" size="small" placeholder="请输入保养内容" />
                    <span v-else>{{ row.content }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="date" label="计划日期" width="120">
                  <template #default="{ row }">
                    <el-date-picker v-if="row._editing" v-model="row.date" type="date" size="small" value-format="YYYY-MM-DD" style="width:100%" />
                    <span v-else>{{ row.date }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="person" label="负责人" width="100">
                  <template #default="{ row }">
                    <el-input v-if="row._editing" v-model="row.person" size="small" placeholder="请输入" />
                    <span v-else>{{ row.person }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{ row, $index }">
                    <template v-if="row._editing">
                      <el-button type="success" size="small" link @click="savePlan($index)">保存</el-button>
                      <el-button type="danger" size="small" link @click="cancelPlan($index)">取消</el-button>
                    </template>
                    <template v-else>
                      <el-button type="primary" size="small" link @click="editPlan($index)">编辑</el-button>
                      <el-button type="danger" size="small" link @click="deletePlan($index)">删除</el-button>
                    </template>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="plan-card frame-card">
              <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
              <div class="frame-corner bl"></div><div class="frame-corner br"></div>
              <div class="card-header-bar">
                <span class="pulse-dot"></span>维保记录
                <div style="flex:1"></div>
                <el-button type="success" size="small" @click="exportRecords">导出 Excel</el-button>
                <el-button type="primary" size="small" @click="addRecord">+ 添加</el-button>
              </div>
              <el-table :data="maintenanceRecords" stripe border class="dark-table" empty-text="暂无记录">
                <el-table-column type="index" label="序号" width="55" />
                <el-table-column prop="date" label="日期" width="120">
                  <template #default="{ row }">
                    <el-date-picker v-if="row._editing" v-model="row.date" type="date" size="small" value-format="YYYY-MM-DD" style="width:100%" />
                    <span v-else>{{ row.date }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="device" label="设备" width="140">
                  <template #default="{ row }">
                    <el-input v-if="row._editing" v-model="row.device" size="small" placeholder="请输入设备" />
                    <span v-else>{{ row.device }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="80">
                  <template #default="{ row }">
                    <el-input v-if="row._editing" v-model="row.type" size="small" placeholder="类型" />
                    <span v-else>{{ row.type }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="content" label="维保内容">
                  <template #default="{ row }">
                    <el-input v-if="row._editing" v-model="row.content" size="small" placeholder="请输入内容" />
                    <span v-else>{{ row.content }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="person" label="负责人" width="80">
                  <template #default="{ row }">
                    <el-input v-if="row._editing" v-model="row.person" size="small" placeholder="姓名" />
                    <span v-else>{{ row.person }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="result" label="结果" width="80">
                  <template #default="{ row }">
                    <el-select v-if="row._editing" v-model="row.result" size="small" style="width:100%">
                      <el-option label="正常" value="正常" />
                      <el-option label="异常" value="异常" />
                    </el-select>
                    <span v-else class="status-pill" :class="row.result === '正常' ? 'ok' : 'warn'">{{ row.result }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{ row, $index }">
                    <template v-if="row._editing">
                      <el-button type="success" size="small" link @click="saveRecord($index)">保存</el-button>
                      <el-button type="danger" size="small" link @click="cancelRecord($index)">取消</el-button>
                    </template>
                    <template v-else>
                      <el-button type="primary" size="small" link @click="editRecord($index)">编辑</el-button>
                      <el-button type="danger" size="small" link @click="deleteRecord($index)">删除</el-button>
                    </template>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'

const activeTab = ref('ledger')

// ====================== localStorage 工具函数 ======================
const STORAGE_KEYS = {
  plans: 'OAM_MAINTENANCE_PLANS',
  records: 'OAM_MAINTENANCE_RECORDS',
  orders: 'OAM_WORK_ORDERS'
}

const safeRead = (key, fallback) => {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

const defaultPlans = [
  { device: '曝气风机 H1-FAN-01', content: '更换润滑油、检查皮带张紧度、清理进气滤网', date: '2026-06-20', person: '张工' },
  { device: '提升泵 B1-P-01', content: '机械密封检查、叶轮磨损检测、电缆绝缘测试', date: '2026-06-25', person: '李工' },
  { device: '紫外消毒 P2-UV-01', content: '灯管强度检测、石英套管清洗、镇流器检查', date: '2026-07-05', person: '王工' }
]

const defaultRecords = [
  { date: '2026-06-10', device: '曝气风机', type: '保养', content: '更换润滑油、清理滤网', person: '张工', result: '正常' },
  { date: '2026-06-05', device: '紫外消毒', type: '更换', content: '更换 UV 灯管 2 支', person: '王工', result: '正常' },
  { date: '2026-06-01', device: '提升泵', type: '维修', content: '更换机械密封', person: '李工', result: '正常' }
]

const defaultOrders = [
  { id: 'WO-20260615-01', device: '潜水搅拌器 H1-MIX-01', fault: '运行异响、轴承磨损严重', priority: 'high', priorityLabel: '紧急', assignee: '张工', createTime: '2026-06-15 08:30', status: '维修中', statusClass: 'warn' },
  { id: 'WO-20260614-01', device: '低压配电室 B 出线柜 #5', fault: '过流保护跳闸', priority: 'high', priorityLabel: '紧急', assignee: '赵工', createTime: '2026-06-14 10:24', status: '维修中', statusClass: 'warn' },
  { id: 'WO-20260613-01', device: '提升泵 B1-P-02', fault: '备用泵定期试运行异常', priority: 'mid', priorityLabel: '一般', assignee: '李工', createTime: '2026-06-13 14:00', status: '待处理', statusClass: 'idle' }
]

// ====================== 设备台账 ======================
const summaryStats = ref([
  { label: '设备总数', icon: '🏭', value: 86, unit: '台' },
  { label: '运行中', icon: '✅', value: 72, unit: '台' },
  { label: '故障中', icon: '🔧', value: 8, unit: '台' }
])

// ====================== 维护保养 ======================
const maintenancePlans = ref(safeRead(STORAGE_KEYS.plans, defaultPlans).map(item => ({ ...item, _editing: false })))

watch(maintenancePlans, (val) => {
  const toSave = val.map(({ _editing, ...rest }) => rest)
  localStorage.setItem(STORAGE_KEYS.plans, JSON.stringify(toSave))
}, { deep: true })

const addPlan = () => {
  maintenancePlans.value.unshift({ device: '', content: '', date: '', person: '', _editing: true })
}

const editPlan = (index) => {
  maintenancePlans.value[index]._editing = true
}

const savePlan = (index) => {
  maintenancePlans.value[index]._editing = false
}

const cancelPlan = (index) => {
  if (!maintenancePlans.value[index].device && !maintenancePlans.value[index].content) {
    maintenancePlans.value.splice(index, 1)
  } else {
    maintenancePlans.value[index]._editing = false
  }
}

const deletePlan = (index) => {
  maintenancePlans.value.splice(index, 1)
}

const maintenanceRecords = ref(safeRead(STORAGE_KEYS.records, defaultRecords).map(item => ({ ...item, _editing: false })))

watch(maintenanceRecords, (val) => {
  const toSave = val.map(({ _editing, ...rest }) => rest)
  localStorage.setItem(STORAGE_KEYS.records, JSON.stringify(toSave))
}, { deep: true })

const addRecord = () => {
  maintenanceRecords.value.unshift({ date: '', device: '', type: '', content: '', person: '', result: '正常', _editing: true })
}

const editRecord = (index) => {
  maintenanceRecords.value[index]._editing = true
}

const saveRecord = (index) => {
  maintenanceRecords.value[index]._editing = false
}

const cancelRecord = (index) => {
  if (!maintenanceRecords.value[index].device && !maintenanceRecords.value[index].content) {
    maintenanceRecords.value.splice(index, 1)
  } else {
    maintenanceRecords.value[index]._editing = false
  }
}

const deleteRecord = (index) => {
  maintenanceRecords.value.splice(index, 1)
}

// ====================== 工单管理 ======================
const woStatCards = ref([
  { label: '待处理', value: 3, color: '#ffbf66' },
  { label: '进行中', value: 2, color: '#00e8ff' },
  { label: '已完成', value: 28, color: '#76ffc8' },
  { label: '本月总数', value: 33, color: '#F2F3F5' }
])

const workOrders = ref(safeRead(STORAGE_KEYS.orders, defaultOrders).map(item => ({ ...item, _editing: false })))

watch(workOrders, (val) => {
  const toSave = val.map(({ _editing, ...rest }) => rest)
  localStorage.setItem(STORAGE_KEYS.orders, JSON.stringify(toSave))
}, { deep: true })

const priorityMap = { high: '紧急', mid: '一般', low: '低' }
const statusClassMap = { '待处理': 'idle', '维修中': 'warn', '已完成': 'ok' }

const updatePriorityLabel = (row) => {
  row.priorityLabel = priorityMap[row.priority] || '一般'
}

const updateStatusClass = (row) => {
  row.statusClass = statusClassMap[row.status] || 'idle'
}

const formatNow = () => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const addWorkOrder = () => {
  workOrders.value.unshift({
    id: '', device: '', fault: '', priority: 'mid', priorityLabel: '一般',
    assignee: '', createTime: formatNow(), status: '待处理', statusClass: 'idle', _editing: true
  })
}

const editWorkOrder = (index) => {
  workOrders.value[index]._editing = true
}

const saveWorkOrder = (index) => {
  workOrders.value[index]._editing = false
}

const cancelWorkOrder = (index) => {
  if (!workOrders.value[index].device && !workOrders.value[index].fault) {
    workOrders.value.splice(index, 1)
  } else {
    workOrders.value[index]._editing = false
  }
}

const deleteWorkOrder = (index) => {
  workOrders.value.splice(index, 1)
}

// ====================== 导出 Excel ======================
const exportWorkOrders = () => {
  if (!workOrders.value?.length) return ElMessage.warning('暂无工单数据')
  const header = ['工单号', '设备', '故障描述', '优先级', '维修人', '创建时间', '状态']
  const rows = workOrders.value.map(r => [r.id, r.device, r.fault, r.priorityLabel, r.assignee, r.createTime, r.status])
  const ws = XLSX.utils.aoa_to_sheet([header, ...rows])
  ws['!cols'] = [{ wch: 18 }, { wch: 24 }, { wch: 30 }, { wch: 10 }, { wch: 10 }, { wch: 20 }, { wch: 10 }]
  const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, '维修工单')
  XLSX.writeFile(wb, `维修工单_${new Date().toISOString().slice(0,10)}.xlsx`)
  ElMessage.success('维修工单导出成功')
}

const exportPlans = () => {
  if (!maintenancePlans.value?.length) return ElMessage.warning('暂无保养计划')
  const header = ['序号', '设备', '保养内容', '计划日期', '负责人']
  const rows = maintenancePlans.value.map((r, i) => [i + 1, r.device, r.content, r.date, r.person])
  const ws = XLSX.utils.aoa_to_sheet([header, ...rows])
  ws['!cols'] = [{ wch: 8 }, { wch: 24 }, { wch: 40 }, { wch: 14 }, { wch: 10 }]
  const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, '保养计划')
  XLSX.writeFile(wb, `保养计划_${new Date().toISOString().slice(0,10)}.xlsx`)
  ElMessage.success('保养计划导出成功')
}

const exportRecords = () => {
  if (!maintenanceRecords.value?.length) return ElMessage.warning('暂无维保记录')
  const header = ['序号', '日期', '设备', '类型', '维保内容', '负责人', '结果']
  const rows = maintenanceRecords.value.map((r, i) => [i + 1, r.date, r.device, r.type, r.content, r.person, r.result])
  const ws = XLSX.utils.aoa_to_sheet([header, ...rows])
  ws['!cols'] = [{ wch: 8 }, { wch: 14 }, { wch: 18 }, { wch: 10 }, { wch: 36 }, { wch: 10 }, { wch: 10 }]
  const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, '维保记录')
  XLSX.writeFile(wb, `维保记录_${new Date().toISOString().slice(0,10)}.xlsx`)
  ElMessage.success('维保记录导出成功')
}
</script>

<style scoped>
.oam-page {
  position: relative;
  min-height: calc(100vh - var(--nav-height, 132px));
  padding: 20px 24px;
  background:
    radial-gradient(circle at 50% -10%, rgba(88, 214, 255, 0.12), transparent 36%),
    linear-gradient(180deg, #1E293B 0%, #1E293B 100%);
  overflow: hidden;
}

.wave-overlay {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background: repeating-linear-gradient(0deg, transparent 0 2px, rgba(0, 232, 255, 0.015) 2px 4px);
  mask-image: linear-gradient(180deg, rgba(255,255,255,0.6), transparent 90%);
}

.page-header { position: relative; z-index: 1; margin-bottom: 16px; }
.header-title-group { display: flex; align-items: center; gap: 10px; }
.header-dot { width: 8px; height: 8px; border-radius: 50%; background: #00e8ff; box-shadow: 0 0 8px rgba(0,232,255,0.5); }
.header-title { font-size: 1.5rem; font-weight: 700; color: #F2F3F5; margin: 0; }
.header-line { width: 40px; height: 2px; background: linear-gradient(90deg, rgba(0,128,255,0.6), transparent); }
.header-sub { font-size: 0.9375rem; color: rgba(140,238,255,0.6); letter-spacing: 2px; }

/* Tabs */
.oam-tabs { position: relative; z-index: 1; }
:deep(.el-tabs__header) { margin-bottom: 16px; }
:deep(.el-tabs__item) { color: rgba(0,232,255,0.55); font-size: 1.0625rem; }
:deep(.el-tabs__item.is-active) { color: #00e8ff; }
:deep(.el-tabs__active-bar) { background: #00e8ff; }
:deep(.el-tabs__nav-wrap::after) { background: rgba(0,128,255,0.15); }

.tab-content { display: flex; flex-direction: column; gap: 16px; }

/* Frame card */
.frame-card {
  position: relative; border: 1px solid rgba(0,128,255,0.22);
  background: radial-gradient(circle at top left, rgba(0,128,255,0.08), transparent 42%),
    linear-gradient(145deg, rgba(74,168,214,0.12), rgba(34,102,142,0.04) 48%, rgba(18,58,84,0.02));
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.04), 0 4px 16px rgba(0,0,0,0.03);
  backdrop-filter: saturate(120%);
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
}
.frame-corner { position: absolute; width: 20px; height: 20px; pointer-events: none; }
.frame-corner.tl { top: -1px; left: -1px; border-top: 2px solid rgba(0,232,255,0.5); border-left: 2px solid rgba(0,232,255,0.5); }
.frame-corner.tr { top: -1px; right: -1px; border-top: 2px solid rgba(0,232,255,0.3); border-right: 2px solid rgba(0,232,255,0.3); }
.frame-corner.bl { bottom: -1px; left: -1px; border-bottom: 2px solid rgba(0,232,255,0.3); border-left: 2px solid rgba(0,232,255,0.3); }
.frame-corner.br { bottom: -1px; right: -1px; border-bottom: 2px solid rgba(0,232,255,0.5); border-right: 2px solid rgba(0,232,255,0.5); }

.card-header-bar {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  font-size: 1.0625rem; font-weight: 600; color: #C9CDD4; border-bottom: 1px solid rgba(0,128,255,0.12);
}
.pulse-dot { width: 7px; height: 7px; border-radius: 50%; background: #00e8ff; box-shadow: 0 0 8px rgba(0,232,255,0.5); animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: 0.6; } 50% { opacity: 1; } }

/* Stat row */
.stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 18px 16px; }
.stat-icon { font-size: 2.125rem; }
.stat-info { flex: 1; }
.stat-label { font-size: 0.9375rem; color: #C9CDD4; }
.stat-value { font-size: 1.875rem; font-weight: 700; color: #F2F3F5; font-family: var(--font-display); margin-top: 4px; }
.stat-unit { font-size: 0.9375rem; color: rgba(0,232,255,0.6); font-weight: 400; }

/* Table */
.table-card { padding: 0; }
.dark-table { width: 100%; }
:deep(.el-table) { background: transparent !important; color: #C9CDD4 !important; --el-table-border-color: rgba(0,128,255,0.12); --el-table-header-bg-color: rgba(0,128,255,0.06); --el-table-tr-bg-color: transparent; --el-table-row-hover-bg-color: rgba(0,232,255,0.06); }
:deep(.el-table__header-wrapper th.el-table__cell) { background: rgba(0,128,255,0.06) !important; color: #C9CDD4 !important; border-bottom: 1px solid rgba(0,128,255,0.15) !important; font-weight: 600; }
:deep(.el-table__row td.el-table__cell) { border-bottom: 1px solid rgba(0,128,255,0.08) !important; }
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) { background: rgba(0,128,255,0.03) !important; }
:deep(.el-table__empty-block) { background: transparent; }
:deep(.el-input__wrapper) { background: rgba(0,128,255,0.06) !important; box-shadow: 0 0 0 1px rgba(0,128,255,0.2) inset !important; }
:deep(.el-input__inner) { color: #C9CDD4 !important; }

.status-pill { padding: 3px 10px; border-radius: 999px; font-size: 0.875rem; font-weight: 600; }
.status-pill.ok { color: #88ffd8; background: rgba(48,230,173,0.12); border: 1px solid rgba(48,230,173,0.25); }
.status-pill.idle { color: #8ad9ff; background: rgba(0,128,255,0.12); border: 1px solid rgba(0,128,255,0.25); }
.status-pill.fault { color: #ffaeae; background: rgba(255,122,122,0.15); border: 1px solid rgba(255,122,122,0.3); }
.status-pill.warn { color: #ffab00; background: rgba(255,191,102,0.12); border: 1px solid rgba(255,191,102,0.25); }

/* Maintenance plans */
.plan-row { display: grid; grid-template-columns: 1fr 1.5fr; gap: 14px; }
.plan-list { padding: 8px 16px 16px; display: flex; flex-direction: column; gap: 12px; }
.plan-item { padding: 14px; border: 1px solid rgba(0,128,255,0.14); border-radius: 6px; background: rgba(0,128,255,0.03); }
.plan-item.overdue { border-left: 3px solid #ff7a7a; }
.plan-item.soon { border-left: 3px solid #ffbf66; }
.plan-item.normal { border-left: 3px solid #30e6ad; }
.plan-top { display: flex; align-items: center; justify-content: space-between; }
.plan-device { font-size: 1.0625rem; font-weight: 600; color: #F2F3F5; }
.plan-tag { padding: 2px 8px; border-radius: 999px; font-size: 0.8125rem; font-weight: 600; }
.plan-tag.overdue { color: #ffaeae; background: rgba(255,122,122,0.15); }
.plan-tag.soon { color: #ffab00; background: rgba(255,191,102,0.12); }
.plan-tag.normal { color: #88ffd8; background: rgba(48,230,173,0.12); }
.plan-desc { margin-top: 8px; font-size: 0.9375rem; color: #C9CDD4; line-height: 1.5; }
.plan-meta { margin-top: 8px; display: flex; gap: 20px; font-size: 0.875rem; color: rgba(0,232,255,0.5); }

/* Work order stats */
.wo-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.wo-stat { text-align: center; padding: 18px 12px; }
.wo-stat-val { font-size: 2.125rem; font-weight: 700; font-family: var(--font-display); }
.wo-stat-label { font-size: 0.9375rem; color: #C9CDD4; margin-top: 6px; }

.priority-tag { padding: 2px 8px; border-radius: 999px; font-size: 0.8125rem; font-weight: 600; }
.priority-tag.high { color: #ffaeae; background: rgba(255,122,122,0.15); }
.priority-tag.mid { color: #ffab00; background: rgba(255,191,102,0.12); }
.priority-tag.low { color: #8ad9ff; background: rgba(0,128,255,0.12); }

@media (max-width: 1200px) {
  .stat-row, .wo-stats { grid-template-columns: repeat(2, 1fr); }
  .plan-row { grid-template-columns: 1fr; }
}
</style>
