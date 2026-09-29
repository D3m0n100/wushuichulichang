<template>
  <div class="dashboard-container">
    <div class="scene-background">
      <img src="/优化排版.png" class="scene-bg-image" alt="厂区总图" />
      <div class="scene-bg-shade"></div>
    </div>
    <!-- Ambient wave overlay -->
    <div class="wave-overlay"></div>

    <!-- Left Panel -->
    <div class="left-panel">
      <!-- Card 1: Water Quality -->
      <Transition name="card-emerge" appear>
        <div v-if="cardVisible[0]" class="card-item">
          <div class="card-frame">
            <div class="frame-corner tl"></div>
            <div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div>
            <div class="frame-corner br"></div>
            <div class="frame-glow"></div>

            <div class="card-header-bar">
              <span class="header-pulse"></span>
              <span class="header-text">水质实时数据</span>
              <span class="header-badge">LIVE</span>
            </div>
            <div class="card-content trend-content">
              <div class="table-scroll">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>指标</th>
                      <th>地点</th>
                      <th>数值</th>
                      <th>状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in waterTableData" :key="row.label">
                      <td class="metric-name">{{ row.label }}</td>
                      <td class="metric-location">{{ row.location }}</td>
                      <td class="metric-value">{{ row.value }}</td>
                      <td>
                        <span class="status-dot" :class="row.status"></span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Card 2: Trend Chart -->
      <Transition name="card-emerge" appear>
        <div v-if="cardVisible[1]" class="card-item">
          <div class="card-frame">
            <div class="frame-corner tl"></div>
            <div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div>
            <div class="frame-corner br"></div>
            <div class="frame-glow"></div>

            <div class="card-header-bar">
              <span class="header-pulse"></span>
              <span class="header-text">水质趋势分析</span>
            </div>
            <div class="card-content">
              <div class="metric-pills">
                <button
                  v-for="m in metrics"
                  :key="m.key"
                  class="pill-btn"
                  :class="{ active: activeMetric === m.key }"
                  @click="switchMetric(m.key)"
                >{{ m.label }}</button>
              </div>
              <div id="trendChart" class="chart-area"></div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Card 6: Alarms (moved from right) -->
      <Transition name="card-emerge" appear>
        <div v-if="cardVisible[5]" class="card-item">
          <div class="card-frame">
            <div class="frame-corner tl"></div>
            <div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div>
            <div class="frame-corner br"></div>
            <div class="frame-glow"></div>

            <div class="card-header-bar">
              <span class="header-pulse danger"></span>
              <span class="header-text">设备报警</span>
              <span class="header-count danger">{{ activeFaultCount }}</span>
            </div>
            <div class="card-content alarm-content">
              <div class="alarm-stats">
                <div class="stat-chip danger">
                  <span class="chip-num">{{ activeFaultCount }}</span>
                  <span class="chip-label">故障中</span>
                </div>
              </div>

              <div class="alarm-scroll">
                <div class="scroll-track" :class="{ scrolling: sortedAlarms.length > 4 }">
                  <table class="alarm-table">
                    <thead>
                      <tr>
                        <th>设备</th>
                        <th>地点</th>
                        <th>时间</th>
                        <th>状态</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="sortedAlarms.length === 0">
                        <td colspan="4" class="alarm-empty">当前无活动设备故障</td>
                      </tr>
                      <tr v-for="item in sortedAlarms" :key="'a-'+item.alarmId">
                        <td class="alarm-device">{{ item.deviceName }}</td>
                        <td class="alarm-location">{{ item.deviceLocation }}</td>
                        <td class="time-cell">{{ item.createTime }}</td>
                        <td><span class="alarm-status">{{ item.status }}</span></td>
                      </tr>
                    </tbody>
                  </table>
                  <table class="alarm-table clone" v-if="sortedAlarms.length > 4">
                    <tbody>
                      <tr v-for="item in sortedAlarms" :key="'b-'+item.alarmId">
                        <td class="alarm-device">{{ item.deviceName }}</td>
                        <td class="alarm-location">{{ item.deviceLocation }}</td>
                        <td class="time-cell">{{ item.createTime }}</td>
                        <td><span class="alarm-status">{{ item.status }}</span></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- Center: Factory Map -->
    <div class="center-panel">
      <div class="map-frame">
        <div class="frame-corner tl"></div>
        <div class="frame-corner tr"></div>
        <div class="frame-corner bl"></div>
        <div class="frame-corner br"></div>
        <div class="map-atmosphere"></div>
        <div class="map-grid"></div>
        <div class="map-vignette"></div>

        <div class="map-header-band">
          <div class="map-title-stack">
            <div class="map-eyebrow">厂区数字指挥中心</div>
            <div class="map-title-main">厂区工艺总览</div>
            <div class="map-title-sub">全厂工艺、告警、监测与设备状态联动总览</div>
          </div>
        </div>

        <!-- Draggable Navigation Dots -->
        <div class="nav-dots-layer">
          <div
            v-for="dot in navDots"
            :key="dot.id"
            class="nav-dot"
            :class="{ locked: isDotsLocked, dragging: draggingDotId === dot.id }"
            :style="{ left: dot.x + 'px', top: dot.y + 'px' }"
            @mousedown="startDrag($event, dot)"
            @click="handleDotClick(dot)"
            @mouseenter="hoveredDotId = dot.id"
            @mouseleave="hoveredDotId = null"
          >
            <div class="dot-core"></div>
            <div class="dot-ring"></div>
            <Transition name="tooltip-fade">
              <div v-if="hoveredDotId === dot.id" class="dot-tooltip">
                {{ dot.name }}
              </div>
            </Transition>
          </div>
        </div>

        <!-- Lock/Unlock Controls -->
        <div class="dot-controls">
          <button
            class="control-btn unlock-btn"
            :class="{ active: !isDotsLocked }"
            title="显示并解锁"
            aria-label="显示并解锁"
            @click="isDotsLocked = false"
          >
            <span class="btn-icon">🔓</span>
          </button>
          <button
            class="control-btn lock-btn"
            :class="{ active: isDotsLocked }"
            title="隐藏并锁定"
            aria-label="隐藏并锁定"
            @click="isDotsLocked = true"
          >
            <span class="btn-icon">🔒</span>
          </button>
        </div>

        <div class="map-command-dock">
          <div class="dock-shortcuts">
            <div v-for="(row, rowIndex) in quickLinkRows" :key="rowIndex" class="dock-shortcut-row">
              <button
                v-for="link in row"
                :key="link.route"
                class="dock-link"
                @click="goToPlc(link.route)"
              >
                <span class="dock-link-name">{{ link.name }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Panel -->
    <div class="right-panel">
      <!-- Card 4: Load Gauge -->
      <Transition name="card-emerge" appear>
        <div v-if="cardVisible[3]" class="card-item">
          <div class="card-frame">
            <div class="frame-corner tl"></div>
            <div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div>
            <div class="frame-corner br"></div>
            <div class="frame-glow"></div>

            <div class="card-header-bar">
              <span class="header-pulse"></span>
              <span class="header-text">一期进水流量</span>
            </div>
            <div class="card-content gauge-content">
              <FlowGauge :value="waterQuality.phase1Flow" unit="m³/h" :max="500" />
            </div>
          </div>
        </div>
      </Transition>

      <!-- Card 5: 二期进水流量 -->
      <Transition name="card-emerge" appear>
        <div v-if="cardVisible[4]" class="card-item">
          <div class="card-frame">
            <div class="frame-corner tl"></div>
            <div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div>
            <div class="frame-corner br"></div>
            <div class="frame-glow"></div>

            <div class="card-header-bar">
              <span class="header-pulse"></span>
              <span class="header-text">二期进水流量</span>
            </div>
            <div class="card-content gauge-content">
              <FlowGauge :value="waterQuality.phase2Flow" unit="m³/h" :max="500" />
            </div>
          </div>
        </div>
      </Transition>

      <!-- Card 6: 总进水流量 -->
      <Transition name="card-emerge" appear>
        <div v-if="cardVisible[5]" class="card-item">
          <div class="card-frame">
            <div class="frame-corner tl"></div>
            <div class="frame-corner tr"></div>
            <div class="frame-corner bl"></div>
            <div class="frame-corner br"></div>
            <div class="frame-glow"></div>

            <div class="card-header-bar">
              <span class="header-pulse"></span>
              <span class="header-text">总进水流量</span>
            </div>
            <div class="card-content total-flow-content">
              <div class="total-flow-wave" role="img" :aria-label="`总进水累计流量 ${formattedTotalFlow} 立方米`">
                <div class="total-flow-water"></div>
                <div class="total-flow-reading">
                  <span class="total-flow-label">累计总量</span>
                  <strong
                    class="total-flow-value"
                    :class="{
                      compact: (formattedTotalFlow || '').length > 10,
                      'extra-compact': (formattedTotalFlow || '').length > 14,
                    }"
                  >{{ formattedTotalFlow }}</strong>
                  <span class="total-flow-unit">m³</span>
                </div>
              </div>
              <div class="total-flow-source">一期累计 + 二期累计</div>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import request from '@/api/request'
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'
import FlowGauge from '@/components/charts/FlowGauge.vue'
import { useRouter } from 'vue-router'
import { readPlcRealtimeValue } from '@/utils/plcInstruments'

const router = useRouter()

// Card stagger animation
const cardVisible = ref([false, false, false, false, false, false])

// Navigation Dots State
const isDotsLocked = ref(true)
const hoveredDotId = ref(null)
const draggingDotId = ref(null)
let dragOffsetX = 0
let dragOffsetY = 0

const navDots = ref([
  { id: 'b1', name: '粗格栅、调节池及提升泵房', route: '/b1', x: 120, y: 200 },
  { id: 'x1', name: '细格栅及旋流沉砂池', route: '/x1', x: 380, y: 180 },
  { id: 'h1', name: 'AAO生物池', route: '/h1', x: 500, y: 280 },
  { id: 'm3', name: '高效沉淀池', route: '/m3', x: 620, y: 180 },
  { id: 'p2', name: '消毒池', route: '/p2', x: 740, y: 280 },
  { id: 't3', name: '污泥浓缩池', route: '/t3', x: 860, y: 180 },
  { id: 'g1', name: '鼓风机房', route: '/g1', x: 120, y: 350 },
  { id: 'j1', name: 'PAC及乙酸钠加药间', route: '/j1', x: 250, y: 400 },
  { id: 's1', name: '生物除臭系统', route: '/s1', x: 380, y: 350 },
  { id: 'd1', name: '脱水车间', route: '/d1', x: 620, y: 400 },
  { id: 'p3', name: '巴氏计量槽', route: '/p3', x: 860, y: 350 },
  { id: 'l1', name: '滤布滤池', route: '/l1', x: 740, y: 400 }
])

const startDrag = (event, dot) => {
  if (isDotsLocked.value) return
  draggingDotId.value = dot.id
  const rect = event.currentTarget.parentElement.getBoundingClientRect()
  dragOffsetX = event.clientX - rect.left - dot.x
  dragOffsetY = event.clientY - rect.top - dot.y

  const onMove = (e) => {
    if (!draggingDotId.value) return
    const newX = e.clientX - rect.left - dragOffsetX
    const newY = e.clientY - rect.top - dragOffsetY
    const targetDot = navDots.value.find(d => d.id === draggingDotId.value)
    if (targetDot) {
      targetDot.x = Math.max(0, Math.min(newX, rect.width - 20))
      targetDot.y = Math.max(0, Math.min(newY, rect.height - 20))
    }
  }

  const onUp = () => {
    draggingDotId.value = null
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

const handleDotClick = (dot) => {
  if (draggingDotId.value || !isDotsLocked.value) return
  router.push(dot.route)
}

const goToPlc = (route) => router.push(route)

const quickLinkRows = [
  [
    { name: '粗格栅、调节池及提升泵房', route: '/b1' },
    { name: '细格栅及旋流沉砂池', route: '/x1' },
    { name: 'AAO生物池', route: '/h1' },
    { name: '高效沉淀池', route: '/m3' },
    { name: '消毒池', route: '/p2' },
    { name: '污泥浓缩池', route: '/t3' },
    { name: '滤布滤池', route: '/l1' },
    { name: '鼓风机房', route: '/g1' }
  ],
  [
    { name: 'PAC及乙酸钠加药间', route: '/j1' },
    { name: 'PAM及次氯酸钠加药间', route: '/j2' },
    { name: '生物除臭系统', route: '/s1' },
    { name: '脱水车间', route: '/d1' },
    { name: '巴氏计量槽', route: '/p3' }
  ]
]

// Water quality
const waterQuality = reactive({
  inlet: { ss: 0, ph: 0, nh3n: 0, cod: 0, tn: 0, tp: 0, turbidity: 0 },
  phase1Flow: 0,
  phase2Flow: 0,
  phase1Totalizer: 0,
  phase2Totalizer: 0,
})

const totalFlow = computed(() => waterQuality.phase1Totalizer + waterQuality.phase2Totalizer)
const formattedTotalFlow = computed(() => {
  const value = Number(totalFlow.value)
  if (!Number.isFinite(value)) return '0.00'
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
})

const formatWaterQualityValue = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(3) : '0.000'
}

const waterTableData = computed(() => [
  { label: 'COD', location: '进水口', value: `${formatWaterQualityValue(waterQuality.inlet.cod)} mg/L`, status: 'ok' },
  { label: '氨氮', location: '进水口', value: `${formatWaterQualityValue(waterQuality.inlet.nh3n)} mg/L`, status: 'ok' },
  { label: '总磷', location: '进水口', value: `${formatWaterQualityValue(waterQuality.inlet.tp)} mg/L`, status: 'ok' },
  { label: '总氮', location: '进水口', value: `${formatWaterQualityValue(waterQuality.inlet.tn)} mg/L`, status: 'ok' },
  { label: 'PH', location: '进水口', value: formatWaterQualityValue(waterQuality.inlet.ph), status: 'ok' },
  { label: '浊度', location: '进水口', value: `${formatWaterQualityValue(waterQuality.inlet.turbidity)} NTU`, status: 'ok' },
])

const applyDashboardWaterValues = (values) => {
  const assign = (code, setter) => {
    const value = Number(values[code])
    if (values[code] !== undefined && Number.isFinite(value)) setter(value)
  }
  assign('B1_PHASE1_FLOWMETER', value => { waterQuality.phase1Flow = value })
  assign('B1_PHASE2_FLOWMETER', value => { waterQuality.phase2Flow = value })
  assign('B1_INFLOW_COD', value => { waterQuality.inlet.cod = value })
  assign('B1_INFLOW_NH3N', value => { waterQuality.inlet.nh3n = value })
  assign('B1_INFLOW_TP', value => { waterQuality.inlet.tp = value })
  assign('B1_INFLOW_TN', value => { waterQuality.inlet.tn = value })
  assign('B1_INFLOW_PH', value => { waterQuality.inlet.ph = value })
  assign('B1_INFLOW_TURBIDITY', value => { waterQuality.inlet.turbidity = value })
  assign('B1_PHASE1_FLOWMETER_TOTALIZER', value => { waterQuality.phase1Totalizer = value })
  assign('B1_PHASE2_FLOWMETER_TOTALIZER', value => { waterQuality.phase2Totalizer = value })
}

const DASHBOARD_INSTRUMENT_CODES = [
  'B1_PHASE1_FLOWMETER',
  'B1_PHASE2_FLOWMETER',
  'B1_INFLOW_COD',
  'B1_INFLOW_NH3N',
  'B1_INFLOW_TP',
  'B1_INFLOW_TN',
  'B1_INFLOW_PH',
  'B1_INFLOW_TURBIDITY',
  'B1_PHASE1_FLOWMETER_TOTALIZER',
  'B1_PHASE2_FLOWMETER_TOTALIZER',
]

const fetchDashboardWaterData = async () => {
  try {
    const res = await request.get('/plc/b1/overview')
    if (res.data?.success) {
      const overview = res.data.data
      const realtimeValues = {}
      DASHBOARD_INSTRUMENT_CODES.forEach(code => {
        const output = readPlcRealtimeValue(overview, code)
        if (output !== undefined && output !== null) realtimeValues[code] = output
      })
      applyDashboardWaterValues(realtimeValues)
    }
  } catch (e) { console.error('获取首页水质和流量数据失败', e) }
}

const unwrapRows = (response) => {
  const body = response?.data ?? response
  if (Array.isArray(body)) return body
  if (Array.isArray(body?.data)) return body.data
  if (Array.isArray(body?.data?.records)) return body.data.records
  if (Array.isArray(body?.records)) return body.records
  return []
}

const parseDateTime = (value) => {
  if (value === null || value === undefined || value === '') return null
  if (Array.isArray(value)) {
    const date = new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0)
    return Number.isNaN(date.getTime()) ? null : date
  }
  const date = typeof value === 'number' ? new Date(value) : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDateTime = (value) => {
  const date = parseDateTime(value)
  if (!date) return '-'
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// Current faults from the same database table used by the alarm page.
const PLC_LOCATIONS = {
  'coarse-screen': '粗格栅',
  'fine-screen': '细格栅',
  'multilayer-cyclone': '旋流沉砂池',
  pac: 'PAC加药间',
  pam: 'PAM加药间',
  'sodium-acetate': '乙酸钠加药间',
  'sodium-hypochlorite': '次氯酸钠加药间',
  'baike-dosing': '百科加药系统',
  'baike-filter-press': '脱水车间',
  'baike-fiber-disc': '滤布滤池',
  fan: '鼓风机房',
  'sludge-silo': '污泥料仓',
  deodorization: '生物除臭系统',
  'rotor-pump': '转子泵系统',
  b1: '调节池',
  h1: 'AAO生物池',
  m3: '高效沉淀池',
}

const currentAlarms = ref([])
const activeFaultCount = computed(() => currentAlarms.value.length)
const sortedAlarms = computed(() => [...currentAlarms.value].sort((a, b) => b.sortTime - a.sortTime))

const normalizeCurrentAlarm = (row) => {
  const deviceCode = row.device_code || row.deviceCode || '-'
  const plcCode = String(row.plc_code || row.plcCode || '').toLowerCase()
  const rawChangeTime = row.last_change_time || row.lastChangeTime
  return {
    alarmId: row.id || `${plcCode}-${deviceCode}-${rawChangeTime || ''}`,
    deviceCode,
    deviceName: row.device_name || row.deviceName || deviceCode || '未知设备',
    deviceLocation: PLC_LOCATIONS[plcCode] || row.plc_code || row.plcCode || '未知区域',
    createTime: formatDateTime(rawChangeTime),
    sortTime: parseDateTime(rawChangeTime)?.getTime() || 0,
    status: '故障中',
  }
}

const fetchCurrentAlarms = async () => {
  try {
    const response = await request.get('/monitor/faults/current', { params: { limit: 1000 } })
    currentAlarms.value = unwrapRows(response).map(normalizeCurrentAlarm)
  } catch (error) {
    console.error('获取首页当前设备故障失败', error)
  }
}

// Hourly water-quality history from the monitoring database.
let chart = null
let chartResizeObserver = null
const activeMetric = ref('cod')
const metrics = [
  { key: 'cod', label: 'COD', code: 'Z-AIT04', unit: 'mg/L' },
  { key: 'nh3n', label: '氨氮', code: 'Z-AIT03', unit: 'mg/L' },
  { key: 'tp', label: '总磷', code: 'Z-AIT06', unit: 'mg/L' },
  { key: 'tn', label: '总氮', code: 'Z-AIT05', unit: 'mg/L' },
  { key: 'ph', label: 'PH', code: 'Z-AIT02', unit: '' },
  { key: 'turbidity', label: '浊度', code: 'Z-AIT01', unit: 'mg/L' },
]

const trendSeries = reactive(Object.fromEntries(metrics.map(metric => [metric.key, []])))

const normalizeHourlyRows = (rows) => {
  const pad = number => String(number).padStart(2, '0')
  return rows.map(row => {
    const date = parseDateTime(row.stat_hour || row.statHour || row.collect_time || row.collectTime || row.time || row.timestamp)
    const value = Number(row.avg_value ?? row.avgValue ?? row.value_decimal ?? row.valueDecimal ?? row.value)
    if (!date || !Number.isFinite(value)) return null
    return {
      time: date.getTime(),
      label: `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:00`,
      value,
    }
  }).filter(Boolean).sort((a, b) => a.time - b.time)
}

const fetchHourlyTrendData = async () => {
  const results = await Promise.allSettled(metrics.map(metric => request.get(
    `/monitor/points/${encodeURIComponent(metric.code)}/hourly`,
    { params: { limit: 24 } }
  )))

  results.forEach((result, index) => {
    const metric = metrics[index]
    if (result.status === 'fulfilled') {
      trendSeries[metric.key] = normalizeHourlyRows(unwrapRows(result.value))
    } else {
      console.error(`获取${metric.label}小时数据失败`, result.reason)
    }
  })
  updateChart()
}

const updateChart = () => {
  if (!chart) return
  const metric = metrics.find(item => item.key === activeMetric.value) || metrics[0]
  const points = trendSeries[activeMetric.value] || []
  const hours = points.map(point => point.label)
  const values = points.map(point => point.value)

  chart.setOption({
    title: {
      show: points.length === 0,
      text: '暂无小时数据',
      left: 'center',
      top: 'middle',
      textStyle: { color: 'rgba(180, 215, 235, 0.68)', fontSize: 13, fontWeight: 400 }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.95)',
      borderColor: 'rgba(0, 232, 255, 0.4)',
      borderWidth: 1,
      textStyle: { color: '#F2F3F5', fontSize: 12, fontFamily: "'Microsoft YaHei', 'Source Han Sans SC', sans-serif" },
      axisPointer: { type: 'line', lineStyle: { color: 'rgba(0, 232, 255, 0.3)' } }
    },
    grid: { left: 8, right: 8, top: 18, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      data: hours,
      axisLabel: { color: 'rgba(0, 232, 255, 0.7)', fontSize: 10, rotate: 0, interval: 3, formatter: value => value.slice(-5), fontFamily: "'Microsoft YaHei', 'Source Han Sans SC', sans-serif" },
      axisLine: { lineStyle: { color: 'rgba(0, 232, 255, 0.25)' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(0, 232, 255, 0.7)', fontSize: 10, fontFamily: "'Microsoft YaHei', 'Source Han Sans SC', sans-serif" },
      splitLine: { lineStyle: { color: 'rgba(0, 232, 255, 0.1)', type: 'dashed' } },
      axisLine: { show: false }
    },
    series: [{
      name: metric.label,
      data: values,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2.5, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: '#00e8ff' },
        { offset: 0.5, color: '#4dfaff' },
        { offset: 1, color: '#00e8ff' }
      ])},
      itemStyle: { color: '#00e8ff', borderColor: '#00e8ff', shadowColor: 'rgba(0, 232, 255, 0.5)', shadowBlur: 6 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 232, 255, 0.25)' },
          { offset: 0.6, color: 'rgba(0, 232, 255, 0.06)' },
          { offset: 1, color: 'rgba(0, 232, 255, 0)' }
        ])
      }
    }]
  })
}

const switchMetric = (m) => { activeMetric.value = m; updateChart() }

const initChart = () => {
  const dom = document.getElementById('trendChart')
  if (!dom) return
  chart = echarts.init(dom)
  updateChart()
  chartResizeObserver = new ResizeObserver(() => { if (chart) chart.resize() })
  chartResizeObserver.observe(dom)
}

// Lifecycle
let alarmTimer = null
let waterTimer = null
let trendTimer = null
const cardEnterTimers = []

const playCardEnterAnimation = () => {
  const delays = [60, 150, 240, 330, 420, 510]
  delays.forEach((delay, index) => {
    const enterTimer = setTimeout(() => {
      cardVisible.value[index] = true
    }, delay)
    cardEnterTimers.push(enterTimer)
  })
}

onMounted(() => {
  playCardEnterAnimation()

  fetchDashboardWaterData()
  fetchCurrentAlarms()
  fetchHourlyTrendData()
  setTimeout(() => initChart(), 300)

  alarmTimer = setInterval(fetchCurrentAlarms, 5000)
  waterTimer = setInterval(fetchDashboardWaterData, 5000)
  trendTimer = setInterval(fetchHourlyTrendData, 5 * 60 * 1000)
})

onBeforeUnmount(() => {
  clearInterval(alarmTimer)
  clearInterval(waterTimer)
  clearInterval(trendTimer)
  cardEnterTimers.forEach((enterTimer) => clearTimeout(enterTimer))
  cardEnterTimers.length = 0
  if (chartResizeObserver) { chartResizeObserver.disconnect(); chartResizeObserver = null }
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
/* ========== Container ========== */
.dashboard-container {
  width: 100%;
  height: calc(100vh - var(--nav-height, 106px));
  background:
    radial-gradient(circle, rgba(0, 232, 255, 0.035) 1px, transparent 1px),
    radial-gradient(ellipse at 15% 40%, rgba(0, 232, 255, 0.03) 0%, transparent 52%),
    radial-gradient(ellipse at 85% 20%, rgba(123, 97, 255, 0.025) 0%, transparent 42%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.08) 0%, rgba(15, 23, 42, 0.12) 50%, rgba(15, 23, 42, 0.18) 100%);
  background-size: 28px 28px, cover, cover, cover;
  display: flex;
  padding: 10px;
  gap: 10px;
  box-sizing: border-box;
  overflow: hidden;
  position: relative;
}

.scene-background {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

.scene-bg-shade {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 50%, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.10)),
    linear-gradient(180deg, rgba(15, 23, 42, 0.04), rgba(15, 23, 42, 0.10));
  pointer-events: none;
}

/* Ambient wave */
.wave-overlay {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 3px,
    rgba(0, 232, 255, 0.005) 3px,
    rgba(0, 232, 255, 0.005) 4px
  );
  pointer-events: none;
  z-index: 1;
  animation: scan-drift 12s linear infinite;
}


/* ========== Panels ========== */
.left-panel, .right-panel {
  width: 380px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}

.center-panel {
  flex: 1;
  position: relative;
  z-index: 2;
}

/* ========== Card Frame (Glass Panel) ========== */
.card-item {
  flex: 1;
  min-height: 0;
  position: relative;
}

.card-frame {
  position: relative;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.88);
  backdrop-filter: saturate(118%);
  border: var(--glass-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  display: flex;
  flex-direction: column;
}

.card-frame:hover {
  border-color: rgba(0, 232, 255, 0.3);
  background: rgba(15, 23, 42, 0.92);
}

/* Frame corners */
.frame-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  z-index: 5;
  pointer-events: none;
}

.frame-corner::before, .frame-corner::after {
  content: '';
  position: absolute;
  background: var(--color-primary);
  border-radius: 1px;
  box-shadow: 0 0 6px var(--color-primary);
  transition: all 0.3s ease;
}

.frame-corner.tl { top: 0; left: 0; }
.frame-corner.tl::before { top: 0; left: 0; width: 14px; height: 2px; }
.frame-corner.tl::after { top: 0; left: 0; width: 2px; height: 14px; }

.frame-corner.tr { top: 0; right: 0; }
.frame-corner.tr::before { top: 0; right: 0; width: 14px; height: 2px; }
.frame-corner.tr::after { top: 0; right: 0; width: 2px; height: 14px; }

.frame-corner.bl { bottom: 0; left: 0; }
.frame-corner.bl::before { bottom: 0; left: 0; width: 14px; height: 2px; }
.frame-corner.bl::after { bottom: 0; left: 0; width: 2px; height: 14px; }

.frame-corner.br { bottom: 0; right: 0; }
.frame-corner.br::before { bottom: 0; right: 0; width: 14px; height: 2px; }
.frame-corner.br::after { bottom: 0; right: 0; width: 2px; height: 14px; }

.card-frame:hover .frame-corner::before,
.card-frame:hover .frame-corner::after {
  box-shadow: 0 0 12px var(--color-primary), 0 0 20px rgba(0, 232, 255, 0.3);
}

/* Glow sweep */
.frame-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 40%, rgba(0, 232, 255, 0.03) 50%, transparent 60%);
  background-size: 300% 300%;
  pointer-events: none;
  z-index: 1;
  animation: glow-sweep 6s ease-in-out infinite;
}


/* ========== Card Header ========== */
.card-header-bar {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: clamp(3px, 0.35vh, 8px) clamp(8px, 0.8vw, 12px);
  border-bottom: 1px solid rgba(0, 232, 255, 0.15);
  flex-shrink: 0;
  position: relative;
  z-index: 3;
}

.header-pulse {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 4px var(--color-primary);
  flex-shrink: 0;
  animation: pulse-glow 2s ease-in-out infinite;
}

.header-pulse.success { background: var(--color-success); box-shadow: 0 0 6px var(--color-success); }
.header-pulse.warning { background: var(--color-warning); box-shadow: 0 0 6px var(--color-warning); }
.header-pulse.danger { background: var(--color-danger); box-shadow: 0 0 6px var(--color-danger); }


.header-text {
  font-family: var(--font-body);
  font-size: clamp(12px, min(1.3vw, 1.8vh), 20px);
  font-weight: 600;
  color: #00e8ff;
  letter-spacing: 0.5px;
  flex: 1;
  text-shadow: 0 0 8px rgba(0, 232, 255, 0.4);
}

.header-badge {
  font-family: var(--font-display);
  font-size: clamp(8px, 0.8vw, 10px);
  font-weight: 600;
  color: #00ff88;
  background: rgba(0, 230, 118, 0.15);
  border: 1px solid rgba(0, 230, 118, 0.4);
  padding: 1px 5px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.5px;
  animation: badge-pulse 1.8s ease-in-out infinite;
}

.header-badge.rec {
  color: #ff4d6a;
  background: rgba(255, 23, 68, 0.15);
  border-color: rgba(255, 23, 68, 0.4);
}


.header-count {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 700;
  min-width: 20px;
  text-align: center;
}

.header-count.danger {
  color: var(--color-danger);
  text-shadow: 0 0 8px rgba(255, 23, 68, 0.5);
}

/* ========== Card Content ========== */
.card-content {
  flex: 1;
  padding: clamp(3px, 0.4vh, 10px) clamp(6px, 0.7vw, 12px);
  min-height: 0;
  overflow: hidden;
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
}

/* ========== Data Table ========== */
.table-scroll {
  flex: 1;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: clamp(10px, min(1.1vw, 1.6vh), 16px);
}

.data-table th {
  text-align: left;
  padding: clamp(2px, 0.3vh, 6px) clamp(4px, 0.5vw, 8px);
  color: #00e8ff;
  font-weight: 600;
  font-size: clamp(9px, min(0.9vw, 1.3vh), 14px);
  font-family: var(--font-display);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(0, 232, 255, 0.2);
  position: sticky;
  top: 0;
  background: rgba(15, 23, 42, 0.95);
}

.data-table td {
  padding: clamp(2px, 0.3vh, 7px) clamp(4px, 0.5vw, 8px);
  border-bottom: 1px solid rgba(0, 232, 255, 0.08);
  transition: all 0.2s ease;
}

.data-table tbody tr:hover td {
  background: rgba(0, 232, 255, 0.08);
}

.metric-name {
  color: #e0eaf5;
  font-weight: 600;
  font-size: clamp(10px, min(1vw, 1.5vh), 16px);
}

.metric-location {
  color: rgba(0, 232, 255, 0.7);
  font-size: clamp(9px, min(0.95vw, 1.4vh), 15px);
}

.metric-value {
  font-family: var(--font-data);
  color: #00e8ff;
  font-weight: 600;
  font-size: clamp(10px, min(1vw, 1.5vh), 16px);
  text-shadow: 0 0 6px rgba(0, 232, 255, 0.3);
}

.status-dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 4px var(--color-success);
}

.status-dot.warn {
  background: var(--color-warning);
  box-shadow: 0 0 4px var(--color-warning);
}

/* ========== Metric Pills ========== */
.trend-content {
  gap: 6px;
  padding-top: 8px;
}

.metric-pills {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 5px;
  margin-bottom: 0;
  flex-shrink: 0;
}

.pill-btn {
  min-width: 0;
  height: 26px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(0, 232, 255, 0.2);
  background: rgba(0, 232, 255, 0.06);
  color: rgba(0, 232, 255, 0.8);
  font-size: 0.8125rem;
  font-family: var(--font-body);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.pill-btn:hover {
  border-color: rgba(0, 232, 255, 0.4);
  color: #fff;
  background: rgba(0, 232, 255, 0.1);
}

.pill-btn.active {
  background: linear-gradient(135deg, rgba(0, 232, 255, 0.2), rgba(0, 232, 255, 0.15));
  border-color: var(--color-primary);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 0 12px rgba(0, 232, 255, 0.2);
}

/* ========== Chart ========== */
.chart-area {
  flex: 1;
  min-height: 145px;
  width: 100%;
}

/* ========== Video ========== */
.video-content { padding: 0 !important; }

.video-player {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: contrast(1.05) saturate(1.1);
}

/* ========== Center Map ========== */
.map-frame {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid rgba(0, 232, 255, 0.12);
  background: transparent;
  backdrop-filter: none;
}

.map-atmosphere,
.map-grid,
.map-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.map-atmosphere {
  background:
    radial-gradient(circle at 24% 26%, rgba(0, 232, 255, 0.08), transparent 20%),
    radial-gradient(circle at 78% 32%, rgba(77, 106, 255, 0.08), transparent 24%),
    radial-gradient(circle at 55% 80%, rgba(0, 232, 255, 0.04), transparent 22%);
  opacity: 0.42;
}

.map-grid {
  background-image:
    linear-gradient(rgba(111, 221, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(111, 221, 255, 0.06) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at center, rgba(255, 255, 255, 0.85), transparent 88%);
  opacity: 0.12;
}

.map-vignette {
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.01), rgba(15, 23, 42, 0.05)),
    radial-gradient(circle at center, transparent 56%, rgba(2, 8, 16, 0.08) 100%);
}

.scene-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  position: relative;
  z-index: 0;
}

.map-header-band {
  position: absolute;
  top: 16px;
  left: 16px;
  right: 16px;
  z-index: 6;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.map-title-stack {
  max-width: 440px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(0, 232, 255, 0.18);
  box-shadow:
    inset 0 1px 0 rgba(179, 245, 255, 0.08),
    0 20px 36px rgba(0, 0, 0, 0.2);
  backdrop-filter: saturate(116%);
}

.map-eyebrow {
  font-size: 0.8125rem;
  letter-spacing: 1.8px;
  color: rgba(0, 232, 255, 0.8);
  margin-bottom: 8px;
  font-family: var(--font-display);
}

.map-title-main {
  font-size: 2.25rem;
  line-height: 1.08;
  color: #f4fbff;
  font-weight: 700;
  text-shadow: 0 0 16px rgba(0, 232, 255, 0.16);
}

.map-title-sub {
  margin-top: 8px;
  color: rgba(224, 234, 245, 0.85);
  font-size: 1rem;
  line-height: 1.5;
}

.map-command-dock {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 14px;
  z-index: 6;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 16px;
}

.dock-shortcuts {
  display: flex;
  flex-direction: column;
  width: min(100%, 1040px);
  gap: 10px;
}

.dock-shortcut-row {
  display: flex;
  justify-content: center;
  width: 100%;
  gap: 10px;
}

.dock-link {
  flex: 1 1 120px;
  max-width: 120px;
  width: 120px;
  min-width: 0;
  box-sizing: border-box;
  min-height: 46px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(0, 232, 255, 0.2);
  background: rgba(15, 23, 42, 0.8);
  color: #effaff;
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
  text-align: center;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15);
  backdrop-filter: saturate(114%);
}

.dock-link:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 232, 255, 0.38);
  box-shadow:
    0 14px 26px rgba(0, 0, 0, 0.24),
    0 0 18px rgba(0, 232, 255, 0.12);
}

.dock-link-name {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
}

.dock-link-name {
  font-size: 1rem;
  line-height: 1.3;
  color: #e0eaf5;
}

/* ========== PLC Beacons ========== */
.plc-beacon {
  position: absolute;
  cursor: pointer;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.beacon-core {
  width: 10px;
  height: 10px;
  background: var(--color-danger);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--color-danger), 0 0 20px rgba(255, 23, 68, 0.4);
  position: relative;
  z-index: 2;
  transition: all 0.3s ease;
  animation: beacon-pulse 2s ease-in-out infinite;
}

.beacon-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 23, 68, 0.6);
  z-index: 1;
}


.plc-beacon:hover .beacon-core {
  transform: scale(1.5);
  box-shadow: 0 0 14px var(--color-danger), 0 0 30px rgba(255, 23, 68, 0.6);
}

.beacon-label {
  position: absolute;
  bottom: 18px;
  white-space: nowrap;
  background: rgba(2, 12, 28, 0.92);
  backdrop-filter: none;
  border: var(--border-glow);
  color: var(--color-primary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.9375rem;
  font-weight: 500;
  opacity: 0;
  transform: translateY(6px);
  pointer-events: none;
  transition: all 0.35s ease;
  box-shadow: 0 0 16px rgba(0, 232, 255, 0.2);
  letter-spacing: 0.5px;
}

.beacon-label::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 4px solid transparent;
  border-top-color: var(--color-primary);
}

.plc-beacon:hover .beacon-label {
  opacity: 1;
  transform: translateY(0);
}

/* ========== Gauge Content ========== */
.gauge-content {
  align-items: center;
  justify-content: center;
}

.total-flow-content {
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding-top: 6px;
}

.total-flow-wave {
  position: relative;
  width: 174px;
  height: 174px;
  flex: 0 0 174px;
  overflow: hidden;
  border: 2px solid rgba(0, 232, 255, 0.56);
  border-radius: 50%;
  background: rgba(3, 18, 35, 0.82);
  box-shadow:
    inset 0 0 24px rgba(0, 232, 255, 0.14),
    0 0 18px rgba(0, 232, 255, 0.16);
  isolation: isolate;
}

.total-flow-wave::before {
  content: '';
  position: absolute;
  inset: 8px;
  z-index: 2;
  border: 1px solid rgba(140, 238, 255, 0.14);
  border-radius: 50%;
  pointer-events: none;
}

.total-flow-water {
  position: absolute;
  right: -3%;
  bottom: -2%;
  left: -3%;
  height: 58%;
  z-index: 1;
  background: linear-gradient(180deg, rgba(0, 232, 255, 0.56), rgba(0, 111, 224, 0.76));
  box-shadow: inset 0 12px 18px rgba(116, 244, 255, 0.12);
}

.total-flow-water::before,
.total-flow-water::after {
  content: '';
  position: absolute;
  left: 50%;
  width: 230%;
  aspect-ratio: 1;
  border-radius: 44%;
  transform: translate(-50%, -88%);
  transform-origin: 50% 50%;
}

.total-flow-water::before {
  top: 0;
  background: rgba(4, 24, 43, 0.96);
  animation: total-flow-wave-roll 9s linear infinite;
}

.total-flow-water::after {
  top: 5px;
  border-radius: 47%;
  background: rgba(4, 37, 65, 0.72);
  animation: total-flow-wave-roll 13s linear infinite reverse;
}

.total-flow-reading {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 22px 14px 10px;
  text-align: center;
  pointer-events: none;
}

.total-flow-label {
  margin-bottom: 5px;
  color: rgba(205, 245, 255, 0.82);
  font-size: 13px;
  font-weight: 600;
}

.total-flow-value {
  max-width: 100%;
  color: #f4fdff;
  font-family: var(--font-data);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.08;
  white-space: nowrap;
  text-shadow: 0 0 12px rgba(0, 232, 255, 0.72), 0 2px 4px rgba(0, 22, 45, 0.9);
}

.total-flow-value.compact { font-size: 22px; }
.total-flow-value.extra-compact { font-size: 18px; }

.total-flow-unit {
  margin-top: 5px;
  color: #a9efff;
  font-family: var(--font-data);
  font-size: 14px;
  font-weight: 600;
}

.total-flow-source {
  color: rgba(169, 221, 238, 0.72);
  font-size: 12px;
  font-weight: 600;
}

/* ========== Alarm Section ========== */
.alarm-content {
  gap: 6px;
}

.alarm-stats {
  display: flex;
  gap: 8px;
}

.stat-chip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 5px 8px;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  background: rgba(15, 23, 42, 0.6);
}

.stat-chip.danger {
  border-color: rgba(255, 23, 68, 0.3);
  background: rgba(255, 23, 68, 0.08);
}

.stat-chip.warning {
  border-color: rgba(255, 171, 0, 0.3);
  background: rgba(255, 171, 0, 0.08);
}

.chip-num {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 700;
  line-height: 1;
}

.stat-chip.danger .chip-num {
  color: var(--color-danger);
  text-shadow: 0 0 8px rgba(255, 23, 68, 0.4);
}

.stat-chip.warning .chip-num {
  color: var(--color-warning);
  text-shadow: 0 0 8px rgba(255, 171, 0, 0.4);
}

.chip-label {
  font-size: 0.8125rem;
  color: rgba(224, 234, 245, 0.8);
}

.alarm-scroll {
  flex: 1;
  overflow: hidden;
}

.alarm-scroll:hover {
  overflow-y: auto;
}

.scroll-track {
  width: 100%;
}

.scroll-track.scrolling {
  animation: scroll-up 12s linear infinite;
}

.alarm-scroll:hover .scroll-track.scrolling {
  animation-play-state: paused;
}

.alarm-scroll::-webkit-scrollbar { width: 4px; }
.alarm-scroll::-webkit-scrollbar-track { background: rgba(10,30,48,0.4); }
.alarm-scroll::-webkit-scrollbar-thumb { background: rgba(255,80,80,0.3); border-radius: 999px; }

@keyframes scroll-up {
  0% { transform: translateY(0); }
  100% { transform: translateY(-50%); }
}

@keyframes total-flow-wave-roll {
  from { transform: translate(-50%, -88%) rotate(0deg); }
  to { transform: translate(-50%, -88%) rotate(360deg); }
}

.alarm-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  font-size: 0.8125rem;
  color: #e0eaf5;
}

.alarm-table th {
  padding: 4px 4px;
  color: #00e8ff;
  font-weight: 600;
  font-size: 0.75rem;
  font-family: var(--font-display);
  letter-spacing: 0.5px;
  text-align: left;
  border-bottom: 1px solid rgba(0, 232, 255, 0.2);
  text-transform: uppercase;
}

.alarm-table th:nth-child(1) { width: 25%; }
.alarm-table th:nth-child(2) { width: 25%; }
.alarm-table th:nth-child(3) { width: 31%; }
.alarm-table th:nth-child(4) { width: 19%; }

.alarm-table td {
  padding: 5px 4px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.08);
  white-space: nowrap;
  color: #e0eaf5;
  overflow: hidden;
  text-overflow: ellipsis;
}

.alarm-table .alarm-empty {
  height: 70px;
  color: rgba(190, 220, 236, 0.68);
  text-align: center;
}

.alarm-table tr.recovered {
  opacity: 0.62;
}

.alarm-device {
  color: #f2f7ff;
  font-weight: 700;
}

.alarm-location {
  color: rgba(140, 238, 255, 0.76);
}

.alarm-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--radius-sm);
  color: #ff9a9a;
  background: rgba(255, 23, 68, 0.12);
  border: 1px solid rgba(255, 23, 68, 0.28);
  font-size: 0.75rem;
  font-weight: 700;
}

.alarm-status.recovered {
  color: #ffd68f;
  background: rgba(255, 171, 0, 0.10);
  border-color: rgba(255, 171, 0, 0.26);
}

.alarm-id {
  font-family: var(--font-data);
  font-size: 0.875rem;
  color: #ff4d6a;
  background: rgba(255, 23, 68, 0.12);
  border: 1px solid rgba(255, 23, 68, 0.3);
  padding: 1px 5px;
  border-radius: 2px;
}

.time-cell {
  font-family: var(--font-data);
  font-size: 0.75rem;
  color: rgba(224, 234, 245, 0.7);
}

/* ========== Card Emerge Animation ========== */
.card-emerge-enter-active {
  transition:
    opacity 0.86s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.86s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.86s ease;
  will-change: opacity, transform;
}

.card-emerge-enter-from {
  opacity: 0;
  transform: translate3d(0, 28px, 0) scale(0.985);
  box-shadow: 0 0 0 rgba(0, 232, 255, 0);
}

.card-emerge-enter-to {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
}

.card-emerge-enter-active .card-frame {
  animation: card-light-pass 0.92s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@media (prefers-reduced-motion: reduce) {
  .card-emerge-enter-active {
    transition: opacity 0.2s ease;
  }

  .card-emerge-enter-from {
    opacity: 0;
    transform: none;
  }

  .card-emerge-enter-active .card-frame {
    animation: none;
  }

  .total-flow-water::before,
  .total-flow-water::after {
    animation: none;
  }
}

@keyframes card-light-pass {
  0% {
    border-color: rgba(0, 232, 255, 0.08);
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.16);
  }
  55% {
    border-color: rgba(0, 232, 255, 0.34);
    box-shadow: 0 14px 36px rgba(0, 232, 255, 0.12);
  }
  100% {
    border-color: rgba(0, 232, 255, 0.14);
    box-shadow: none;
  }
}

@keyframes scan-drift {
  0% { transform: translateY(0); }
  100% { transform: translateY(20px); }
}

@keyframes glow-sweep {
  0% { background-position: 200% 0; }
  50% { background-position: -100% 0; }
  100% { background-position: 200% 0; }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 4px var(--color-primary); opacity: 0.7; }
  50% { box-shadow: 0 0 10px var(--color-primary), 0 0 20px rgba(0, 232, 255, 0.3); opacity: 1; }
}

@keyframes badge-pulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

@keyframes beacon-pulse {
  0%, 100% { box-shadow: 0 0 8px var(--color-danger), 0 0 20px rgba(255, 23, 68, 0.4); transform: scale(1); }
  50% { box-shadow: 0 0 14px var(--color-danger), 0 0 30px rgba(255, 23, 68, 0.6); transform: scale(1.2); }
}

/* ========== Navigation Dots ========== */
.nav-dots-layer {
  position: absolute;
  inset: 0;
  z-index: 5;
  pointer-events: none;
}

.nav-dot {
  position: absolute;
  width: 40px;
  height: 40px;
  cursor: pointer;
  pointer-events: auto;
  transform: translate(-50%, -50%);
  transition: opacity 0.3s ease;
  z-index: 10;
}

.nav-dot.locked {
  opacity: 0.3;
  cursor: default;
}

.nav-dot.locked:hover {
  opacity: 0.6;
}

.nav-dot:not(.locked) {
  opacity: 1;
}

.nav-dot.dragging {
  z-index: 20;
  cursor: grabbing;
}

.dot-core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-danger);
  box-shadow: 0 0 8px var(--color-danger), 0 0 20px rgba(255, 23, 68, 0.4);
  transition: all 0.3s ease;
  animation: dot-pulse 2s ease-in-out infinite;
}

.nav-dot.locked .dot-core {
  background: rgba(0, 232, 255, 0.4);
  box-shadow: 0 0 6px rgba(0, 232, 255, 0.3);
  animation: none;
}

.nav-dot:not(.locked) .dot-core {
  background: var(--color-danger);
  box-shadow: 0 0 8px var(--color-danger), 0 0 20px rgba(255, 23, 68, 0.4);
}

.dot-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 23, 68, 0.6);
  transition: all 0.3s ease;
}

.nav-dot.locked .dot-ring {
  border-color: rgba(0, 232, 255, 0.3);
}

.nav-dot:not(.locked) .dot-ring {
  border-color: rgba(255, 23, 68, 0.6);
}

.nav-dot:not(.locked):hover .dot-core {
  transform: translate(-50%, -50%) scale(1.3);
  box-shadow: 0 0 14px var(--color-danger), 0 0 30px rgba(255, 23, 68, 0.6);
}

.dot-tooltip {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  background: rgba(2, 12, 28, 0.95);
  border: 1px solid rgba(0, 232, 255, 0.4);
  color: #00e8ff;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  pointer-events: none;
  box-shadow: 0 0 16px rgba(0, 232, 255, 0.3);
  letter-spacing: 0.5px;
}

.dot-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: rgba(0, 232, 255, 0.4);
}

.tooltip-fade-enter-active {
  transition: all 0.2s ease;
}
.tooltip-fade-leave-active {
  transition: all 0.15s ease;
}
.tooltip-fade-enter-from,
.tooltip-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px);
}

/* ========== Dot Controls ========== */
.dot-controls {
  position: absolute;
  inset: auto 0 0;
  z-index: 8;
  pointer-events: none;
}

.control-btn {
  position: absolute;
  bottom: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 50%;
  border: 1px solid rgba(0, 232, 255, 0.16);
  background: rgba(15, 23, 42, 0.42);
  color: #e0eaf5;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: var(--font-body);
  opacity: 0.48;
  pointer-events: auto;
  backdrop-filter: saturate(108%);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.16);
}

.unlock-btn {
  left: 18px;
}

.lock-btn {
  right: 18px;
}

.control-btn:hover {
  opacity: 0.82;
  border-color: rgba(0, 232, 255, 0.32);
  background: rgba(15, 23, 42, 0.7);
  transform: translateY(-1px) scale(1.04);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.22);
}

.control-btn.active {
  opacity: 0.64;
  border-color: rgba(0, 232, 255, 0.42);
  background: rgba(0, 232, 255, 0.08);
  box-shadow: 0 0 10px rgba(0, 232, 255, 0.12);
}

.btn-icon {
  font-size: 0.875rem;
  line-height: 1;
}

@keyframes dot-pulse {
  0%, 100% { box-shadow: 0 0 8px var(--color-danger), 0 0 20px rgba(255, 23, 68, 0.4); transform: translate(-50%, -50%) scale(1); }
  50% { box-shadow: 0 0 14px var(--color-danger), 0 0 30px rgba(255, 23, 68, 0.6); transform: translate(-50%, -50%) scale(1.15); }
}
</style>
