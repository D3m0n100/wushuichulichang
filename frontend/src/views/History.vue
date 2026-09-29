<template>
  <div class="history-page">
    <!-- Ambient wave -->
    <div class="wave-overlay"></div>

    <!-- Query Card -->
    <div class="query-card">
      <div class="frame-corner tl"></div>
      <div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div>
      <div class="frame-corner br"></div>
      <div class="card-header-bar">
        <span class="header-pulse"></span>
        <span class="header-text">数据库历史数据查询</span>
      </div>
      <div class="card-body-inner">
        <el-form :model="query" inline>
          <el-form-item label="监测点">
            <el-select v-model="query.dbCode" placeholder="选择监测点" filterable style="width: 220px">
              <el-option v-for="item in dbList" :key="item.db_code" :label="item.db_name" :value="item.db_code" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="timeRange" type="datetimerange" range-separator="至"
              start-placeholder="开始时间" end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss" style="width: 340px"
            />
          </el-form-item>
          <el-form-item label="间隔">
            <el-select v-model="query.interval" style="width: 100px">
              <el-option label="5分钟" :value="5" />
              <el-option label="10分钟" :value="10" />
              <el-option label="15分钟" :value="15" />
              <el-option label="30分钟" :value="30" />
              <el-option label="1小时" :value="60" />
              <el-option label="1天" :value="1440" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery" :icon="Search">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="success" @click="handleExportExcel">导出 Excel</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="view-tabs" role="tablist" aria-label="历史数据视图">
      <button
        class="view-tab"
        :class="{ active: activeView === 'table' }"
        role="tab"
        :aria-selected="activeView === 'table'"
        @click="switchView('table')"
      >数据矩阵</button>
      <button
        class="view-tab"
        :class="{ active: activeView === 'flow' }"
        role="tab"
        :aria-selected="activeView === 'flow'"
        @click="switchView('flow')"
      >进水流量分析</button>
    </div>

    <!-- Table Card -->
    <div v-show="activeView === 'table'" class="table-card" v-loading="loading">
      <div class="frame-corner tl"></div>
      <div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div>
      <div class="frame-corner br"></div>
      <div class="card-header-bar">
        <span class="header-pulse success"></span>
        <span class="header-text">数据矩阵</span>
        <span class="header-meta" v-if="deviceColumns.length > 0">
          {{ timeColumns.length }} 时间点 × {{ deviceColumns.length }} 设备
        </span>
        <span v-if="hasQueried && rawData.length === 0" class="header-warn">无数据</span>
      </div>
      <div class="card-body-inner table-body">
        <el-table
          :data="tableData" border stripe
          height="100%"
          :header-cell-style="headerStyle"
          style="width: 100%;"
        >
          <el-table-column type="index" label="序号" width="55" align="center" fixed="left" />
          <el-table-column label="采集时间" width="160" fixed="left" align="center">
            <template #default="{ row }">
              <span class="time-label">{{ row.time }}</span>
            </template>
          </el-table-column>
          <el-table-column v-for="device in deviceColumns" :key="device.deviceId" align="center" min-width="110">
            <template #header>
              <div class="device-header">
                <div class="dh-name">{{ device.deviceName }}</div>
                <div class="dh-meta">
                  <span class="dh-type">{{ device.deviceType }}</span>
                  <span class="dh-unit">({{ device.unit }})</span>
                </div>
              </div>
            </template>
            <template #default="{ row }">
              <span :class="getValueClass(row[device.deviceId], device)">{{ formatValue(row[device.deviceId], device) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Inflow chart card -->
    <div v-show="activeView === 'flow'" class="flow-card" v-loading="flowLoading">
      <div class="frame-corner tl"></div>
      <div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div>
      <div class="frame-corner br"></div>
      <div class="card-header-bar">
        <span class="header-pulse success"></span>
        <span class="header-text">进水流量折线图</span>
        <span class="header-meta">{{ flowRangeLabel }}</span>
      </div>
      <div class="flow-toolbar">
        <div class="flow-filter-group">
          <span class="flow-filter-label">粒度</span>
          <el-radio-group v-model="flowResolution" size="small" @change="handleFlowResolutionChange">
            <el-radio-button v-for="item in flowResolutionOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="flow-filter-group">
          <span class="flow-filter-label">范围</span>
          <el-checkbox-group v-model="selectedFlowPhases" size="small" @change="handlePhaseSelectionChange">
            <el-checkbox-button v-for="item in flowPhaseOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-checkbox-button>
          </el-checkbox-group>
        </div>
        <div class="flow-filter-group">
          <span class="flow-filter-label">类型</span>
          <el-checkbox-group v-model="selectedFlowTypes" size="small" @change="handleTypeSelectionChange">
            <el-checkbox-button v-for="item in flowTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-checkbox-button>
          </el-checkbox-group>
        </div>
        <div class="flow-sample-meta">
          <strong>{{ flowHistoryRows.length }}</strong>
          <span>时间点</span>
        </div>
      </div>
      <div class="flow-chart-wrap">
        <div ref="flowChartRef" class="flow-chart"></div>
        <div v-if="!flowLoading && flowHistoryRows.length === 0" class="flow-empty">所选时间范围暂无进水流量数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import * as echarts from 'echarts'
import * as XLSX from 'xlsx'

const loading = ref(false)
const dbList = ref([])
const tableData = ref([])
const deviceColumns = ref([])
const timeColumns = ref([])
const rawData = ref([])
const hasQueried = ref(false)
const activeView = ref('table')
const flowLoading = ref(false)
const flowHistoryRows = ref([])
const flowChartRef = ref(null)
let flowChart = null

const query = reactive({ dbCode: '', interval: 5 })
const timeRange = ref([])
const ALL_MONITOR_POINTS = '__ALL_MONITOR_POINTS__'

const TOTALIZER_POINTS = [
  {
    db_code: 'monitor_phase1_totalizer',
    db_name: '进水监测室-一期累计流量计（monitor_phase1_totalizer）',
    point_name: '进水监测室-一期累计流量计',
    unit: 'm³',
    raw_save_cycle_type: 'minute',
    flow_field: 'phase1_totalizer',
    is_totalizer: true,
  },
  {
    db_code: 'monitor_phase2_totalizer',
    db_name: '进水监测室-二期累计流量计（monitor_phase2_totalizer）',
    point_name: '进水监测室-二期累计流量计',
    unit: 'm³',
    raw_save_cycle_type: 'minute',
    flow_field: 'phase2_totalizer',
    is_totalizer: true,
  },
  {
    db_code: 'monitor_totalizer_snapshot',
    db_name: '进水监测室-一期二期累计流量合计（monitor_totalizer_snapshot）',
    point_name: '进水监测室-一期二期累计流量合计',
    unit: 'm³',
    raw_save_cycle_type: 'minute',
    flow_field: 'totalizer_sum',
    is_totalizer: true,
  },
]

const flowPhaseOptions = [
  { value: 'phase1', label: '一期' },
  { value: 'phase2', label: '二期' },
  { value: 'sum', label: '两期合计' },
]
const flowTypeOptions = [
  { value: 'instant', label: '瞬时流量' },
  { value: 'totalizer', label: '累计流量' },
]
const flowResolutionOptions = [
  { value: 'minute', label: '分钟数据' },
  { value: 'hourly', label: '小时平均' },
  { value: 'daily', label: '日平均' },
]
const selectedFlowPhases = ref(['phase1', 'phase2'])
const selectedFlowTypes = ref(['instant', 'totalizer'])
const flowResolution = ref('minute')

const FLOW_SERIES = [
  { phase: 'phase1', type: 'instant', field: 'phase1_instant', timeField: 'collect_time', name: '一期瞬时流量', unit: 'm³/h', color: '#00e8ff', yAxisIndex: 0 },
  { phase: 'phase2', type: 'instant', field: 'phase2_instant', timeField: 'collect_time', name: '二期瞬时流量', unit: 'm³/h', color: '#30e6ad', yAxisIndex: 0 },
  { phase: 'sum', type: 'instant', field: 'instant_sum', timeField: 'collect_time', name: '两期瞬时流量合计', unit: 'm³/h', color: '#ffbf55', yAxisIndex: 0 },
  { phase: 'phase1', type: 'totalizer', field: 'phase1_totalizer', timeField: 'phase1_totalizer_time', name: '一期累计流量', unit: 'm³', color: '#5aa7ff', yAxisIndex: 1 },
  { phase: 'phase2', type: 'totalizer', field: 'phase2_totalizer', timeField: 'phase2_totalizer_time', name: '二期累计流量', unit: 'm³', color: '#b78cff', yAxisIndex: 1 },
  { phase: 'sum', type: 'totalizer', field: 'totalizer_sum', timeField: 'totalizer_sum_time', name: '两期累计流量合计', unit: 'm³', color: '#ff6f7d', yAxisIndex: 1 },
]

const INSTANT_SERIES_NAMES = {
  minute: {
    phase1: '一期瞬时流量',
    phase2: '二期瞬时流量',
    sum: '两期瞬时流量合计',
  },
  hourly: {
    phase1: '一期小时平均瞬时流量',
    phase2: '二期小时平均瞬时流量',
    sum: '两期小时平均瞬时流量合计',
  },
  daily: {
    phase1: '一期日平均瞬时流量',
    phase2: '二期日平均瞬时流量',
    sum: '两期日平均瞬时流量合计',
  },
}

const activeFlowSeries = computed(() => FLOW_SERIES
  .filter(item => selectedFlowPhases.value.includes(item.phase) && selectedFlowTypes.value.includes(item.type))
  .map(item => item.type === 'instant'
    ? { ...item, name: INSTANT_SERIES_NAMES[flowResolution.value]?.[item.phase] || item.name }
    : item))
const flowResolutionLabel = computed(() => (
  flowResolutionOptions.find(item => item.value === flowResolution.value)?.label || '分钟数据'
))
const flowRangeLabel = computed(() => {
  if (!timeRange.value || timeRange.value.length !== 2) return `${flowResolutionLabel.value} · 未选择时间范围`
  return `${flowResolutionLabel.value} · ${timeRange.value[0]} 至 ${timeRange.value[1]}`
})

const POINT_NAME_ZH = {
  'Z-FIT01': '一期瞬时流量计',
  'Z-FIT02': '二期瞬时流量计',
  'Z-AIT01': '浊度',
  'Z-AIT02': 'PH',
  'Z-AIT03': '氨氮',
  'Z-AIT04': 'COD',
  'Z-AIT05': '总氮',
  'Z-AIT06': '总磷',
  'H1-AIT101': '1#混合液浓度仪',
  'H1-AIT102': '1#氧化还原电位',
  'H1-AIT103': '1#氨氮分析仪',
  'H1-AIT104': '1#硝氮分析仪',
  'H1-AIT105': '2#氧化还原电位',
  'H1-AIT106': '1#溶解氧',
  'H1-AIT107': '2#混合液浓度仪',
  'H1-AIT108': '2#溶解氧',
  'H1-AIT109': '2#氨氮分析仪',
  'H1-AIT110': '2#硝氮分析仪',
  'H1-AIT201': '3#混合液浓度仪',
  'H1-AIT202': '3#氧化还原电位',
  'H1-AIT203': '3#氨氮分析仪',
  'H1-AIT204': '3#硝氮分析仪',
  'H1-AIT205': '4#氧化还原电位',
  'H1-AIT206': '3#溶解氧',
  'H1-AIT207': '4#混合液浓度仪',
  'H1-AIT208': '4#溶解氧',
  'H1-AIT209': '4#氨氮分析仪',
  'H1-AIT210': '4#硝氮分析仪',
  'H1-LIT01': '1#污泥界面仪',
  'H1-LIT02': '2#污泥界面仪',
  'H1-LIT03': '3#污泥界面仪',
  'H1-LIT04': '4#污泥界面仪',
  'H1-AIT301': '污泥浓度计',
  'M3-RIT01': '污泥界面仪',
  'M3-AIT01': '出水浊度',
  'T3-RIT01': '污泥界面仪',
  'P2-AIT01': '余氯',
}

const getPointLocation = (code) => {
  if (code.startsWith('Z-')) return '进水监测室'
  if (code.startsWith('H1-LIT') || code === 'H1-AIT301') return '二沉池'
  if (code.startsWith('H1-')) return 'AAO生物池'
  if (code.startsWith('M3-')) return '高效沉淀池'
  if (code.startsWith('T3-')) return '污泥浓缩池'
  if (code.startsWith('P2-')) return '接触消毒池'
  return ''
}

const getPointDisplayName = (code, fallback) => {
  const name = POINT_NAME_ZH[code] || fallback
  const location = POINT_NAME_ZH[code] ? getPointLocation(code) : ''
  return location ? `${location}-${name}` : name
}

const headerStyle = {
  background: 'rgba(30, 41, 59, 0.95)',
  color: '#00e8ff',
  fontWeight: 'bold',
  textAlign: 'center',
  padding: '8px 0',
  fontSize: '16px'
}

const unwrapPayload = (res) => {
  const body = res?.data ?? res
  if (Array.isArray(body)) return body
  if (Array.isArray(body?.data)) return body.data
  if (Array.isArray(body?.rows)) return body.rows
  if (Array.isArray(body?.records)) return body.records
  if (Array.isArray(body?.list)) return body.list
  if (Array.isArray(body?.data?.rows)) return body.data.rows
  if (Array.isArray(body?.data?.records)) return body.data.records
  if (Array.isArray(body?.data?.list)) return body.data.list
  return []
}

const getField = (item, names, fallback = '') => {
  for (const name of names) {
    if (item?.[name] !== undefined && item?.[name] !== null && item?.[name] !== '') return item[name]
  }
  return fallback
}

const normalizeDatabases = (items) => {
  return items.map((item, index) => {
    const code = getField(item, ['point_code', 'pointCode', 'db_code', 'dbCode', 'code'], `point_${index + 1}`)
    const sourceName = getField(item, ['point_name', 'pointName', 'db_name', 'dbName', 'label', 'name'], code)
    const name = getPointDisplayName(code, sourceName)
    const unit = getField(item, ['unit'], '')
    const saveCycle = getField(item, ['raw_save_cycle_type', 'rawSaveCycleType'], 'hour')
    return { ...item, db_code: code, db_name: `${name}（${code}）`, point_name: name, unit, raw_save_cycle_type: saveCycle }
  })
}

const normalizeHistoryRows = (items, pointMeta = {}) => {
  return items.map((item, index) => {
    const deviceId = getField(item, ['deviceId', 'device_id', 'tagCode', 'tag_code', 'code', 'pointCode', 'point_code', 'metric_code'], pointMeta.db_code || `point_${index + 1}`)
    const sourceName = getField(item, ['deviceName', 'device_name', 'tagName', 'tag_name', 'pointName', 'point_name', 'metric_name', 'name'], pointMeta.point_name || deviceId)
    const deviceName = getPointDisplayName(deviceId, pointMeta.point_name || sourceName)
    const deviceType = getField(item, ['deviceType', 'device_type', 'type', 'category'], pointMeta.raw_save_cycle_type === 'minute' ? '分钟数据' : '小时数据')
    const unit = getField(item, ['unit', 'unitName', 'unit_name'], pointMeta.unit || '')
    const collectTime = getField(item, ['collectTime', 'collect_time', 'stat_hour', 'statHour', 'stat_date', 'statDate', 'time', 'timestamp', 'createdAt', 'created_at', 'createTime', 'create_time'])
    const tagValue = getField(item, ['tagValue', 'tag_value', 'value', 'val', 'dataValue', 'data_value', 'value_decimal', 'valueDecimal', 'avg_value', 'avgValue', 'total_value', 'totalValue'])
    return { ...item, deviceId, deviceName, deviceType, unit, collectTime, tagValue }
  }).filter(item => item.collectTime)
}

const isSuccessResponse = (res) => {
  const body = res?.data ?? res
  return body?.code === undefined || body.code === 200 || body.code === 0 || body.success === true
}

const getDatabases = async () => {
  try {
    const res = await request.get('/monitor/points')
    if (isSuccessResponse(res)) {
      const points = normalizeDatabases(unwrapPayload(res))
      const pointCodes = new Set(points.map(item => item.db_code))
      const totalizerPoints = TOTALIZER_POINTS.filter(item => !pointCodes.has(item.db_code))
      dbList.value = [
        { db_code: ALL_MONITOR_POINTS, db_name: '全部监测点', point_name: '全部监测点', unit: '', raw_save_cycle_type: 'mixed' },
        ...points,
        ...totalizerPoints,
      ]
      if (dbList.value.length > 0 && !query.dbCode) query.dbCode = dbList.value[0].db_code
    } else {
      ElMessage.error(res.data?.msg || res.data?.message || '获取监测点列表失败')
    }
  } catch (e) {
    console.error('获取监测点列表失败', e)
    dbList.value = []
    query.dbCode = ''
    ElMessage.error('监测点列表接口不可用，请检查后端 /api/monitor/points')
  }
}

const parseDateValue = (value) => {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const text = String(value).trim()
  const localMatch = text.match(/^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/)
  const date = localMatch
    ? new Date(
      Number(localMatch[1]),
      Number(localMatch[2]) - 1,
      Number(localMatch[3]),
      Number(localMatch[4] || 0),
      Number(localMatch[5] || 0),
      Number(localMatch[6] || 0),
    )
    : new Date(text)
  return Number.isNaN(date.getTime()) ? null : date
}

const isRowInRange = (row, start, end, resolution = 'minute') => {
  const time = parseDateValue(row.collectTime)
  const startDate = parseDateValue(start)
  const endDate = parseDateValue(end)
  if (!time || !startDate || !endDate) return false
  if (resolution === 'daily') {
    const dayKey = date => date.getFullYear() * 10000 + (date.getMonth() + 1) * 100 + date.getDate()
    return dayKey(time) >= dayKey(startDate) && dayKey(time) <= dayKey(endDate)
  }
  return time >= startDate && time <= endDate
}

const fetchPointHistory = async (point) => {
  const cycle = String(point.raw_save_cycle_type || '').toLowerCase()
  const resolution = query.interval >= 1440
    ? 'daily'
    : cycle === 'minute' ? 'minute' : cycle === 'day' || cycle === 'daily' ? 'daily' : 'hourly'
  const code = encodeURIComponent(point.db_code)
  const res = await request.get(`/monitor/points/${code}/range`, {
    params: {
      start: timeRange.value[0],
      end: timeRange.value[1],
      resolution,
      intervalMinutes: query.interval,
    },
  })
  if (!isSuccessResponse(res)) throw new Error(res.data?.msg || res.data?.message || `${point.db_code} 查询失败`)
  return normalizeHistoryRows(unwrapPayload(res), point)
    .filter(row => isRowInRange(row, timeRange.value[0], timeRange.value[1], resolution))
}

const normalizeFlowRows = (res) => {
  const body = res?.data ?? res
  const payload = body?.data && !Array.isArray(body.data) ? body.data : body
  const rows = Array.isArray(payload?.rows) ? payload.rows : []
  return rows.map(row => ({
    collect_time: getField(row, ['collect_time', 'collectTime', 'time']),
    phase1_instant: getField(row, ['phase1_instant', 'phase1Instant'], null),
    phase2_instant: getField(row, ['phase2_instant', 'phase2Instant'], null),
    instant_sum: getField(row, ['instant_sum', 'instantSum'], null),
    phase1_totalizer: getField(row, ['phase1_totalizer', 'phase1Totalizer'], null),
    phase2_totalizer: getField(row, ['phase2_totalizer', 'phase2Totalizer'], null),
    totalizer_sum: getField(row, ['totalizer_sum', 'totalizerSum'], null),
    phase1_totalizer_time: getField(row, ['phase1_totalizer_time', 'phase1TotalizerTime'], null),
    phase2_totalizer_time: getField(row, ['phase2_totalizer_time', 'phase2TotalizerTime'], null),
    totalizer_sum_time: getField(row, ['totalizer_sum_time', 'totalizerSumTime'], null),
  })).filter(row => row.collect_time)
}

const fetchFlowHistory = async ({ resolution = flowResolution.value, intervalMinutes = query.interval } = {}) => {
  const res = await request.get('/monitor/metrics/inflow/history', {
    params: {
      start: timeRange.value[0],
      end: timeRange.value[1],
      intervalMinutes,
      resolution,
    },
  })
  if (!isSuccessResponse(res)) throw new Error(res.data?.msg || res.data?.message || '进水流量历史查询失败')
  return normalizeFlowRows(res)
}

const normalizeTotalizerHistory = (point, rows) => rows
  .filter(row => row[point.flow_field] !== null && row[point.flow_field] !== undefined)
  .map(row => ({
    deviceId: point.db_code,
    deviceName: point.point_name,
    deviceType: '累计流量',
    unit: point.unit,
    collectTime: row.collect_time,
    tagValue: row[point.flow_field],
  }))

const alignTimeToInterval = (timeStr, interval) => {
  if (!timeStr) return ''
  const date = parseDateValue(timeStr)
  if (!date) return ''
  const pad = (n) => n.toString().padStart(2, '0')
  if (interval >= 1440) {
    return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} 00:00`
  }
  const ms = interval * 60 * 1000
  const aligned = new Date(Math.floor(date.getTime() / ms) * ms)
  return `${aligned.getFullYear()}-${pad(aligned.getMonth()+1)}-${pad(aligned.getDate())} ${pad(aligned.getHours())}:${pad(aligned.getMinutes())}`
}

const generateTimeColumns = (start, end, interval) => {
  const columns = []
  const startDate = parseDateValue(start)
  const endDate = parseDateValue(end)
  if (!startDate || !endDate) return []
  const pad = (n) => n.toString().padStart(2, '0')
  if (interval >= 1440) {
    const cursor = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())
    const lastDay = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate())
    while (cursor <= lastDay) {
      columns.push(`${cursor.getFullYear()}-${pad(cursor.getMonth()+1)}-${pad(cursor.getDate())} 00:00`)
      cursor.setDate(cursor.getDate() + 1)
    }
    return columns
  }
  const ms = interval * 60 * 1000
  const alignedStart = new Date(Math.ceil(startDate.getTime() / ms) * ms)
  for (let t = alignedStart; t <= endDate; t = new Date(t.getTime() + ms)) {
    columns.push(`${t.getFullYear()}-${pad(t.getMonth()+1)}-${pad(t.getDate())} ${pad(t.getHours())}:${pad(t.getMinutes())}`)
  }
  return columns
}

const mergeData = (data, times, interval) => {
  timeColumns.value = times
  if (!data || data.length === 0) { tableData.value = []; deviceColumns.value = []; return }
  const deviceMap = new Map()
  data.forEach(item => {
    const deviceId = item.deviceId
    if (!deviceId) return
    if (!deviceMap.has(deviceId)) {
      deviceMap.set(deviceId, { deviceId, deviceName: item.deviceName, deviceType: item.deviceType, unit: item.unit })
    }
  })
  deviceColumns.value = Array.from(deviceMap.values())
  const dataMap = new Map()
  data.forEach(item => {
    const timeKey = alignTimeToInterval(item.collectTime, interval)
    const deviceId = item.deviceId
    const value = item.tagValue
    if (!timeKey || !deviceId) return
    if (!dataMap.has(timeKey)) dataMap.set(timeKey, {})
    dataMap.get(timeKey)[deviceId] = value
  })
  tableData.value = times.map(time => {
    const row = { time }
    deviceColumns.value.forEach(device => {
      const val = dataMap.get(time)?.[device.deviceId]
      row[device.deviceId] = val !== undefined ? val : '-'
    })
    return row
  })
}

const handleQuery = async () => {
  if (!query.dbCode) { ElMessage.warning('请选择监测点'); return }
  if (!timeRange.value || timeRange.value.length !== 2) { ElMessage.warning('请选择时间范围'); return }
  const targetTimes = generateTimeColumns(timeRange.value[0], timeRange.value[1], query.interval)
  if (targetTimes.length > 10000) {
    ElMessage.warning('所选时间范围过大，请提高查询间隔或缩短时间范围')
    return
  }
  loading.value = true
  flowLoading.value = true
  try {
    const points = query.dbCode === ALL_MONITOR_POINTS
      ? dbList.value.filter(item => item.db_code !== ALL_MONITOR_POINTS)
      : dbList.value.filter(item => item.db_code === query.dbCode)
    const normalPoints = points.filter(item => !item.is_totalizer)
    const totalizerPoints = points.filter(item => item.is_totalizer)
    if (query.interval >= 1440) flowResolution.value = 'daily'
    const chartFlowPromise = fetchFlowHistory()
    const matrixFlowResolution = query.interval >= 1440 ? 'daily' : 'minute'
    const matrixFlowPromise = totalizerPoints.length === 0
      ? Promise.resolve([])
      : matrixFlowResolution === flowResolution.value
        ? chartFlowPromise
        : fetchFlowHistory({ resolution: matrixFlowResolution })
    const [normalResults, flowRows, matrixFlowRows] = await Promise.all([
      Promise.all(normalPoints.map(fetchPointHistory)),
      chartFlowPromise,
      matrixFlowPromise,
    ])
    flowHistoryRows.value = flowRows
    rawData.value = [
      ...normalResults.flat(),
      ...totalizerPoints.flatMap(point => normalizeTotalizerHistory(point, matrixFlowRows)),
    ]
    mergeData(rawData.value, targetTimes, query.interval)
    hasQueried.value = true
    await nextTick()
    renderFlowChart()
  } catch (e) {
    console.error('查询失败', e)
    const backendMessage = e.response?.data?.message || e.response?.data?.error
    ElMessage.error(backendMessage || '查询失败，请检查历史数据接口')
  } finally {
    loading.value = false
    flowLoading.value = false
  }
}

const handleReset = () => {
  query.dbCode = dbList.value.length > 0 ? dbList.value[0].db_code : ''
  query.interval = 5; timeRange.value = []
  tableData.value = []; deviceColumns.value = []; timeColumns.value = []; rawData.value = []
  flowHistoryRows.value = []
  selectedFlowPhases.value = ['phase1', 'phase2']
  selectedFlowTypes.value = ['instant', 'totalizer']
  flowResolution.value = 'minute'
  hasQueried.value = false
  renderFlowChart()
}

const formatValue = (val, device) => {
  if (val === '-' || val === null || val === undefined || val === '') return '-'
  const num = Number(val)
  if (!Number.isFinite(num)) return val
  return device?.deviceType === '累计流量' ? num.toFixed(3) : num.toFixed(1)
}

const getValueClass = (val, device) => {
  if (val === '-' || val === null || val === undefined || val === '') return 'text-empty'
  const num = Number(val)
  if (!Number.isFinite(num)) return 'text-normal'
  if (num === 0) return 'text-zero'
  if (device?.deviceType === '水泵' && num > 30) return 'text-danger'
  if (device?.deviceType === '风机' && num > 55) return 'text-danger'
  return 'text-normal'
}

const toChartValue = (value) => {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

const formatChartValue = (value) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return number.toLocaleString('zh-CN', { minimumFractionDigits: 3, maximumFractionDigits: 3 })
}

const formatFlowTime = (value, includeSeconds = false) => {
  const date = typeof value === 'number' ? new Date(value) : parseDateValue(value)
  if (!date || Number.isNaN(date.getTime())) return String(value || '')
  const pad = (number) => String(number).padStart(2, '0')
  const base = `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  return includeSeconds ? `${base}:${pad(date.getSeconds())}` : base
}

const buildFlowSeriesData = (series) => flowHistoryRows.value
  .map(row => {
    const value = toChartValue(row[series.field])
    const time = parseDateValue(row[series.timeField] || row.collect_time)
    return value === null || !time ? null : [time.getTime(), value]
  })
  .filter(Boolean)
  .sort((first, second) => first[0] - second[0])

const renderFlowChart = () => {
  if (activeView.value !== 'flow' || !flowChartRef.value) return
  const rect = flowChartRef.value.getBoundingClientRect()
  if (rect.width <= 0 || rect.height <= 0) return
  if (!flowChart) flowChart = echarts.init(flowChartRef.value)

  const selectedSeries = activeFlowSeries.value
  const hasInstant = selectedSeries.some(item => item.type === 'instant')
  const hasTotalizer = selectedSeries.some(item => item.type === 'totalizer')
  const unitByName = Object.fromEntries(selectedSeries.map(item => [item.name, item.unit]))

  flowChart.setOption({
    animationDuration: 420,
    color: selectedSeries.map(item => item.color),
    legend: {
      top: 8,
      left: 'center',
      data: selectedSeries.map(item => item.name),
      textStyle: { color: '#c9edf5', fontSize: 13 },
      itemWidth: 20,
      itemHeight: 8,
      itemGap: 18,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(5, 18, 32, 0.96)',
      borderColor: 'rgba(0, 232, 255, 0.38)',
      textStyle: { color: '#eafcff' },
      formatter: (params) => {
        const rows = Array.isArray(params) ? params : [params]
        if (rows.length === 0) return ''
        const lines = rows
          .filter(item => item.value !== null && item.value !== undefined)
          .map(item => {
            const value = Array.isArray(item.value) ? item.value[1] : item.value
            return `${item.marker}${item.seriesName}：${formatChartValue(value)} ${unitByName[item.seriesName] || ''}`
          })
        return [`<strong>${formatFlowTime(rows[0].axisValue, true)}</strong>`, ...lines].join('<br/>')
      },
    },
    grid: {
      top: 58,
      right: hasTotalizer ? 84 : 32,
      bottom: 76,
      left: hasInstant ? 72 : 32,
      containLabel: true,
    },
    xAxis: {
      type: 'time',
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(109, 190, 214, 0.38)' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#8bb9c8',
        fontSize: 12,
        hideOverlap: true,
        formatter: value => formatFlowTime(value).slice(5),
      },
    },
    yAxis: [
      {
        type: 'value',
        name: '瞬时流量 (m³/h)',
        show: hasInstant,
        scale: true,
        nameTextStyle: { color: '#7ddff0', fontSize: 12, padding: [0, 0, 8, 0] },
        axisLabel: { color: '#8bb9c8', formatter: value => Number(value).toLocaleString('zh-CN') },
        axisLine: { show: true, lineStyle: { color: 'rgba(0, 232, 255, 0.35)' } },
        splitLine: { lineStyle: { color: 'rgba(106, 181, 202, 0.10)' } },
      },
      {
        type: 'value',
        name: '累计流量 (m³)',
        show: hasTotalizer,
        scale: true,
        nameTextStyle: { color: '#ffc0c6', fontSize: 12, padding: [0, 0, 8, 0] },
        axisLabel: { color: '#c8aeb8', formatter: value => Number(value).toLocaleString('zh-CN') },
        axisLine: { show: true, lineStyle: { color: 'rgba(255, 111, 125, 0.38)' } },
        splitLine: { show: false },
      },
    ],
    dataZoom: [
      { type: 'inside', start: 0, end: 100, filterMode: 'none' },
      {
        type: 'slider',
        height: 20,
        bottom: 16,
        borderColor: 'rgba(0, 232, 255, 0.16)',
        backgroundColor: 'rgba(5, 24, 40, 0.5)',
        fillerColor: 'rgba(0, 232, 255, 0.12)',
        handleStyle: { color: '#00e8ff', borderColor: '#8ff5ff' },
        textStyle: { color: '#7aa9b8' },
      },
    ],
    series: selectedSeries.map(item => ({
      name: item.name,
      type: 'line',
      yAxisIndex: item.yAxisIndex,
      data: buildFlowSeriesData(item),
      showSymbol: false,
      connectNulls: false,
      smooth: 0.18,
      sampling: 'lttb',
      lineStyle: {
        width: item.type === 'totalizer' ? 2.6 : 2.2,
        type: item.type === 'totalizer' ? 'solid' : 'dashed',
      },
      emphasis: { focus: 'series', lineStyle: { width: 3.5 } },
    })),
  }, true)
}

const switchView = async (view) => {
  activeView.value = view
  if (view === 'flow') {
    await nextTick()
    renderFlowChart()
    flowChart?.resize()
  }
}

const handlePhaseSelectionChange = (values) => {
  if (values.length > 0) return
  selectedFlowPhases.value = ['phase1']
  ElMessage.warning('至少保留一个流量范围')
}

const handleTypeSelectionChange = (values) => {
  if (values.length > 0) return
  selectedFlowTypes.value = ['instant']
  ElMessage.warning('至少保留一种流量类型')
}

const handleFlowResolutionChange = async () => {
  if (!hasQueried.value || !timeRange.value || timeRange.value.length !== 2) {
    await nextTick()
    renderFlowChart()
    return
  }
  flowLoading.value = true
  try {
    flowHistoryRows.value = await fetchFlowHistory()
    await nextTick()
    renderFlowChart()
  } catch (e) {
    console.error('进水流量粒度切换失败', e)
    const backendMessage = e.response?.data?.message || e.response?.data?.error
    ElMessage.error(backendMessage || '进水流量查询失败')
  } finally {
    flowLoading.value = false
  }
}

const handleWindowResize = () => flowChart?.resize()

const formatDateTime = (date) => {
  const pad = (n) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const handleExportExcel = () => {
  if (activeView.value === 'flow') {
    if (!flowHistoryRows.value.length) { ElMessage.warning('当前无流量数据，请先查询'); return }
    const series = activeFlowSeries.value
    const header = ['采集时间', ...series.map(item => `${item.name}(${item.unit})`)]
    const rows = flowHistoryRows.value.map(row => [
      row.collect_time,
      ...series.map(item => {
        const value = row[item.field]
        return value !== undefined && value !== null ? Number(value) : ''
      }),
    ])
    const worksheet = XLSX.utils.aoa_to_sheet([header, ...rows])
    worksheet['!cols'] = [{ wch: 20 }, ...series.map(() => ({ wch: 20 }))]
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '进水流量分析')
    XLSX.writeFile(workbook, `进水流量分析_${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.xlsx`)
    ElMessage.success('Excel 导出成功')
    return
  }
  if (!tableData.value?.length) { ElMessage.warning('当前无数据，请先查询'); return }
  const header = ['采集时间', ...deviceColumns.value.map(d => `${d.deviceName}(${d.unit})`)]
  const rows = tableData.value.map(row => [row.time, ...deviceColumns.value.map(d => {
    const val = row[d.deviceId]
    return val !== undefined && val !== null && val !== '-' ? val : ''
  })])
  const worksheet = XLSX.utils.aoa_to_sheet([header, ...rows])
  worksheet['!cols'] = [{ wch: 20 }, ...deviceColumns.value.map(() => ({ wch: 16 }))]
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '历史数据')
  const dbName = dbList.value.find(d => d.db_code === query.dbCode)?.db_name || query.dbCode
  XLSX.writeFile(workbook, `历史数据_${dbName}_${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.xlsx`)
  ElMessage.success('Excel 导出成功')
}

let isInitialized = false
watch([selectedFlowPhases, selectedFlowTypes, flowResolution, flowHistoryRows], () => {
  nextTick(renderFlowChart)
}, { deep: true })

onMounted(() => {
  if (isInitialized) return; isInitialized = true
  window.addEventListener('resize', handleWindowResize)
  getDatabases().then(() => {
    const end = new Date()
    const start = new Date(end.getTime() - 60 * 60 * 1000)
    timeRange.value = [formatDateTime(start), formatDateTime(end)]
    if (query.dbCode) handleQuery()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWindowResize)
  if (flowChart) {
    flowChart.dispose()
    flowChart = null
  }
})
</script>

<style scoped>
.history-page {
  height: calc(100vh - var(--nav-height, 106px));
  height: calc(100dvh - var(--nav-height, 106px));
  min-height: 0;
  padding: 16px 16px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #F2F3F5;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(0, 232, 255, 0.03) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(0, 232, 255, 0.03) 0%, transparent 50%),
    linear-gradient(180deg, var(--color-bg-deep) 0%, var(--color-bg-main) 100%);
  position: relative;
}

.wave-overlay {
  position: absolute; inset: 0;
  background: repeating-linear-gradient(0deg, transparent, transparent 3px, rgba(0, 232, 255, 0.006) 3px, rgba(0, 232, 255, 0.006) 4px);
  pointer-events: none; z-index: 0;
}

/* ========== Cards ========== */
.query-card, .table-card, .flow-card {
  position: relative;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  overflow: visible;
  z-index: 1;
}

.query-card,
.view-tabs {
  flex-shrink: 0;
}

.table-card {
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.view-tabs {
  position: relative;
  z-index: 2;
  display: inline-flex;
  gap: 2px;
  margin: 0 0 12px;
  padding: 3px;
  border: 1px solid rgba(0, 232, 255, 0.16);
  border-radius: 6px;
  background: rgba(7, 24, 40, 0.76);
}

.view-tab {
  min-width: 132px;
  height: 34px;
  padding: 0 18px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #8eb6c5;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.view-tab:hover { color: #dffaff; background: rgba(0, 232, 255, 0.06); }
.view-tab.active {
  color: #eaffff;
  background: rgba(0, 232, 255, 0.14);
  box-shadow: inset 0 0 0 1px rgba(0, 232, 255, 0.26), 0 0 12px rgba(0, 232, 255, 0.08);
}

.flow-card {
  min-height: 560px;
  overflow: hidden;
}

.flow-toolbar {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  min-height: 62px;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.10);
  background: rgba(4, 20, 34, 0.42);
}

.flow-filter-group { display: flex; align-items: center; gap: 10px; }
.flow-filter-label { color: #85cddd; font-size: 0.875rem; font-weight: 700; white-space: nowrap; }
.flow-sample-meta { display: flex; align-items: baseline; gap: 6px; margin-left: auto; color: #7fa9b8; white-space: nowrap; }
.flow-sample-meta strong { color: #00e8ff; font-family: var(--font-data); font-size: 1.25rem; }
.flow-sample-meta span { font-size: 0.8125rem; }
.flow-chart-wrap { position: relative; min-height: 430px; }
.flow-chart { width: 100%; height: calc(100vh - 390px); min-height: 430px; max-height: 600px; }
.flow-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(168, 211, 224, 0.58);
  font-size: 1rem;
  pointer-events: none;
}

/* Frame corners */
.frame-corner { position: absolute; width: 12px; height: 12px; z-index: 5; pointer-events: none; }
.frame-corner::before, .frame-corner::after {
  content: ''; position: absolute; background: var(--color-primary); border-radius: 1px;
  box-shadow: 0 0 6px var(--color-primary);
}
.frame-corner.tl { top: 0; left: 0; }
.frame-corner.tl::before { top: 0; left: 0; width: 12px; height: 2px; }
.frame-corner.tl::after { top: 0; left: 0; width: 2px; height: 12px; }
.frame-corner.tr { top: 0; right: 0; }
.frame-corner.tr::before { top: 0; right: 0; width: 12px; height: 2px; }
.frame-corner.tr::after { top: 0; right: 0; width: 2px; height: 12px; }
.frame-corner.bl { bottom: 0; left: 0; }
.frame-corner.bl::before { bottom: 0; left: 0; width: 12px; height: 2px; }
.frame-corner.bl::after { bottom: 0; left: 0; width: 2px; height: 12px; }
.frame-corner.br { bottom: 0; right: 0; }
.frame-corner.br::before { bottom: 0; right: 0; width: 12px; height: 2px; }
.frame-corner.br::after { bottom: 0; right: 0; width: 2px; height: 12px; }

/* Header */
.card-header-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.1);
  position: relative; z-index: 3;
}

.header-pulse {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--color-primary); box-shadow: 0 0 6px var(--color-primary);
  animation: pulse-glow 2s ease-in-out infinite;
}
.header-pulse.success { background: var(--color-success); box-shadow: 0 0 6px var(--color-success); }

.header-text {
  font-size: 1.0625rem; font-weight: 600; color: rgba(0, 232, 255, 0.85);
  letter-spacing: 1px; flex: 1;
}

.header-meta {
  font-family: var(--font-data); font-size: 0.9375rem;
  color: rgba(0, 232, 255, 0.45);
}

.header-warn {
  font-size: 0.9375rem; color: var(--color-danger);
  background: rgba(255, 23, 68, 0.08);
  border: 1px solid rgba(255, 23, 68, 0.2);
  padding: 1px 8px; border-radius: var(--radius-sm);
}

.card-body-inner {
  padding: 14px;
  position: relative; z-index: 3;
}

.table-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 0;
  overflow: hidden;
}

.table-body :deep(.el-table) {
  flex: 1;
  min-height: 0;
}

/* ========== Element Plus Overrides ========== */
:deep(.flow-toolbar .el-checkbox-button__inner),
:deep(.flow-toolbar .el-radio-button__inner) {
  min-width: 70px;
  padding: 7px 12px;
  border-color: rgba(0, 232, 255, 0.18) !important;
  background: rgba(5, 24, 40, 0.68) !important;
  color: #8fb8c6 !important;
  box-shadow: none !important;
  font-size: 0.8125rem;
  font-weight: 700;
}

:deep(.flow-toolbar .el-checkbox-button.is-checked .el-checkbox-button__inner),
:deep(.flow-toolbar .el-radio-button.is-active .el-radio-button__inner) {
  border-color: rgba(0, 232, 255, 0.58) !important;
  background: rgba(0, 188, 218, 0.22) !important;
  color: #ecfeff !important;
  box-shadow: -1px 0 0 0 rgba(0, 232, 255, 0.34) !important;
}

:deep(.flow-toolbar .el-checkbox-button__inner:hover),
:deep(.flow-toolbar .el-radio-button__inner:hover) { color: #fff !important; }

:deep(.el-form-item__label) {
  color: rgba(0, 232, 255, 0.65) !important;
  font-size: 1rem;
}

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-date-editor .el-input__wrapper) {
  background: rgba(0, 15, 30, 0.5) !important;
  box-shadow: 0 0 0 1px rgba(0, 232, 255, 0.15) inset !important;
  border-radius: var(--radius-sm) !important;
}

:deep(.el-input__inner) {
  color: #F2F3F5 !important;
  font-family: var(--font-data);
}

:deep(.el-input__wrapper:hover),
:deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 232, 255, 0.4) inset !important;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary)) !important;
  border-color: var(--color-primary) !important;
  color: #000 !important;
  font-weight: bold;
  box-shadow: 0 0 10px rgba(0, 232, 255, 0.2) !important;
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, var(--color-success), #00c853) !important;
  border-color: var(--color-success) !important;
  color: #fff !important;
  box-shadow: 0 0 8px rgba(0, 230, 118, 0.2) !important;
}

:deep(.el-button:not(.el-button--primary):not(.el-button--success)) {
  background: rgba(0, 232, 255, 0.06) !important;
  border: 1px solid rgba(0, 232, 255, 0.15) !important;
  color: rgba(0, 232, 255, 0.7) !important;
}

:deep(.el-button:not(.el-button--primary):not(.el-button--success):hover) {
  background: rgba(0, 232, 255, 0.12) !important;
  color: #fff !important;
}

/* ========== Table ========== */
:deep(.el-table) {
  background: transparent !important;
  --el-table-border-color: rgba(0, 232, 255, 0.1) !important;
  --el-table-row-hover-bg-color: rgba(0, 232, 255, 0.05) !important;
}

:deep(.el-table__header-wrapper th.el-table__cell) {
  background: rgba(30, 41, 59, 0.95) !important;
  color: var(--color-primary) !important;
  font-weight: bold;
  border-bottom: 1px solid rgba(0, 232, 255, 0.25) !important;
  text-shadow: 0 0 5px rgba(0, 232, 255, 0.3);
}

:deep(.el-table__body-wrapper td.el-table__cell) {
  background: rgba(15, 23, 42, 0.4) !important;
  color: #F2F3F5 !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03) !important;
  transition: all 0.2s ease;
}

:deep(.el-table__body-wrapper tr:hover td.el-table__cell) {
  background: rgba(0, 232, 255, 0.06) !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(0, 15, 30, 0.25) !important;
}

:deep(.el-table--border .el-table__cell) {
  border-right: 1px solid rgba(0, 232, 255, 0.06) !important;
}

/* ========== Data Styles ========== */
.time-label {
  font-family: var(--font-data); font-size: 1rem;
  color: var(--color-primary); text-shadow: 0 0 4px rgba(0, 232, 255, 0.3);
}

.device-header { line-height: 1.4; }
.dh-name { font-size: 1rem; font-weight: bold; color: #fff; }
.dh-meta { font-size: 0.875rem; color: rgba(0, 232, 255, 0.5); }
.dh-unit { color: var(--color-warning); text-shadow: 0 0 3px rgba(255, 171, 0, 0.2); }

.text-empty { color: #444; }
.text-zero { color: #666; }
.text-normal { color: #d0dae8; }
.text-danger { color: var(--color-danger); font-weight: bold; text-shadow: 0 0 6px rgba(255, 23, 68, 0.4); }

/* ========== Loading ========== */
:deep(.el-loading-mask) {
  background: rgba(15, 23, 42, 0.8) !important;
  backdrop-filter: none;
}

/* Scrollbar */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: rgba(0, 15, 30, 0.3); }
::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--color-primary), var(--color-secondary));
  border-radius: 2px;
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 4px var(--color-primary); opacity: 0.7; }
  50% { box-shadow: 0 0 10px var(--color-primary), 0 0 20px rgba(0, 232, 255, 0.3); opacity: 1; }
}
</style>
