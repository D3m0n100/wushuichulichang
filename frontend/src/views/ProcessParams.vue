<template>
  <div class="params-page">
    <div class="wave-overlay"></div>

    <div class="page-header">
      <div class="header-title-group">
        <span class="header-dot"></span>
        <h2 class="header-title">工艺参数设置</h2>
        <span class="header-line"></span>
        <span class="header-sub">PROCESS PARAMETERS SETTINGS</span>
      </div>
    </div>

    <div class="cards-grid">
    <div class="cards-col">

    <!-- ========== B1 粗格栅、调节池及提升泵房 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>B1 粗格栅、调节池及提升泵房</div>

      <!-- 液位参数 -->
      <div class="sub-title">液位参数</div>
      <el-table :data="b1LevelParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="2" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onParameterWrite(row.code, row.setValue)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 联动参数（搅拌机） -->
      <div class="sub-title">联动参数（搅拌机）</div>
      <el-table :data="b1TimerParams" stripe border class="dark-table" size="small">
        <el-table-column prop="deviceName" label="设备名称" min-width="90" show-overflow-tooltip />
        <el-table-column label="已运行时间" width="110" align="center">
          <template #default="{ row }"><span class="val-ro">{{ row.runtimeDisplay }}</span></template>
        </el-table-column>
        <el-table-column label="已停止时间" width="110" align="center">
          <template #default="{ row }"><span class="val-ro">{{ row.stoppedDisplay }}</span></template>
        </el-table-column>
        <el-table-column label="运行计时" width="176" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ row.runSetDisplay }}</span>
              <div class="time-inputs">
                <el-input-number v-model="row.runSetH" size="small" :min="0" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.runSetM" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.runSetS" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="停止计时" width="176" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ row.stopSetDisplay }}</span>
              <div class="time-inputs">
                <el-input-number v-model="row.stopSetH" size="small" :min="0" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.stopSetM" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.stopSetS" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onTimerWrite(row)">确认运行</el-button>
            <el-button type="primary" size="small" link @click="onTimerStopWrite(row)">确认停止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 仪表 -->
      <div class="sub-title">仪表</div>
      <el-table :data="b1Instruments" stripe border class="dark-table" size="small">
        <el-table-column prop="code" label="参数代码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="输出值" width="80" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.output) }}</span></template>
        </el-table-column>
        <el-table-column label="上限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.upperCur) }}</span>
              <el-input-number v-model="row.upper" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.lowerCur) }}</span>
              <el-input-number v-model="row.lower" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏差值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.devCur) }}</span>
              <el-input-number v-model="row.dev" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onInstrumentWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 压滤机加药装置 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>压滤机加药装置</div>

      <!-- 参数 -->
      <div class="sub-title">参数</div>
      <el-table :data="bjdParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.readonly" class="cell-stack">
              <span class="val-dash">—</span>
            </div>
            <div v-else class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.readonly" type="primary" size="small" link @click="onBjdParameterWrite(row.code, row.setValue)">确认</el-button>
            <span v-else class="val-dash">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 滤布滤池转盘 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>滤布滤池转盘</div>

      <!-- 参数 -->
      <div class="sub-title">参数</div>
      <el-table :data="bfdParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onBfdParameterWrite(row.code, row.setValue)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 压滤机 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>压滤机</div>

      <!-- 参数 -->
      <div class="sub-title">参数</div>
      <el-table :data="bfpParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.readonly" class="cell-stack">
              <span class="val-dash">—</span>
            </div>
            <div v-else class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.readonly" type="primary" size="small" link @click="onBfpParameterWrite(row.code, row.setValue)">确认</el-button>
            <span v-else class="val-dash">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 高效沉淀池 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>高效沉淀池</div>

      <!-- 仪表 -->
      <div class="sub-title">仪表</div>
      <el-table :data="m3Instruments" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="输出值" width="80" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.output) }}</span></template>
        </el-table-column>
        <el-table-column label="上限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.upperCur) }}</span>
              <el-input-number v-model="row.upper" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.lowerCur) }}</span>
              <el-input-number v-model="row.lower" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏差值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.devCur) }}</span>
              <el-input-number v-model="row.dev" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onM3InstrumentWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 设备频率 -->
      <div class="sub-title">设备频率</div>
      <el-table :data="m3FreqParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="频率反馈" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.freqFb) }}</span></template>
        </el-table-column>
        <el-table-column label="频率给定" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.freqSet" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onM3FreqWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 逻辑参数 -->
      <div class="sub-title">逻辑参数</div>
      <el-table :data="m3LogicParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="2" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onM3LogicWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 联动参数 -->
      <div class="sub-title">联动参数</div>
      <el-table :data="m3TimerParams" stripe border class="dark-table" size="small">
        <el-table-column prop="deviceName" label="设备组" min-width="90" show-overflow-tooltip />
        <el-table-column label="已运行时间" width="110" align="center">
          <template #default="{ row }"><span class="val-ro">{{ row.runtimeDisplay }}</span></template>
        </el-table-column>
        <el-table-column label="已停止时间" width="110" align="center">
          <template #default="{ row }"><span class="val-ro">{{ row.stoppedDisplay }}</span></template>
        </el-table-column>
        <el-table-column label="运行计时" width="176" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ row.runSetDisplay }}</span>
              <div class="time-inputs">
                <el-input-number v-model="row.runSetH" size="small" :min="0" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.runSetM" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.runSetS" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="停止计时" width="176" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ row.stopSetDisplay }}</span>
              <div class="time-inputs">
                <el-input-number v-model="row.stopSetH" size="small" :min="0" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.stopSetM" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.stopSetS" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onM3TimerWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 接触消毒池 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>接触消毒池</div>

      <!-- 仪表 -->
      <div class="sub-title">仪表</div>
      <el-table :data="p2Instruments" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="输出值" width="80" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.output) }}</span></template>
        </el-table-column>
        <el-table-column label="上限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.upperCur) }}</span>
              <el-input-number v-model="row.upper" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.lowerCur) }}</span>
              <el-input-number v-model="row.lower" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏差值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.devCur) }}</span>
              <el-input-number v-model="row.dev" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onP2InstrumentWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 逻辑参数 -->
      <div class="sub-title">逻辑参数</div>
      <el-table :data="p2LogicParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="2" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onP2LogicWrite()">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    </div><!-- cards-col left -->
    <div class="cards-col">

    <!-- ========== 鼓风机房 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>鼓风机房</div>

      <!-- 仪表 -->
      <div class="sub-title">仪表</div>
      <el-table :data="fanInstruments" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="输出值" width="80" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.output) }}</span></template>
        </el-table-column>
        <el-table-column label="上限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.upperCur) }}</span>
              <el-input-number v-model="row.upper" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.lowerCur) }}</span>
              <el-input-number v-model="row.lower" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏差值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.devCur) }}</span>
              <el-input-number v-model="row.dev" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onFanInstrumentWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 参数 -->
      <div class="sub-title">参数</div>
      <el-table :data="fanParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :min="FAN_FREQUENCY_MIN" :max="FAN_FREQUENCY_MAX" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onFanParameterWrite(row.code, row.setValue)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== AAO生物池 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>AAO生物池</div>

      <!-- 仪表 -->
      <div class="sub-title">仪表</div>
      <el-table :data="h1Instruments" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="输出值" width="80" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.output) }}</span></template>
        </el-table-column>
        <el-table-column label="上限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.upperCur) }}</span>
              <el-input-number v-model="row.upper" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下限值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.lowerCur) }}</span>
              <el-input-number v-model="row.lower" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="偏差值" width="110" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ formatParameterValue(row.devCur) }}</span>
              <el-input-number v-model="row.dev" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onH1InstrumentWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 设备频率 -->
      <div class="sub-title">设备频率</div>
      <el-table :data="h1FreqParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="频率反馈" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.freqFb) }}</span></template>
        </el-table-column>
        <el-table-column label="频率给定" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.freqSet" size="small" :step="0.1" :precision="1" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onH1FreqWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 逻辑参数 -->
      <div class="sub-title">逻辑参数</div>
      <el-table :data="h1LogicParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number v-model="row.setValue" size="small" :step="0.1" :precision="2" controls-position="right" style="width:78px" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onH1LogicWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-gap"></div>

      <!-- 联动参数 -->
      <div class="sub-title">联动参数</div>
      <el-table :data="h1TimerParams" stripe border class="dark-table" size="small">
        <el-table-column prop="deviceName" label="设备组" min-width="90" show-overflow-tooltip />
        <el-table-column label="已运行时间" width="110" align="center">
          <template #default="{ row }"><span class="val-ro">{{ row.runtimeDisplay }}</span></template>
        </el-table-column>
        <el-table-column label="已停止时间" width="110" align="center">
          <template #default="{ row }"><span class="val-ro">{{ row.stoppedDisplay }}</span></template>
        </el-table-column>
        <el-table-column label="运行计时" width="176" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ row.runSetDisplay }}</span>
              <div class="time-inputs">
                <el-input-number v-model="row.runSetH" size="small" :min="0" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.runSetM" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.runSetS" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="停止计时" width="176" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <span class="cell-cur">{{ row.stopSetDisplay }}</span>
              <div class="time-inputs">
                <el-input-number v-model="row.stopSetH" size="small" :min="0" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.stopSetM" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
                <span class="time-sep">:</span>
                <el-input-number v-model="row.stopSetS" size="small" :min="0" :max="59" controls-position="right" style="width:38px" />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onH1TimerWrite(row)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 细格栅及旋流沉砂池 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>细格栅及旋流沉砂池</div>

      <!-- 参数 -->
      <div class="sub-title">参数</div>
      <el-table :data="mlcParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ========== 除臭设备 ========== -->
    <div class="section-card frame-card">
      <div class="frame-corner tl"></div><div class="frame-corner tr"></div>
      <div class="frame-corner bl"></div><div class="frame-corner br"></div>
      <div class="card-header-bar"><span class="pulse-dot"></span>除臭设备</div>

      <div class="sub-title">风机频率及系统启停延时</div>
      <el-table :data="deoControlParams" stripe border class="dark-table" size="small">
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前值" width="100" align="center">
          <template #default="{ row }"><span class="val-ro">{{ formatParameterValue(row.currentValue) }}</span></template>
        </el-table-column>
        <el-table-column label="设置值" width="120" align="center">
          <template #default="{ row }">
            <div class="cell-stack">
              <el-input-number
                v-model="row.setValue"
                size="small"
                :min="row.min"
                :max="row.max"
                :step="row.step"
                :precision="row.precision"
                controls-position="right"
                style="width:88px"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="onDeoParameterWrite(row.code, row.setValue)">确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    </div><!-- cards-col right -->
    </div><!-- cards-grid -->
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { FAN_FREQUENCY_MAX, FAN_FREQUENCY_MIN, isFanFrequencyInRange } from '@/utils/fanFrequency'
import {
  DEO_FAN_FREQUENCY_MAX,
  DEO_FAN_FREQUENCY_MIN,
  isDeodorizationFanFrequencyInRange,
} from '@/utils/deodorizationFrequency'

const formatParameterValue = (value) => {
  if (value === null || value === undefined || value === '') return '--'
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(3) : value
}

// ==================== B1 粗格栅、调节池及提升泵房 — 数据定义 ====================

// 液位参数
const b1LevelParams = ref([
  { code: 'B1_GP_SINGLE_LEVEL', description: '调节池提升泵单台液位', currentValue: 0, setValue: 0 },
  { code: 'B1_GP_DOUBLE_LEVEL', description: '调节池提升泵两台液位', currentValue: 0, setValue: 0 },
  { code: 'B1_GP_STOP_LEVEL', description: '调节池提升泵停止液位', currentValue: 0, setValue: 0 }
])

// 联动参数（搅拌机 HW1~2 定时器）
const b1TimerParams = ref([
  {
    deviceName: '调节池潜水搅拌机1-2',
    runtimeDisplay: '0:0:0',
    stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0',
    runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0',
    stopSetH: 0, stopSetM: 0, stopSetS: 0,
    // 后端参数代码映射
    runtimeCodes: { h: 'B1_HW_RUNTIME_HOURS', m: 'B1_HW_RUNTIME_MINUTES', s: 'B1_HW_RUNTIME_SECONDS' },
    stoppedCodes: { h: 'B1_HW_STOPPED_HOURS', m: 'B1_HW_STOPPED_MINUTES', s: 'B1_HW_STOPPED_SECONDS' },
    runSetCodes: { h: 'B1_HW_RUN_SET_HOURS', m: 'B1_HW_RUN_SET_MINUTES', s: 'B1_HW_RUN_SET_SECONDS' },
    stopSetCodes: { h: 'B1_HW_STOP_SET_HOURS', m: 'B1_HW_STOP_SET_MINUTES', s: 'B1_HW_STOP_SET_SECONDS' }
  }
])

// 仪表
const b1Instruments = ref([
  { code: 'B1_FIT01', description: '一期CASS池进水流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'B1_LIT01', description: '调节池液位', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'T3_RIT01', description: '污泥界面仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 }
])

// ==================== 压滤机加药装置 — 数据定义 ====================

const bjdParams = ref([
  { code: 'BJD_FEED_MOTOR_FREQ_DISPLAY', description: '进料电机频率显示', currentValue: 0, readonly: true },
  { code: 'BJD_FEED_MOTOR_FREQ_SET', description: '进料电机频率给定', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_HEAT_OFF_TIME', description: '加热关时间设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_HEAT_ON_TIME', description: '加热开时间设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_HIGH_LEVEL_SET', description: '高液位设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_LOW_LEVEL_SET', description: '低液位设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_MID_LEVEL_SET', description: '中液位设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_START_FLOW_SET', description: '开机进水流量设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_STOP_FLOW_SET', description: '停机进水流量设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_VIBRATION_OFF_TIME', description: '振动关时间设置', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BJD_VIBRATION_ON_TIME', description: '振动开时间设置', currentValue: 0, setValue: 0, readonly: false }
])

// ==================== 滤布滤池转盘 — 数据定义 ====================

const bfdParams = ref([
  { code: 'BFD_1_HIGH_LEVEL_SET', description: '1#转盘高液位设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_1_LOW_LEVEL_SET', description: '1#转盘低液位设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_1_SUPER_HIGH_LEVEL_SET', description: '1#转盘超高液位设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_2_HIGH_LEVEL_SET', description: '2#转盘高液位设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_2_LOW_LEVEL_SET', description: '2#转盘低液位设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_2_SUPER_HIGH_LEVEL_SET', description: '2#转盘超高液位设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_BACKWASH_TIME_SET', description: '反冲时间设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_FILTER_TIME_SET', description: '过滤时间设置', currentValue: 0, setValue: 0 },
  { code: 'BFD_LEVEL_RANGE_SET', description: '液位测量范围设置', currentValue: 0, setValue: 0 }
])

// ==================== 压滤机 — 数据定义 ====================

const bfpParams = ref([
  { code: 'BFP_1_MAIN_FREQ_DISPLAY', description: '1#带机主机频率显示', currentValue: 0, readonly: true },
  { code: 'BFP_1_MAIN_FREQ_SET', description: '1#带机主机频率给定', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BFP_1_THICKENER_FREQ_DISPLAY', description: '1#带机浓缩机频率显示', currentValue: 0, readonly: true },
  { code: 'BFP_1_THICKENER_FREQ_SET', description: '1#带机浓缩机频率给定', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BFP_2_MAIN_FREQ_DISPLAY', description: '2#带机主机频率显示', currentValue: 0, readonly: true },
  { code: 'BFP_2_MAIN_FREQ_SET', description: '2#带机主机频率给定', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BFP_2_THICKENER_FREQ_DISPLAY', description: '2#带机浓缩机频率显示', currentValue: 0, readonly: true },
  { code: 'BFP_2_THICKENER_FREQ_SET', description: '2#带机浓缩机频率给定', currentValue: 0, setValue: 0, readonly: false },
  { code: 'BFP_AIR_DELAY_AFTER_START', description: '空压机启动后其余设备延时开启时间给定', currentValue: 0, setValue: 0, readonly: false }
])

// ==================== 鼓风机房 — 数据定义 ====================

const fanInstruments = ref([
  { code: 'H6_FIQT01', description: '空气管热质流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H6_PIT01', description: '空气管压力变送器', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H6_TTT01', description: '空气管温度变送器', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 }
])

const fanParams = ref([
  { code: 'FAN_SYS_1_TARGET_FREQ', description: '1#鼓风机 目标频率', currentValue: 0, setValue: 0 },
  { code: 'FAN_SYS_2_TARGET_FREQ', description: '2#鼓风机 目标频率', currentValue: 0, setValue: 0 },
  { code: 'FAN_SYS_3_TARGET_FREQ', description: '3#鼓风机 目标频率', currentValue: 0, setValue: 0 }
])

// ==================== 高效沉淀池 — 数据定义 ====================

const m3Instruments = ref([
  { code: 'M3_AIT01', description: '高效沉淀池浊度计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'M3_FIT01', description: '一期转盘进水流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'M3_FIT02', description: '高效沉淀池剩余污泥流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'M3_LIT01', description: '高效沉淀池1#超声波液位计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'M3_LIT02', description: '高效沉淀池2#超声波液位计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'M3_RIT01', description: '高效沉淀池污泥界面仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 }
])

// 设备频率
const m3FreqParams = ref([
  { code: 'M3_GP1', description: '1#回流污泥转子泵', freqFb: 0, freqSet: 0, fbCode: 'M3_GP1_FREQ_FB', setCode: 'M3_GP1_FREQ_SET' },
  { code: 'M3_GP2', description: '2#回流污泥转子泵', freqFb: 0, freqSet: 0, fbCode: 'M3_GP2_FREQ_FB', setCode: 'M3_GP2_FREQ_SET' },
  { code: 'M3_GP3', description: '剩余污泥转子泵', freqFb: 0, freqSet: 0, fbCode: 'M3_GP3_FREQ_FB', setCode: 'M3_GP3_FREQ_SET' },
  { code: 'M3_HW1', description: '高效沉淀池絮凝搅拌机', freqFb: 0, freqSet: 0, fbCode: 'M3_HW1_FREQ_FB', setCode: 'M3_HW1_FREQ_SET' },
  { code: 'M3_HW2', description: '高效沉淀池混合搅拌机', freqFb: 0, freqSet: 0, fbCode: 'M3_HW2_FREQ_FB', setCode: 'M3_HW2_FREQ_SET' }
])

// 逻辑参数
const m3LogicParams = ref([
  { code: 'M3_GP3_LEVEL_START_LEVEL', configCode: 'M3_GP3_LEVEL', description: '剩余污泥转子泵 液位联动启动液位', currentValue: 0, setValue: 0, paramType: 'startLevel' },
  { code: 'M3_GP3_LEVEL_STOP_LEVEL', configCode: 'M3_GP3_LEVEL', description: '剩余污泥转子泵 液位联动停止液位', currentValue: 0, setValue: 0, paramType: 'stopLevel' }
])

// 联动参数（定时器）
const m3TimerParams = ref([
  {
    deviceName: '回流污泥转子泵1-2', code: 'M3_TIMER_GP12',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '剩余污泥转子泵', code: 'M3_TIMER_GP3',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '刮泥机', code: 'M3_TIMER_HL1',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  }
])

// ==================== 除臭设备 — 数据定义 ====================

const deoControlParams = ref([
  { code: 'DEO_FAN_FREQ_SET_VD472', description: '风机频率给定', currentValue: 0, setValue: 0, min: DEO_FAN_FREQUENCY_MIN, max: DEO_FAN_FREQUENCY_MAX, step: 0.1, precision: 1 },
  { code: 'DEO_SYSTEM_START_DELAY_VW530', description: '一键启动延时时间', currentValue: 0, setValue: 0, min: 0, max: 32767, step: 1, precision: 0 },
  { code: 'DEO_SYSTEM_STOP_DELAY_VW532', description: '一键停止延时时间', currentValue: 0, setValue: 0, min: 0, max: 32767, step: 1, precision: 0 }
])

// ==================== 细格栅及旋流沉砂池 — 数据定义 ====================

const mlcParams = ref([
  { code: 'MLC_COARSE_INTERVAL_TIME', description: '粗格栅间歇时间', currentValue: 0 },
  { code: 'MLC_CONVEYOR_DELAY_TIME', description: '输送机延时时间', currentValue: 0 },
  { code: 'MLC_PUMP1_INTERVAL_TIME', description: '涡流排砂泵1间歇时间', currentValue: 0 },
  { code: 'MLC_PUMP1_RUN_TIME', description: '涡流排砂泵1运行时间', currentValue: 0 },
  { code: 'MLC_PUMP2_INTERVAL_TIME', description: '涡流排砂泵2间歇时间', currentValue: 0 },
  { code: 'MLC_PUMP2_RUN_TIME', description: '涡流排砂泵2运行时间', currentValue: 0 },
  { code: 'MLC_SYSTEM1_SWITCH_TIME', description: '系统1冲洗泵切换时间', currentValue: 0 },
  { code: 'MLC_SYSTEM1_WASH_TIME', description: '系统1冲洗时间', currentValue: 0 },
  { code: 'MLC_SYSTEM2_SWITCH_TIME', description: '系统2冲洗泵切换时间', currentValue: 0 },
  { code: 'MLC_SYSTEM2_WASH_TIME', description: '系统2冲洗时间', currentValue: 0 }
])

// ==================== 接触消毒池 — 数据定义 ====================

const p2Instruments = ref([
  { code: 'P2_AIT01', description: '消毒池余氯', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 }
])

const p2LogicParams = ref([
  { code: 'P2_GP1_LEVEL_DELAY_START_LEVEL', description: '消毒池中水泵 液位与延时联动启动液位', currentValue: 0, setValue: 0, paramType: 'startLevel' },
  { code: 'P2_GP1_LEVEL_DELAY_STOP_LEVEL', description: '消毒池中水泵 液位与延时联动停止液位', currentValue: 0, setValue: 0, paramType: 'stopLevel' },
  { code: 'P2_GP1_LEVEL_DELAY_DELAY_MIN', description: '消毒池中水泵 液位与延时联动延时分', currentValue: 0, setValue: 0, paramType: 'delayMinutes' },
  { code: 'P2_GP1_LEVEL_DELAY_DELAY_SEC', description: '消毒池中水泵 液位与延时联动延时秒', currentValue: 0, setValue: 0, paramType: 'delaySeconds' }
])

// ==================== AAO生物池 — 数据定义 ====================

// 仪表
const h1Instruments = ref([
  { code: 'H1_AIT101', description: '1#厌氧池混合液浓度', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT102', description: '1#厌氧池氧化还原电位', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT103', description: '1#厌氧池氨氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT104', description: '1#厌氧池硝氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT105', description: '1#缺氧池氧化还原电位', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT106', description: '1#好氧池溶解氧', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT107', description: '1#出水端混合液浓度', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT108', description: '1#出水端溶解氧', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT109', description: '1#出水端氨氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT110', description: '1#出水端硝氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT201', description: '2#厌氧池混合液浓度', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT202', description: '2#厌氧池氧化还原电位', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT203', description: '2#厌氧池氨氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT204', description: '2#厌氧池硝氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT205', description: '2#缺氧池氧化还原电位', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT206', description: '2#好氧池溶解氧', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT207', description: '2#出水端混合液浓度', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT208', description: '2#出水端溶解氧', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT209', description: '2#出水端氨氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT210', description: '2#出水端硝氮分析仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_AIT301', description: '污泥浓度计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_FIT301', description: '回流泵流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_FIT302', description: '剩余泵流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_FIT_A20', description: 'A20池进水流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_FIT_DISC_2', description: '二期转盘进水流量计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_LIT01', description: '1#污泥界面仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_LIT02', description: '2#污泥界面仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_LIT03', description: '3#污泥界面仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_LIT04', description: '4#污泥界面仪', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 },
  { code: 'H1_LIT05', description: '超声波液位计', output: 0, upperCur: 0, upper: 0, lowerCur: 0, lower: 0, devCur: 0, dev: 0 }
])

// 设备频率
const h1FreqParams = ref([
  { code: 'H1_GP5', description: '1#污泥回流泵', freqFb: 0, freqSet: 0, fbCode: 'H1_GP5_FREQ_FB', setCode: 'H1_GP5_FREQ_SET' },
  { code: 'H1_GP6', description: '2#污泥回流泵', freqFb: 0, freqSet: 0, fbCode: 'H1_GP6_FREQ_FB', setCode: 'H1_GP6_FREQ_SET' },
  { code: 'H1_GP7', description: '3#污泥回流泵', freqFb: 0, freqSet: 0, fbCode: 'H1_GP7_FREQ_FB', setCode: 'H1_GP7_FREQ_SET' },
  { code: 'H1_GP8', description: '8#污泥剩余泵', freqFb: 0, freqSet: 0, fbCode: 'H1_GP8_FREQ_FB', setCode: 'H1_GP8_FREQ_SET' },
  { code: 'H1_GP9', description: '9#污泥剩余泵', freqFb: 0, freqSet: 0, fbCode: 'H1_GP9_FREQ_FB', setCode: 'H1_GP9_FREQ_SET' }
])

// 逻辑参数
const h1LogicParams = ref([
  { code: 'H1_GP57_PROTECT_LEVEL_START_LEVEL', configCode: 'H1_GP57_PROTECT_LEVEL', description: '污泥回流泵1-3 保护液位启动液位', currentValue: 0, setValue: 0, paramType: 'startLevel' },
  { code: 'H1_GP89_LEVEL_START_LEVEL', configCode: 'H1_GP89_LEVEL', description: '8#-9#污泥剩余泵 液位联动启动液位', currentValue: 0, setValue: 0, paramType: 'startLevel' },
  { code: 'H1_GP89_LEVEL_STOP_LEVEL', configCode: 'H1_GP89_LEVEL', description: '8#-9#污泥剩余泵 液位联动停止液位', currentValue: 0, setValue: 0, paramType: 'stopLevel' }
])

// 联动参数（定时器）
const h1TimerParams = ref([
  {
    deviceName: '污水回流泵1-4', code: 'H1_TIMER_GP14',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '污泥回流泵1-3', code: 'H1_TIMER_GP57',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '选择区搅拌机1-2', code: 'H1_TIMER_HW12',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '厌氧区搅拌机1-2', code: 'H1_TIMER_HW34',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '选择区搅拌机1-4', code: 'H1_TIMER_HW58',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  },
  {
    deviceName: '曝气管阀门1/10', code: 'H1_TIMER_QM1_10',
    runtimeDisplay: '0:0:0', stoppedDisplay: '0:0:0',
    runSetDisplay: '0:0:0', runSetH: 0, runSetM: 0, runSetS: 0,
    stopSetDisplay: '0:0:0', stopSetH: 0, stopSetM: 0, stopSetS: 0
  }
])

// ==================== 数据轮询 ====================
let pollTimer = null

// 从 overview 数据中更新
function updateFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { instruments, parameters } = overviewData.data

  // 更新仪表数据
  if (instruments) {
    instruments.forEach(inst => {
      const local = b1Instruments.value.find(i => i.code === inst.code)
      if (!local) return
      local.output = inst.output ?? 0
      if (inst.upperLimit !== undefined) local.upperCur = inst.upperLimit
      if (inst.lowerLimit !== undefined) local.lowerCur = inst.lowerLimit
      if (inst.deviation !== undefined) local.devCur = inst.deviation
    })
  }

  // 更新参数数据（液位、定时器）
  if (parameters) {
    parameters.forEach(p => {
      // 液位参数
      const levelParam = b1LevelParams.value.find(lp => lp.code === p.code)
      if (levelParam) {
        levelParam.currentValue = p.value ?? 0
        return
      }
      // 定时器参数
      const timer = b1TimerParams.value[0]
      if (!timer) return
      const allTimerCodes = [
        timer.runtimeCodes.h, timer.runtimeCodes.m, timer.runtimeCodes.s,
        timer.stoppedCodes.h, timer.stoppedCodes.m, timer.stoppedCodes.s,
        timer.runSetCodes.h, timer.runSetCodes.m, timer.runSetCodes.s,
        timer.stopSetCodes.h, timer.stopSetCodes.m, timer.stopSetCodes.s
      ]
      if (allTimerCodes.includes(p.code)) {
        // 已运行时间
        if (p.code === timer.runtimeCodes.h) timer.runtimeDisplay = `${p.value ?? 0}:${extractTimerPart(timer.runtimeDisplay, 1)}:${extractTimerPart(timer.runtimeDisplay, 2)}`
        if (p.code === timer.runtimeCodes.m) timer.runtimeDisplay = `${extractTimerPart(timer.runtimeDisplay, 0)}:${p.value ?? 0}:${extractTimerPart(timer.runtimeDisplay, 2)}`
        if (p.code === timer.runtimeCodes.s) timer.runtimeDisplay = `${extractTimerPart(timer.runtimeDisplay, 0)}:${extractTimerPart(timer.runtimeDisplay, 1)}:${p.value ?? 0}`
        // 已停止时间
        if (p.code === timer.stoppedCodes.h) timer.stoppedDisplay = `${p.value ?? 0}:${extractTimerPart(timer.stoppedDisplay, 1)}:${extractTimerPart(timer.stoppedDisplay, 2)}`
        if (p.code === timer.stoppedCodes.m) timer.stoppedDisplay = `${extractTimerPart(timer.stoppedDisplay, 0)}:${p.value ?? 0}:${extractTimerPart(timer.stoppedDisplay, 2)}`
        if (p.code === timer.stoppedCodes.s) timer.stoppedDisplay = `${extractTimerPart(timer.stoppedDisplay, 0)}:${extractTimerPart(timer.stoppedDisplay, 1)}:${p.value ?? 0}`
        // 运行计时设置
        if (p.code === timer.runSetCodes.h) timer.runSetH = p.value ?? 0
        if (p.code === timer.runSetCodes.m) timer.runSetM = p.value ?? 0
        if (p.code === timer.runSetCodes.s) timer.runSetS = p.value ?? 0
        timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`
        // 停止计时设置
        if (p.code === timer.stopSetCodes.h) timer.stopSetH = p.value ?? 0
        if (p.code === timer.stopSetCodes.m) timer.stopSetM = p.value ?? 0
        if (p.code === timer.stopSetCodes.s) timer.stopSetS = p.value ?? 0
        timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`
      }
    })
  }
}

// 辅助：从 "H:M:S" 格式中提取指定位置的值
function extractTimerPart(display, index) {
  const parts = (display || '0:0:0').split(':')
  return parts[index] ?? '0'
}

// 从 overview 数据中更新压滤机加药装置参数
function updateBjdFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { parameters } = overviewData.data
  if (!parameters) return
  parameters.forEach(p => {
    const local = bjdParams.value.find(item => item.code === p.code)
    if (!local) return
    local.currentValue = p.value ?? 0
  })
}

// 从 overview 数据中更新滤布滤池转盘参数
function updateBfdFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { parameters } = overviewData.data
  if (!parameters) return
  parameters.forEach(p => {
    const local = bfdParams.value.find(item => item.code === p.code)
    if (!local) return
    local.currentValue = p.value ?? 0
  })
}

// 从 overview 数据中更新压滤机参数
function updateBfpFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { parameters } = overviewData.data
  if (!parameters) return
  parameters.forEach(p => {
    const local = bfpParams.value.find(item => item.code === p.code)
    if (!local) return
    local.currentValue = p.value ?? 0
  })
}

// 从 overview 数据中更新高效沉淀池
function updateM3FromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { devices, instruments, parameters } = overviewData.data

  // 设备频率属于 devices 字段，不在通用 parameters 列表中。
  if (devices) {
    devices.forEach(device => {
      const local = m3FreqParams.value.find(item => item.code === device.code)
      if (!local) return
      local.freqFb = device.frequencyFeedback ?? 0
      if (device.frequencySetpoint !== undefined) local.freqSet = device.frequencySetpoint ?? 0
    })
  }

  // 更新仪表
  if (instruments) {
    instruments.forEach(inst => {
      // 高效沉淀池仪表
      const local = m3Instruments.value.find(i => i.code === inst.code)
      if (local) {
        local.output = inst.output ?? 0
        if (inst.upperLimit !== undefined) local.upperCur = inst.upperLimit
        if (inst.lowerLimit !== undefined) local.lowerCur = inst.lowerLimit
        if (inst.deviation !== undefined) local.devCur = inst.deviation
        return
      }
      // 接触消毒池仪表（P2 开头，共用 M3 PLC）
      const p2Inst = p2Instruments.value.find(i => i.code === inst.code)
      if (p2Inst) {
        p2Inst.output = inst.output ?? 0
        if (inst.upperLimit !== undefined) p2Inst.upperCur = inst.upperLimit
        if (inst.lowerLimit !== undefined) p2Inst.lowerCur = inst.lowerLimit
        if (inst.deviation !== undefined) p2Inst.devCur = inst.deviation
      }
    })
  }

  // 更新参数（设备频率、逻辑参数、定时器）
  if (parameters) {
    parameters.forEach(p => {
      // 设备频率反馈
      const freqParam = m3FreqParams.value.find(fp => fp.fbCode === p.code || fp.setCode === p.code)
      if (freqParam) {
        if (freqParam.fbCode === p.code) freqParam.freqFb = p.value ?? 0
        if (freqParam.setCode === p.code) freqParam.freqSet = p.value ?? 0
        return
      }

      // 高效沉淀池逻辑参数
      const logicParam = m3LogicParams.value.find(lp => lp.code === p.code)
      if (logicParam) { logicParam.currentValue = p.value ?? 0; return }

      // 接触消毒池逻辑参数
      const p2Logic = p2LogicParams.value.find(lp => lp.code === p.code)
      if (p2Logic) { p2Logic.currentValue = p.value ?? 0; return }

      // 定时器参数
      for (const timer of m3TimerParams.value) {
        const prefix = timer.code
        if (p.code === `${prefix}_RUN_ELAPSED_H`) { timer.runtimeDisplay = `${p.value ?? 0}:${extractTimerPart(timer.runtimeDisplay, 1)}:${extractTimerPart(timer.runtimeDisplay, 2)}`; return }
        if (p.code === `${prefix}_RUN_ELAPSED_M`) { timer.runtimeDisplay = `${extractTimerPart(timer.runtimeDisplay, 0)}:${p.value ?? 0}:${extractTimerPart(timer.runtimeDisplay, 2)}`; return }
        if (p.code === `${prefix}_RUN_ELAPSED_S`) { timer.runtimeDisplay = `${extractTimerPart(timer.runtimeDisplay, 0)}:${extractTimerPart(timer.runtimeDisplay, 1)}:${p.value ?? 0}`; return }
        if (p.code === `${prefix}_STOP_ELAPSED_H`) { timer.stoppedDisplay = `${p.value ?? 0}:${extractTimerPart(timer.stoppedDisplay, 1)}:${extractTimerPart(timer.stoppedDisplay, 2)}`; return }
        if (p.code === `${prefix}_STOP_ELAPSED_M`) { timer.stoppedDisplay = `${extractTimerPart(timer.stoppedDisplay, 0)}:${p.value ?? 0}:${extractTimerPart(timer.stoppedDisplay, 2)}`; return }
        if (p.code === `${prefix}_STOP_ELAPSED_S`) { timer.stoppedDisplay = `${extractTimerPart(timer.stoppedDisplay, 0)}:${extractTimerPart(timer.stoppedDisplay, 1)}:${p.value ?? 0}`; return }
        if (p.code === `${prefix}_RUN_SET_H`) { timer.runSetH = p.value ?? 0; timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`; return }
        if (p.code === `${prefix}_RUN_SET_M`) { timer.runSetM = p.value ?? 0; timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`; return }
        if (p.code === `${prefix}_RUN_SET_S`) { timer.runSetS = p.value ?? 0; timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`; return }
        if (p.code === `${prefix}_STOP_SET_H`) { timer.stopSetH = p.value ?? 0; timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`; return }
        if (p.code === `${prefix}_STOP_SET_M`) { timer.stopSetM = p.value ?? 0; timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`; return }
        if (p.code === `${prefix}_STOP_SET_S`) { timer.stopSetS = p.value ?? 0; timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`; return }
      }
    })
  }
}

// 从 overview 数据中更新除臭设备参数
function updateDeoFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { parameters } = overviewData.data
  if (!parameters) return
  parameters.forEach(p => {
    const local = deoControlParams.value.find(item => item.code === p.code)
    if (local) { local.currentValue = p.value ?? 0 }
  })
}

// 从 overview 数据中更新细格栅及旋流沉砂池参数
function updateMlcFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { parameters } = overviewData.data
  if (!parameters) return
  parameters.forEach(p => {
    const local = mlcParams.value.find(item => item.code === p.code)
    if (!local) return
    local.currentValue = p.value ?? 0
  })
}

// 从 overview 数据中更新鼓风机房参数
function updateFanFromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { parameters } = overviewData.data
  if (!parameters) return
  parameters.forEach(p => {
    const local = fanParams.value.find(item => item.code === p.code)
    if (!local) return
    local.currentValue = p.value ?? 0
  })
}

// 从 overview 数据中更新AAO生物池数据
function updateH1FromOverview(overviewData) {
  if (!overviewData?.success || !overviewData?.data) return
  const { devices, instruments, parameters } = overviewData.data

  // 设备频率属于 devices 字段，不在通用 parameters 列表中。
  if (devices) {
    devices.forEach(device => {
      const local = h1FreqParams.value.find(item => item.code === device.code)
      if (!local) return
      local.freqFb = device.frequencyFeedback ?? 0
      if (device.frequencySetpoint !== undefined) local.freqSet = device.frequencySetpoint ?? 0
    })
  }

  // 更新仪表
  if (instruments) {
    instruments.forEach(inst => {
      // AAO生物池仪表
      const local = h1Instruments.value.find(i => i.code === inst.code)
      if (local) {
        local.output = inst.output ?? 0
        if (inst.upperLimit !== undefined) local.upperCur = inst.upperLimit
        if (inst.lowerLimit !== undefined) local.lowerCur = inst.lowerLimit
        if (inst.deviation !== undefined) local.devCur = inst.deviation
        return
      }
      // 鼓风机房仪表（H6 开头，共用 H1 PLC）
      const fanInst = fanInstruments.value.find(i => i.code === inst.code)
      if (fanInst) {
        fanInst.output = inst.output ?? 0
        if (inst.upperLimit !== undefined) fanInst.upperCur = inst.upperLimit
        if (inst.lowerLimit !== undefined) fanInst.lowerCur = inst.lowerLimit
        if (inst.deviation !== undefined) fanInst.devCur = inst.deviation
      }
    })
  }

  // 更新参数（设备频率、逻辑参数、定时器）
  if (parameters) {
    parameters.forEach(p => {
      // 设备频率反馈
      const freqParam = h1FreqParams.value.find(fp => fp.fbCode === p.code || fp.setCode === p.code)
      if (freqParam) {
        if (freqParam.fbCode === p.code) freqParam.freqFb = p.value ?? 0
        if (freqParam.setCode === p.code) freqParam.freqSet = p.value ?? 0
        return
      }

      // 逻辑参数
      const logicParam = h1LogicParams.value.find(lp => lp.code === p.code)
      if (logicParam) { logicParam.currentValue = p.value ?? 0; return }

      // 定时器参数
      for (const timer of h1TimerParams.value) {
        const prefix = timer.code
        if (p.code === `${prefix}_RUN_ELAPSED_H`) { timer.runtimeDisplay = `${p.value ?? 0}:${extractTimerPart(timer.runtimeDisplay, 1)}:${extractTimerPart(timer.runtimeDisplay, 2)}`; return }
        if (p.code === `${prefix}_RUN_ELAPSED_M`) { timer.runtimeDisplay = `${extractTimerPart(timer.runtimeDisplay, 0)}:${p.value ?? 0}:${extractTimerPart(timer.runtimeDisplay, 2)}`; return }
        if (p.code === `${prefix}_RUN_ELAPSED_S`) { timer.runtimeDisplay = `${extractTimerPart(timer.runtimeDisplay, 0)}:${extractTimerPart(timer.runtimeDisplay, 1)}:${p.value ?? 0}`; return }
        if (p.code === `${prefix}_STOP_ELAPSED_H`) { timer.stoppedDisplay = `${p.value ?? 0}:${extractTimerPart(timer.stoppedDisplay, 1)}:${extractTimerPart(timer.stoppedDisplay, 2)}`; return }
        if (p.code === `${prefix}_STOP_ELAPSED_M`) { timer.stoppedDisplay = `${extractTimerPart(timer.stoppedDisplay, 0)}:${p.value ?? 0}:${extractTimerPart(timer.stoppedDisplay, 2)}`; return }
        if (p.code === `${prefix}_STOP_ELAPSED_S`) { timer.stoppedDisplay = `${extractTimerPart(timer.stoppedDisplay, 0)}:${extractTimerPart(timer.stoppedDisplay, 1)}:${p.value ?? 0}`; return }
        if (p.code === `${prefix}_RUN_SET_H`) { timer.runSetH = p.value ?? 0; timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`; return }
        if (p.code === `${prefix}_RUN_SET_M`) { timer.runSetM = p.value ?? 0; timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`; return }
        if (p.code === `${prefix}_RUN_SET_S`) { timer.runSetS = p.value ?? 0; timer.runSetDisplay = `${timer.runSetH}:${timer.runSetM}:${timer.runSetS}`; return }
        if (p.code === `${prefix}_STOP_SET_H`) { timer.stopSetH = p.value ?? 0; timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`; return }
        if (p.code === `${prefix}_STOP_SET_M`) { timer.stopSetM = p.value ?? 0; timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`; return }
        if (p.code === `${prefix}_STOP_SET_S`) { timer.stopSetS = p.value ?? 0; timer.stopSetDisplay = `${timer.stopSetH}:${timer.stopSetM}:${timer.stopSetS}`; return }
      }
    })
  }
}

// 获取 B1 PLC 数据
async function fetchAllData() {
  try {
    const res = await request.get('/plc/b1/overview')
    updateFromOverview(res.data)
  } catch (e) {
    console.error('获取 B1 PLC 数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/baike-dosing/overview')
    updateBjdFromOverview(res.data)
  } catch (e) {
    console.error('获取压滤机加药装置数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/baike-fiber-disc/overview')
    updateBfdFromOverview(res.data)
  } catch (e) {
    console.error('获取滤布滤池转盘数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/baike-filter-press/overview')
    updateBfpFromOverview(res.data)
  } catch (e) {
    console.error('获取压滤机数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/fan/overview')
    updateFanFromOverview(res.data)
  } catch (e) {
    console.error('获取鼓风机房数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/h1/overview')
    updateH1FromOverview(res.data)
  } catch (e) {
    console.error('获取AAO生物池数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/m3/overview')
    updateM3FromOverview(res.data)
  } catch (e) {
    console.error('获取高效沉淀池数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/multilayer-cyclone/overview')
    updateMlcFromOverview(res.data)
  } catch (e) {
    console.error('获取细格栅及旋流沉砂池数据失败:', e.message)
  }
  try {
    const res = await request.get('/plc/deodorization/overview')
    updateDeoFromOverview(res.data)
  } catch (e) {
    console.error('获取除臭设备数据失败:', e.message)
  }
}

// ==================== 参数写入（调用后端 B1PlcController） ====================

// 参数写入 → POST /api/plc/b1/parameters/{parameterCode}
const onParameterWrite = async (code, value) => {
  try {
    await request.post(`/plc/b1/parameters/${code}`, null, { params: { value } })
    ElMessage.success(`${code} - 参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('参数写入失败:', e)
    ElMessage.error(`${code} - 参数下发失败`)
  }
}

// 定时器运行计时写入 → POST /api/plc/b1/parameters/{parameterCode}（逐个写入 H/M/S）
const onTimerWrite = async (row) => {
  try {
    await request.post(`/plc/b1/parameters/${row.runSetCodes.h}`, null, { params: { value: row.runSetH } })
    await request.post(`/plc/b1/parameters/${row.runSetCodes.m}`, null, { params: { value: row.runSetM } })
    await request.post(`/plc/b1/parameters/${row.runSetCodes.s}`, null, { params: { value: row.runSetS } })
    ElMessage.success('运行计时参数已下发')
    fetchAllData()
  } catch (e) {
    console.error('运行计时写入失败:', e)
    ElMessage.error('运行计时下发失败')
  }
}

// 定时器停止计时写入 → POST /api/plc/b1/parameters/{parameterCode}（逐个写入 H/M/S）
const onTimerStopWrite = async (row) => {
  try {
    await request.post(`/plc/b1/parameters/${row.stopSetCodes.h}`, null, { params: { value: row.stopSetH } })
    await request.post(`/plc/b1/parameters/${row.stopSetCodes.m}`, null, { params: { value: row.stopSetM } })
    await request.post(`/plc/b1/parameters/${row.stopSetCodes.s}`, null, { params: { value: row.stopSetS } })
    ElMessage.success('停止计时参数已下发')
    fetchAllData()
  } catch (e) {
    console.error('停止计时写入失败:', e)
    ElMessage.error('停止计时下发失败')
  }
}

// AAO生物池仪表写入 → POST /api/plc/h1/instruments/{instrumentCode}
const onH1InstrumentWrite = async (row) => {
  try {
    await request.post(`/plc/h1/instruments/${row.code}`, null, {
      params: { upperLimit: row.upper, lowerLimit: row.lower, deviation: row.dev }
    })
    ElMessage.success(`${row.code} - 仪表参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('H1仪表参数写入失败:', e)
    ElMessage.error(`${row.code} - 仪表参数下发失败`)
  }
}

// AAO生物池设备频率写入 → POST /api/plc/h1/devices/{deviceCode}/frequency
const onH1FreqWrite = async (row) => {
  try {
    await request.post(`/plc/h1/devices/${row.code}/frequency`, null, { params: { value: row.freqSet } })
    ElMessage.success(`${row.code} - 频率 ${row.freqSet}Hz 已下发`)
    fetchAllData()
  } catch (e) {
    console.error('H1频率写入失败:', e)
    ElMessage.error(`${row.code} - 频率下发失败`)
  }
}

// AAO生物池逻辑参数写入 → POST /api/plc/h1/logic-configs/{configCode}
const onH1LogicWrite = async (row) => {
  try {
    const params = {}
    if (row.paramType === 'startLevel') params.startLevel = row.setValue
    if (row.paramType === 'stopLevel') params.stopLevel = row.setValue
    await request.post(`/plc/h1/logic-configs/${row.configCode}`, null, { params })
    ElMessage.success(`${row.code} - 逻辑参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('H1逻辑参数写入失败:', e)
    ElMessage.error(`${row.code} - 逻辑参数下发失败`)
  }
}

// AAO生物池定时器写入 → POST /api/plc/h1/timers/{timerCode}
const onH1TimerWrite = async (row) => {
  try {
    await request.post(`/plc/h1/timers/${row.code}`, null, {
      params: {
        runHours: row.runSetH, runMinutes: row.runSetM, runSeconds: row.runSetS,
        stopHours: row.stopSetH, stopMinutes: row.stopSetM, stopSeconds: row.stopSetS
      }
    })
    ElMessage.success(`${row.code} - 定时参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('H1定时器写入失败:', e)
    ElMessage.error(`${row.code} - 定时参数下发失败`)
  }
}

// 接触消毒池仪表写入 → POST /api/plc/m3/instruments/{instrumentCode}
const onP2InstrumentWrite = async (row) => {
  try {
    await request.post(`/plc/m3/instruments/${row.code}`, null, {
      params: { upperLimit: row.upper, lowerLimit: row.lower, deviation: row.dev }
    })
    ElMessage.success(`${row.code} - 仪表参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('接触消毒池仪表写入失败:', e)
    ElMessage.error(`${row.code} - 仪表参数下发失败`)
  }
}

// 接触消毒池逻辑参数写入 → POST /api/plc/m3/logic-configs/{configCode}
const onP2LogicWrite = async () => {
  try {
    const params = {}
    for (const item of p2LogicParams.value) {
      if (item.paramType === 'startLevel') params.startLevel = item.setValue
      if (item.paramType === 'stopLevel') params.stopLevel = item.setValue
      if (item.paramType === 'delayMinutes') params.delayMinutes = item.setValue
      if (item.paramType === 'delaySeconds') params.delaySeconds = item.setValue
    }
    await request.post('/plc/m3/logic-configs/P2_GP1_LEVEL_DELAY', null, { params })
    ElMessage.success('P2_GP1_LEVEL_DELAY - 逻辑参数已下发')
    fetchAllData()
  } catch (e) {
    console.error('接触消毒池逻辑参数写入失败:', e)
    ElMessage.error('逻辑参数下发失败')
  }
}

// 高效沉淀池设备频率写入 → POST /api/plc/m3/devices/{deviceCode}/frequency
const onM3FreqWrite = async (row) => {
  try {
    await request.post(`/plc/m3/devices/${row.code}/frequency`, null, { params: { value: row.freqSet } })
    ElMessage.success(`${row.code} - 频率 ${row.freqSet}Hz 已下发`)
    fetchAllData()
  } catch (e) {
    console.error('M3频率写入失败:', e)
    ElMessage.error(`${row.code} - 频率下发失败`)
  }
}

// 高效沉淀池逻辑参数写入 → POST /api/plc/m3/logic-configs/{configCode}
const onM3LogicWrite = async (row) => {
  try {
    const params = {}
    if (row.paramType === 'startLevel') params.startLevel = row.setValue
    if (row.paramType === 'stopLevel') params.stopLevel = row.setValue
    await request.post(`/plc/m3/logic-configs/${row.configCode}`, null, { params })
    ElMessage.success(`${row.code} - 逻辑参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('M3逻辑参数写入失败:', e)
    ElMessage.error(`${row.code} - 逻辑参数下发失败`)
  }
}

// 高效沉淀池定时器写入 → POST /api/plc/m3/timers/{timerCode}
const onM3TimerWrite = async (row) => {
  try {
    await request.post(`/plc/m3/timers/${row.code}`, null, {
      params: {
        runHours: row.runSetH, runMinutes: row.runSetM, runSeconds: row.runSetS,
        stopHours: row.stopSetH, stopMinutes: row.stopSetM, stopSeconds: row.stopSetS
      }
    })
    ElMessage.success(`${row.code} - 定时参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('M3定时器写入失败:', e)
    ElMessage.error(`${row.code} - 定时参数下发失败`)
  }
}

// 高效沉淀池仪表写入 → POST /api/plc/m3/instruments/{instrumentCode}
const onM3InstrumentWrite = async (row) => {
  try {
    await request.post(`/plc/m3/instruments/${row.code}`, null, {
      params: { upperLimit: row.upper, lowerLimit: row.lower, deviation: row.dev }
    })
    ElMessage.success(`${row.code} - 仪表参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('高效沉淀池仪表写入失败:', e)
    ElMessage.error(`${row.code} - 仪表参数下发失败`)
  }
}

// 鼓风机房仪表写入 → POST /api/plc/h1/instruments/{instrumentCode}（H6仪表共用H1 PLC）
const onFanInstrumentWrite = async (row) => {
  try {
    await request.post(`/plc/h1/instruments/${row.code}`, null, {
      params: { upperLimit: row.upper, lowerLimit: row.lower, deviation: row.dev }
    })
    ElMessage.success(`${row.code} - 仪表参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('鼓风机房仪表写入失败:', e)
    ElMessage.error(`${row.code} - 仪表参数下发失败`)
  }
}

// 除臭设备参数写入 → POST /api/plc/deodorization/parameters/{parameterCode}
const onDeoParameterWrite = async (code, value) => {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) return ElMessage.warning('请输入有效的参数值')
  if (code === 'DEO_FAN_FREQ_SET_VD472' && !isDeodorizationFanFrequencyInRange(numericValue)) {
    return ElMessage.warning(`除臭风机频率给定范围为 ${DEO_FAN_FREQUENCY_MIN}-${DEO_FAN_FREQUENCY_MAX}Hz`)
  }
  const isDelay = code === 'DEO_SYSTEM_START_DELAY_VW530' || code === 'DEO_SYSTEM_STOP_DELAY_VW532'
  if (isDelay && (!Number.isInteger(numericValue) || numericValue < 0 || numericValue > 32767)) {
    return ElMessage.warning('启停延时必须是0-32767之间的整数')
  }
  try {
    await request.post(`/plc/deodorization/parameters/${code}`, null, { params: { value: numericValue } })
    ElMessage.success(`${code} - 参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('除臭设备参数写入失败:', e)
    ElMessage.error(`${code} - 参数下发失败`)
  }
}

// 鼓风机房参数写入 → POST /api/plc/fan/parameters/{parameterCode}
const onFanParameterWrite = async (code, value) => {
  if (!isFanFrequencyInRange(value)) {
    return ElMessage.warning(`风机频率给定范围为 ${FAN_FREQUENCY_MIN}-${FAN_FREQUENCY_MAX}Hz`)
  }
  try {
    await request.post(`/plc/fan/parameters/${code}`, null, { params: { value } })
    ElMessage.success(`${code} - 参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('鼓风机房参数写入失败:', e)
    ElMessage.error(`${code} - 参数下发失败`)
  }
}

// 压滤机参数写入 → POST /api/plc/baike-filter-press/parameters/{parameterCode}
const onBfpParameterWrite = async (code, value) => {
  try {
    await request.post(`/plc/baike-filter-press/parameters/${code}`, null, { params: { value } })
    ElMessage.success(`${code} - 参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('压滤机参数写入失败:', e)
    ElMessage.error(`${code} - 参数下发失败`)
  }
}

// 滤布滤池转盘参数写入 → POST /api/plc/baike-fiber-disc/parameters/{parameterCode}
const onBfdParameterWrite = async (code, value) => {
  try {
    await request.post(`/plc/baike-fiber-disc/parameters/${code}`, null, { params: { value } })
    ElMessage.success(`${code} - 参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('滤布滤池转盘参数写入失败:', e)
    ElMessage.error(`${code} - 参数下发失败`)
  }
}

// 压滤机加药装置参数写入 → POST /api/plc/baike-dosing/parameters/{parameterCode}
const onBjdParameterWrite = async (code, value) => {
  try {
    await request.post(`/plc/baike-dosing/parameters/${code}`, null, { params: { value } })
    ElMessage.success(`${code} - 参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('压滤机加药装置参数写入失败:', e)
    ElMessage.error(`${code} - 参数下发失败`)
  }
}

// 仪表参数写入 → POST /api/plc/b1/instruments/{instrumentCode}
const onInstrumentWrite = async (row) => {
  try {
    await request.post(`/plc/b1/instruments/${row.code}`, null, {
      params: { upperLimit: row.upper, lowerLimit: row.lower, deviation: row.dev }
    })
    ElMessage.success(`${row.code} - 仪表参数已下发`)
    fetchAllData()
  } catch (e) {
    console.error('仪表参数写入失败:', e)
    ElMessage.error(`${row.code} - 仪表参数下发失败`)
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchAllData()
  pollTimer = setInterval(fetchAllData, 5000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.params-page {
  width: 100%;
  box-sizing: border-box;
  padding: 24px 28px 48px;
  background: linear-gradient(180deg, #060d1f 0%, #0a1628 40%, #0d1e36 100%);
  position: relative;
  overflow-x: hidden;
}

.wave-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(ellipse 120% 60% at 50% 100%, rgba(0,128,255,0.06), transparent 70%),
    radial-gradient(ellipse 80% 40% at 20% 80%, rgba(0,232,255,0.04), transparent 60%);
}

.page-header {
  position: relative;
  z-index: 1;
  margin-bottom: 22px;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-dot {
  width: 9px;
  height: 9px;
  background: var(--color-primary);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--color-primary);
}

.header-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: 0;
}

.header-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(0,128,255,0.4), transparent);
}

.header-sub {
  font-size: 0.8125rem;
  color: rgba(186,225,245,0.78);
  letter-spacing: 0;
  font-family: var(--font-display);
}

.cards-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  gap: 20px;
}

.cards-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-card {
  min-width: 0;
  padding: 18px;
  position: relative;
  z-index: 1;
}

.frame-card {
  position: relative;
  background: linear-gradient(135deg, rgba(0,20,60,0.8), rgba(0,30,80,0.6));
  border: 1px solid rgba(63,190,255,0.28);
  border-radius: 4px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.025), 0 8px 24px rgba(0,0,0,0.12);
}

.frame-corner {
  position: absolute;
  width: 10px;
  height: 10px;
  border-color: rgba(0,232,255,0.6);
  border-style: solid;
}

.frame-corner.tl { top: -1px; left: -1px; border-width: 2px 0 0 2px; }
.frame-corner.tr { top: -1px; right: -1px; border-width: 2px 2px 0 0; }
.frame-corner.bl { bottom: -1px; left: -1px; border-width: 0 0 2px 2px; }
.frame-corner.br { bottom: -1px; right: -1px; border-width: 0 2px 2px 0; }

.card-header-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding-bottom: 12px;
  font-size: 1.375rem;
  font-weight: 800;
  color: #79efff;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(93,225,255,0.42);
  letter-spacing: 0;
  text-shadow: 0 0 12px rgba(65,224,255,0.38);
}

.sub-title {
  display: flex;
  align-items: center;
  min-height: 24px;
  margin: 12px 0 8px;
  padding: 5px 10px;
  border-left: 3px solid #4de2ff;
  background: linear-gradient(90deg, rgba(50,190,235,0.12), rgba(50,190,235,0.015));
  color: #c8f8ff;
  font-size: 1.0625rem;
  font-weight: 800;
  line-height: 1.4;
  text-shadow: 0 0 8px rgba(77,226,255,0.28);
}

.pulse-dot {
  width: 9px;
  height: 9px;
  background: #8af4ff;
  border-radius: 50%;
  box-shadow: 0 0 9px #35eaff;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.table-gap {
  height: 8px;
}

/* Cell stack: current value + input */
.cell-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 3px 0;
}

.cell-cur {
  font-size: 0.8125rem;
  color: rgba(200,225,240,0.72);
  font-family: var(--font-data);
  line-height: 1.2;
}

/* Dark table */
:deep(.dark-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(7,34,67,0.96);
  --el-table-row-hover-bg-color: rgba(0,128,255,0.1);
  --el-table-border-color: rgba(0,128,255,0.12);
  --el-table-text-color: #fff;
  --el-table-header-text-color: #ffd36a;
  width: 100%;
  border-radius: 3px;
  overflow: hidden;
}

:deep(.el-table__row.el-table__row--striped td.el-table__cell) {
  background: rgba(0,20,60,0.3) !important;
}

:deep(.el-table .cell) {
  padding: 0 7px !important;
  font-size: 0.875rem !important;
  line-height: 1.45 !important;
}

:deep(.el-table td, .el-table th) {
  padding: 7px 0 !important;
}

:deep(.dark-table .el-table__header-wrapper th.el-table__cell) {
  background: rgba(7,34,67,0.96) !important;
  border-bottom-color: rgba(255,211,106,0.42) !important;
}

:deep(.dark-table .el-table__header-wrapper th .cell) {
  color: #ffe38a !important;
  font-size: 1rem !important;
  font-weight: 800 !important;
  line-height: 1.4 !important;
  text-shadow: 0 0 10px rgba(255,220,105,0.32);
}

:deep(.dark-table .el-table__body-wrapper td:first-child .cell) {
  color: #e6f7ff;
  font-weight: 600;
}

/* Read-only value */
.val-ro {
  color: #f3fbff;
  font-family: var(--font-data);
  font-size: 0.875rem;
  font-weight: 600;
}

.val-on {
  color: #00e8ff;
}

.val-err {
  color: #ff4d4f;
  font-weight: bold;
}

.val-dash {
  color: rgba(255,255,255,0.2);
  font-size: 0.875rem;
}

/* Time inputs inline */
.time-inputs {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.time-sep {
  color: rgba(255,255,255,0.5);
  font-size: 0.875rem;
  margin: 0 1px;
}

/* Input number compact */
:deep(.el-input-number--small) {
  width: 92px !important;
}

:deep(.el-input-number .el-input__wrapper) {
  background: rgba(0,20,60,0.6) !important;
  box-shadow: 0 0 0 1px rgba(70,190,240,0.34) inset !important;
  padding: 0 22px 0 6px !important;
}

:deep(.el-input-number .el-input__inner) {
  color: #fff !important;
  text-align: center;
  font-size: 0.875rem !important;
  height: 28px !important;
  line-height: 28px !important;
}

:deep(.el-input-number .el-input-number__decrease),
:deep(.el-input-number .el-input-number__increase) {
  width: 20px !important;
  background: transparent !important;
  color: rgba(255,255,255,0.5) !important;
  border: none !important;
}

:deep(.time-inputs .el-input-number) {
  width: 46px !important;
}

:deep(.time-inputs .el-input__wrapper) {
  padding: 0 16px 0 3px !important;
}

:deep(.time-inputs .el-input-number__decrease),
:deep(.time-inputs .el-input-number__increase) {
  width: 14px !important;
}

:deep(.dark-table .el-button.is-link) {
  min-height: 28px;
  padding: 3px 6px;
  color: #65e6ff;
  font-size: 0.875rem;
  font-weight: 700;
}

:deep(.dark-table .el-button + .el-button) {
  margin-left: 0;
}

:deep(.dark-table .el-button.is-link:hover) {
  color: #fff0ae;
}

@media (max-width: 1400px) {
  .params-page { padding: 20px 18px 40px; }
  .cards-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .params-page { padding: 16px 12px 32px; }
  .header-sub, .header-line { display: none; }
  .header-title { font-size: 1.5rem; }
  .section-card { padding: 14px; overflow-x: auto; }
}
</style>
