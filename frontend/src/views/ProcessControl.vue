<template>
  <div class="process-control-page">
    <div class="page-header">
      <div class="header-title-group">
        <span class="header-dot"></span>
        <h2 class="header-title">工艺总控</h2>
        <span class="header-line"></span>
      </div>
    </div>

    <div class="columns-container">

      <!-- 1. 调节池及提升泵房 B1 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">调节池及提升泵房</div></div>
        <div class="device-list">
          <div v-for="d in b1Devices" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button v-if="d.readOnly" class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
              <button v-else-if="d.code.startsWith('CS')" class="btn-toggle" :disabled="!canControlScreen(d)" @click="sendSmartCmd(d.code)">{{ d.run?'停止':'启动' }}</button>
              <button v-else-if="d.type==='valve'" class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.open?'CLOSE':'OPEN')">{{ d.open?'关':'开' }}</button>
              <button v-else class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.run?'STOP':'START')">{{ d.run?'停止':'启动' }}</button>
            </div>
            <div v-if="d.remote !== undefined || d.manualMode !== undefined" class="card-status-bar">
              <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
              <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
              <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'open') ? 'tag-position-on' : 'tag-position-off'">开到位</span>
              <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'close') ? 'tag-position-on' : 'tag-position-off'">关到位</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 旋流沉砂池 X1 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">旋流沉砂池</div></div>
        <div class="device-list">
          <div v-for="d in x1Devices" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button v-if="d.readOnly" class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
              <button v-else class="btn-toggle" :disabled="!canControlScreen(d)" @click="sendSmartCmd(d.code)">{{ d.run?'停止':'启动' }}</button>
            </div>
            <div v-if="!d.readOnly && (d.remote !== undefined || d.manualMode !== undefined)" class="card-status-bar">
              <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
              <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 高效沉淀池 M3 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">高效沉淀池</div></div>
        <div class="device-list">
          <div v-for="d in m3Devices" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button v-if="d.type==='valve'" class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.open?'CLOSE':'OPEN')">{{ d.open?'关':'开' }}</button>
              <button v-else class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.run?'STOP':'START')">{{ d.run?'停止':'启动' }}</button>
            </div>
            <div v-if="d.remote !== undefined || d.manualMode !== undefined" class="card-status-bar">
              <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
              <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
              <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'open') ? 'tag-position-on' : 'tag-position-off'">开到位</span>
              <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'close') ? 'tag-position-on' : 'tag-position-off'">关到位</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AAO生物池 阀门 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">AAO生物池阀门</div></div>
        <div class="device-list">
          <div v-for="d in h1DevicesLeft" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.open?'CLOSE':'OPEN')">{{ d.open?'关':'开' }}</button>
            </div>
            <div v-if="d.remote !== undefined || d.manualMode !== undefined" class="card-status-bar">
              <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
              <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
              <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'open') ? 'tag-position-on' : 'tag-position-off'">开到位</span>
              <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'close') ? 'tag-position-on' : 'tag-position-off'">关到位</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AAO生物池 搅拌机 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">AAO生物池搅拌机</div></div>
        <div class="device-list">
          <div v-for="d in h1Mixers" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.run?'STOP':'START')">{{ d.run?'停止':'启动' }}</button>
            </div>
            <div v-if="d.remote !== undefined || d.manualMode !== undefined" class="card-status-bar">
              <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
              <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AAO生物池 泵 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">AAO生物池泵</div></div>
        <div class="device-list">
          <div v-for="d in h1DevicesRight" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.run?'STOP':'START')">{{ d.run?'停止':'启动' }}</button>
            </div>
            <div v-if="d.remote !== undefined || d.manualMode !== undefined" class="card-status-bar">
              <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
              <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 鼓风机房 + 消毒池 + 脱水车间（合并栏） -->
      <div class="zone-column merged-column">
        <div class="column-header merged-header">
          <div class="col-title">鼓风机房 / 消毒池 / 脱水车间</div>
        </div>
        <div class="merged-section">
          <div class="merged-section-title">鼓风机房</div>
          <div class="device-list">
            <div v-for="d in fanDevices" :key="d.code" class="device-card" :class="cardClass(d)">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <div class="card-btn-group">
                <button class="btn-toggle btn-start" :disabled="d.run || d.fault" @click="sendFanCommand(d, true)">启动</button>
                <button class="btn-toggle btn-stop" @click="sendFanCommand(d, false)">停止</button>
              </div>
            </div>
          </div>
        </div>
        <div class="merged-section">
          <div class="merged-section-title">消毒池</div>
          <div class="device-list">
            <div v-for="d in p2Devices" :key="d.code" class="device-card" :class="cardClass(d)">
              <div class="card-main">
                <span class="card-name">{{ d.name }}</span>
                <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
                <button v-if="d.type==='valve'" class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.open?'CLOSE':'OPEN')">{{ d.open?'关':'开' }}</button>
                <button v-else class="btn-toggle" :disabled="!canControl(d)" @click="sendM3Cmd(d.code, d.run?'STOP':'START')">{{ d.run?'停止':'启动' }}</button>
              </div>
              <div v-if="d.remote !== undefined || d.manualMode !== undefined" class="card-status-bar">
                <span v-if="d.remote !== undefined" class="status-tag" :class="d.remote ? 'tag-on' : 'tag-off'">{{ d.remote ? '远程' : '就地' }}</span>
                <span v-if="d.manualMode !== undefined" class="status-tag clickable" :class="d.manualMode ? 'tag-manual' : 'tag-auto'" @click="toggleManualMode(d)">{{ d.manualMode ? '手动' : '自动' }}</span>
                <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'open') ? 'tag-position-on' : 'tag-position-off'">开到位</span>
                <span v-if="hasValvePositionFeedback(d)" class="status-tag" :class="isValvePositionActive(d, 'close') ? 'tag-position-on' : 'tag-position-off'">关到位</span>
              </div>
            </div>
          </div>
        </div>
        <div class="merged-section">
          <div class="merged-section-title">脱水车间</div>
          <div class="device-list">
            <div v-for="d in dewaterDevices" :key="d.code" class="device-card standby">
              <span class="card-name">{{ d.name }}</span>
              <div class="card-btn-group">
                <button class="btn-toggle btn-start" @click="sendBfpParam(d.startParam)">启动</button>
                <button class="btn-toggle btn-stop" @click="sendBfpParam(d.stopParam)">停止</button>
              </div>
            </div>
          </div>
        </div>
        <div class="merged-section deodorization-section">
          <div class="merged-section-title">
            <span>除臭系统</span>
            <span class="section-read-state" :class="deoControlFresh ? 'online' : 'offline'">
              {{ deoControlFresh ? '数据正常' : '数据未读取' }}
            </span>
          </div>
          <div class="device-list deodorization-control-list">
            <div class="device-card" :class="deoSystemOneKeyActive ? 'running' : 'standby'">
              <div class="card-main">
                <span class="card-name">除臭系统一键启停</span>
                <button
                  class="btn-toggle"
                  :class="deoSystemOneKeyActive ? 'btn-stop' : 'btn-start'"
                  :disabled="role !== 'admin' || deoControlBusy.oneKey || !deoControlFresh || (!deoSystemOneKeyActive && (!deoSystemAutoMode || deoEstopIndicatorActive))"
                  @click="toggleDeoOneKey"
                >
                  {{ deoSystemOneKeyActive ? '一键停止' : '一键启动' }}
                </button>
              </div>
              <div class="card-status-bar">
                <span class="status-tag" :class="deoSystemOneKeyActive ? 'tag-on' : 'tag-off'">{{ deoSystemOneKeyActive ? '运行' : '停止' }}</span>
                <span class="status-tag" :class="deoSystemAutoMode ? 'tag-auto' : 'tag-manual'">{{ deoSystemAutoMode ? '自动' : '手动' }}</span>
                <span v-if="deoEstopIndicatorActive" class="status-tag tag-off">急停</span>
              </div>
            </div>
            <div class="device-card mode-control-card">
              <span class="card-name">除臭系统手自动控制</span>
              <button
                class="btn-toggle"
                :class="deoSystemAutoMode ? 'btn-stop' : 'btn-start'"
                :disabled="role !== 'admin' || deoControlBusy.mode || !deoControlFresh"
                @click="toggleDeoMode"
              >
                {{ deoSystemAutoMode ? '切手动' : '切自动' }}
              </button>
            </div>
            <div class="device-card deodorization-frequency-card">
              <div class="deo-frequency-feedback">
                <span class="card-name">风机频率反馈</span>
                <strong>{{ formatDeoFrequency(deoFanFrequencyFeedback) }} Hz</strong>
              </div>
              <div class="deo-frequency-editor">
                <el-input-number
                  v-model="deoFanFrequencySet"
                  :min="DEO_FAN_FREQUENCY_MIN"
                  :max="DEO_FAN_FREQUENCY_MAX"
                  :step="0.1"
                  :precision="1"
                  size="small"
                  controls-position="right"
                  class="deo-frequency-input"
                  :disabled="role !== 'admin' || deoControlBusy.frequency || !deoControlFresh"
                  @focus="deoFanFrequencyEditing = true"
                  @blur="deoFanFrequencyEditing = false"
                />
                <span class="deo-frequency-unit">Hz</span>
                <button
                  class="btn-toggle btn-start"
                  :disabled="role !== 'admin' || deoControlBusy.frequency || !deoControlFresh"
                  @click="writeDeoFanFrequency"
                >写入</button>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 加药间区域 -->
    <div class="columns-container dosing-row">

      <!-- PAC加药间 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">PAC加药间</div></div>
        <div class="device-list">
          <div v-for="d in pacDevices" :key="d.code" class="device-card" :class="cardClass(d)">
            <span class="card-name">{{ d.name }}</span>
            <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
            <button class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
          </div>
        </div>
      </div>

      <!-- PAM加药间 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">PAM加药间</div></div>
        <div class="device-list">
          <div v-for="d in pamDevices" :key="d.code" class="device-card" :class="cardClass(d)">
            <span class="card-name">{{ d.name }}</span>
            <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
            <button class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
          </div>
        </div>
      </div>

      <!-- 乙酸钠加药间 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">乙酸钠加药间</div></div>
        <div class="device-list">
          <div v-for="d in sacDevices" :key="d.code" class="device-card" :class="cardClass(d)">
            <span class="card-name">{{ d.name }}</span>
            <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
            <button class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
          </div>
        </div>
      </div>

      <!-- 次氯酸钠加药间 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">次氯酸钠加药间</div></div>
        <div class="device-list">
          <div v-for="d in shcDevices" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <button class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 滤布滤池区域 -->
    <div class="columns-container dosing-row">

      <!-- 1#转盘 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">滤布滤池1#转盘</div></div>
        <div class="device-list">
          <div class="device-card mode-control-card">
            <div class="card-main">
              <span class="card-name">{{ bfdModes.BFD_1_MODE_SWITCH.name }}</span>
              <span v-if="!bfdModes.BFD_1_MODE_SWITCH.read" class="card-unread" title="未读取到数据">✕</span>
              <button class="btn-toggle" :class="{ 'btn-on': bfdModes.BFD_1_MODE_SWITCH.modeAuto }" @click="toggleBfdMode(bfdModes.BFD_1_MODE_SWITCH)">{{ bfdModes.BFD_1_MODE_SWITCH.modeAuto ? '自动' : '手动' }}</button>
            </div>
          </div>
          <div v-for="d in bfd1Devices" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <template v-if="d.readOnly">
                <button class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
              </template>
              <template v-else>
                <div class="card-btn-group">
                  <button class="btn-toggle" :class="d.run ? 'btn-stop' : 'btn-start'" @click="toggleBfdDevice(d)">{{ d.run ? '停止' : '启动' }}</button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- 2#转盘 -->
      <div class="zone-column">
        <div class="column-header"><div class="col-title">滤布滤池2#转盘</div></div>
        <div class="device-list">
          <div class="device-card mode-control-card">
            <div class="card-main">
              <span class="card-name">{{ bfdModes.BFD_2_MODE_SWITCH.name }}</span>
              <span v-if="!bfdModes.BFD_2_MODE_SWITCH.read" class="card-unread" title="未读取到数据">✕</span>
              <button class="btn-toggle" :class="{ 'btn-on': bfdModes.BFD_2_MODE_SWITCH.modeAuto }" @click="toggleBfdMode(bfdModes.BFD_2_MODE_SWITCH)">{{ bfdModes.BFD_2_MODE_SWITCH.modeAuto ? '自动' : '手动' }}</button>
            </div>
          </div>
          <div v-for="d in bfd2Devices" :key="d.code" class="device-card" :class="cardClass(d)">
            <div class="card-main">
              <span class="card-name">{{ d.name }}</span>
              <span v-if="shouldShowUnread(d.code)" class="card-unread" title="未读取到数据">✕</span>
              <template v-if="d.readOnly">
                <button class="btn-toggle" disabled>{{ d.run?'运行中':'已停止' }}</button>
              </template>
              <template v-else>
                <div class="card-btn-group">
                  <button class="btn-toggle" :class="d.run ? 'btn-stop' : 'btn-start'" @click="toggleBfdDevice(d)">{{ d.run ? '停止' : '启动' }}</button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import jwt_decode from 'jwt-decode'
import request from '@/api/request'
import { usePlcDiagnostics } from '@/composables/usePlcDiagnostics'
import { canControlInRemoteManual, manualModeWriteValue, normalizePlcBit, readManualMode } from '@/utils/plcControlModes'
import {
  DEO_FAN_FREQUENCY_MAX,
  DEO_FAN_FREQUENCY_MIN,
  isDeodorizationFanFrequencyInRange,
} from '@/utils/deodorizationFrequency'

const { logFailure } = usePlcDiagnostics()
const token = localStorage.getItem('token')
let role = 'user'
try { role = jwt_decode(token || '{}')?.role || 'user' } catch { role = 'user' }

// ====================== 设备定义（code 匹配后端） ======================

const b1Devices = reactive([
  { code: 'B1_QM1', name: '调节池电动阀门1', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'B1_QM2', name: '调节池电动阀门2', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'B1_QM3', name: '调节池电动阀门3', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'B1_HW1', name: '调节池潜水搅拌机1', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'B1_HW2', name: '调节池潜水搅拌机2', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'B1_GP1', name: '调节池提升泵1', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'B1_GP2', name: '调节池提升泵2', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'B1_GP3', name: '调节池提升泵3', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'CS1', name: '粗格栅1', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'CS2', name: '粗格栅2', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'CS_CONVEYOR', name: '皮带输送机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
])

const x1Devices = reactive([
  { code: 'FS1', name: '细格栅1', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'FS2', name: '细格栅2', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'FS_CONVEYOR', name: '皮带输送机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'MLC_PUMP_1', name: '涡流排砂泵1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'MLC_PUMP_2', name: '涡流排砂泵2', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'MLC_SEPARATOR', name: '分砂电机', type: 'motor', run: false, fault: false, readOnly: true },
  { code: 'MLC_WASH_11', name: '冲洗泵1-1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'MLC_WASH_12', name: '冲洗泵1-2', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'MLC_WASH_21', name: '冲洗泵2-1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'MLC_WASH_22', name: '冲洗泵2-2', type: 'pump', run: false, fault: false, readOnly: true },
])

const h1DevicesLeft = reactive([
  { code: 'H1_QM1', name: '1#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM2', name: '2#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM3', name: '3#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM4', name: '4#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM5', name: '5#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM6', name: '6#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM7', name: '7#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM8', name: '8#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM9', name: '9#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'H1_QM10', name: '10#曝气管阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
])

const h1Mixers = reactive([
  { code: 'H1_HW1', name: '1#选择区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW2', name: '2#选择区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW3', name: '1#厌氧区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW4', name: '2#厌氧区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW5', name: '3#选择区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW6', name: '4#选择区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW7', name: '5#选择区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_HW8', name: '6#选择区搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
])

const h1DevicesRight = reactive([
  { code: 'H1_GP1', name: '1#污水回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP2', name: '2#污水回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP3', name: '3#污水回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP4', name: '4#污水回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP5', name: '1#污泥回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP6', name: '2#污泥回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP7', name: '3#污泥回流泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP8', name: '8#污泥剩余泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'H1_GP9', name: '9#污泥剩余泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
])

const m3Devices = reactive([
  { code: 'M3_QM1', name: '高效沉淀池电动闸板', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'M3_GP1', name: '1#回流污泥转子泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'M3_GP2', name: '2#回流污泥转子泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'M3_GP3', name: '剩余污泥转子泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
  { code: 'M3_HW1', name: '絮凝搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'M3_HW2', name: '混凝搅拌机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'M3_HL1', name: '刮泥机', type: 'motor', remote: false, manualMode: false, run: false, fault: false },
  { code: 'M3_GP4', name: '集水坑潜污泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
])

const fanDevices = reactive([
  { code: 'FAN_SYS_1', name: '1#风机系统', type: 'motor', run: false, fault: false },
  { code: 'FAN_SYS_2', name: '2#风机系统', type: 'motor', run: false, fault: false },
  { code: 'FAN_SYS_3', name: '3#风机系统', type: 'motor', run: false, fault: false },
])

const deoSystemOneKeyActive = ref(false)
const deoSystemAutoMode = ref(false)
const deoEstopIndicatorActive = ref(false)
const deoFanFrequencyFeedback = ref(0)
const deoFanFrequencySet = ref(0)
const deoFanFrequencyEditing = ref(false)
const deoLastReadTime = ref(0)
const deoControlBusy = reactive({ oneKey: false, mode: false, frequency: false })

const dewaterDevices = reactive([
  { code: 'BFP_1', name: '1#带机', startParam: 'BFP_1_ONE_KEY_START', stopParam: 'BFP_1_ONE_KEY_STOP' },
  { code: 'BFP_2', name: '2#带机', startParam: 'BFP_2_ONE_KEY_START', stopParam: 'BFP_2_ONE_KEY_STOP' },
])

const p2Devices = reactive([
  { code: 'P2_QM1', name: '消毒池中水阀门', type: 'valve', remote: false, manualMode: false, open: false, close: true, fault: false, run: false },
  { code: 'P2_GP1', name: '消毒池中水水泵', type: 'pump', remote: false, manualMode: false, run: false, fault: false },
])

// ====================== 加药间设备定义 ======================
const pacDevices = reactive([
  { code: 'PAC_MIXER_1', name: '搅拌机1', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'PAC_MIXER_2', name: '搅拌机2', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'PAC_DOSING_1', name: '计量泵1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'PAC_DOSING_2', name: '计量泵2', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'PAC_VALVE_1', name: '出药阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'PAC_VALVE_2', name: '出药阀2', type: 'valve', run: false, fault: false, readOnly: true },
])

const pamDevices = reactive([
  { code: 'PAM_MIXER_1', name: '搅拌机1', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'PAM_MIXER_2', name: '搅拌机2', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'PAM_MIXER_3', name: '搅拌机3', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'PAM_FEEDER', name: '投料机', type: 'feeder', run: false, fault: false, readOnly: true },
  { code: 'PAM_SCREW_1', name: '螺杆计量泵1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'PAM_SCREW_2', name: '螺杆计量泵2', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'PAM_SCREW_3', name: '螺杆计量泵3', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'PAM_VIBRATOR', name: '振动机', type: 'vibrator', run: false, fault: false, readOnly: true },
  { code: 'PAM_INLET_VALVE', name: '进水电磁阀', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'PAM_OUTLET_VALVE_1', name: '出液电磁阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'PAM_OUTLET_VALVE_2', name: '出液电磁阀2', type: 'valve', run: false, fault: false, readOnly: true },
])

const sacDevices = reactive([
  { code: 'SAC_MIXER_1', name: '搅拌机1', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'SAC_MIXER_2', name: '搅拌机2', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'SAC_DOSING_1', name: '计量泵1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'SAC_DOSING_2', name: '计量泵2', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'SAC_DOSING_3', name: '计量泵3', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'SAC_DOSING_4', name: '计量泵4', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'SAC_VALVE_1', name: '出液电磁阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'SAC_VALVE_2', name: '出液电磁阀2', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'SAC_VALVE_3', name: '出液电磁阀3', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'SAC_VALVE_4', name: '出液电磁阀4', type: 'valve', run: false, fault: false, readOnly: true },
])

const shcDevices = reactive([
  { code: 'SHC_MIXER_1', name: '搅拌机1', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'SHC_MIXER_2', name: '搅拌机2', type: 'mixer', run: false, fault: false, readOnly: true },
  { code: 'SHC_SCREW_1', name: '螺杆计量泵1', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'SHC_SCREW_2', name: '螺杆计量泵2', type: 'pump', run: false, fault: false, readOnly: true },
  { code: 'SHC_SCREW_3', name: '螺杆计量泵3', type: 'pump', run: false, fault: false, readOnly: true },
])

// ====================== 滤布滤池设备定义 ======================
const bfdModes = reactive({
  BFD_1_MODE_SWITCH: { code: 'BFD_1_MODE_SWITCH', name: '1#转盘系统手自动位', modeAuto: false, read: false },
  BFD_2_MODE_SWITCH: { code: 'BFD_2_MODE_SWITCH', name: '2#转盘系统手自动位', modeAuto: false, read: false },
})

const bfdDevices = reactive([
  { code: 'BFD_1_DRIVE_MOTOR', name: '1#转盘传动电机', type: 'motor', run: false, fault: false },
  { code: 'BFD_1_BACKWASH_PUMP_1', name: '1#转盘反冲泵1', type: 'pump', run: false, fault: false },
  { code: 'BFD_1_BACKWASH_PUMP_2', name: '1#转盘反冲泵2', type: 'pump', run: false, fault: false },
  { code: 'BFD_1_BACKWASH_VALVE_1', name: '1#转盘反冲阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_1_BACKWASH_VALVE_2', name: '1#转盘反冲阀2', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_1_BACKWASH_VALVE_3', name: '1#转盘反冲阀3', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_1_BACKWASH_VALVE_4', name: '1#转盘反冲阀4', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_1_SLUDGE_VALVE_1', name: '1#转盘排泥阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_1_SLUDGE_VALVE_2', name: '1#转盘排泥阀2', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_2_DRIVE_MOTOR', name: '2#转盘传动电机', type: 'motor', run: false, fault: false },
  { code: 'BFD_2_BACKWASH_PUMP_1', name: '2#转盘反冲泵1', type: 'pump', run: false, fault: false },
  { code: 'BFD_2_BACKWASH_PUMP_2', name: '2#转盘反冲泵2', type: 'pump', run: false, fault: false },
  { code: 'BFD_2_BACKWASH_VALVE_1', name: '2#转盘反冲阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_2_BACKWASH_VALVE_2', name: '2#转盘反冲阀2', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_2_BACKWASH_VALVE_3', name: '2#转盘反冲阀3', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_2_BACKWASH_VALVE_4', name: '2#转盘反冲阀4', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_2_BACKWASH_VALVE_5', name: '2#转盘排泥阀1', type: 'valve', run: false, fault: false, readOnly: true },
  { code: 'BFD_2_BACKWASH_VALVE_6', name: '2#转盘排泥阀2', type: 'valve', run: false, fault: false, readOnly: true },
])

// ====================== 滤布滤池分组 ======================
const bfd1Devices = bfdDevices.filter(d => d.code.startsWith('BFD_1'))
const bfd2Devices = bfdDevices.filter(d => d.code.startsWith('BFD_2'))

// ====================== 所有设备扁平索引 ======================
const allDevices = [...b1Devices, ...x1Devices, ...fanDevices, ...h1DevicesLeft, ...h1Mixers, ...h1DevicesRight, ...m3Devices, ...p2Devices, ...dewaterDevices, ...pacDevices, ...pamDevices, ...sacDevices, ...shcDevices, ...bfdDevices]
const deviceMap = {}
allDevices.forEach(d => { deviceMap[d.code] = d })

// ====================== 状态 ======================
const anyConnected = ref(false)
const lastReadTime = ref(Object.fromEntries(allDevices.map(d => [d.code, 0])))
const now = ref(Date.now())
setInterval(() => { now.value = Date.now() }, 1000)
let pollTimer = null

// 每个 PLC 端点独立的连接状态和失败计数
const plcEndpointStatus = {}
const FAILURE_THRESHOLD = 3

function getOrCreatePlcStatus(url) {
  if (!plcEndpointStatus[url]) {
    plcEndpointStatus[url] = { connected: false, consecutiveFailures: 0 }
  }
  return plcEndpointStatus[url]
}

// 判断设备是否应该显示×：距上次成功超过 5 秒
const UNREAD_THRESHOLD_MS = 5000
const deoControlFresh = computed(() => deoLastReadTime.value > 0 && now.value - deoLastReadTime.value <= UNREAD_THRESHOLD_MS)

function shouldShowUnread(code) {
  if (!lastReadTime.value[code]) return true
  return now.value - lastReadTime.value[code] > UNREAD_THRESHOLD_MS
}

// ====================== 后端路径映射（依据文档） ======================
// M3/H1/B1 系列：S7-1200/1500，command 接口需要 action 参数
function getM3ApiBase(code) {
  if (code.startsWith('B1_')) return '/plc/b1'
  if (code.startsWith('H1_')) return '/plc/h1'
  // M3 和 P2 共用同一个控制器
  return '/plc/m3'
}

// S7-200 Smart 系列：command 接口只需 pulseMs，不需要 action
function getSmartApiBase(code) {
  if (code.startsWith('CS')) return '/plc/coarse-screen'
  if (code.startsWith('FS')) return '/plc/fine-screen'
  if (code.startsWith('MLC_')) return '/plc/multilayer-cyclone'
  if (code.startsWith('BFD_')) return '/plc/baike-fiber-disc'
  return '/plc/coarse-screen'
}

// ====================== 状态更新 ======================
function applyDevice(d) {
  const local = deviceMap[d.code]
  if (!local) return
  if (d.remote !== undefined) local.remote = normalizePlcBit(d.remote)
  if (d.fault !== undefined) local.fault = !!d.fault
  if (d.manualModeBit !== undefined) {
    local.manualMode = readManualMode(d.code, d.manualModeBit)
  }
  if (local.type === 'valve') {
    if (d.openPosition !== undefined) local.open = !!d.openPosition
    if (d.closePosition !== undefined) local.close = !!d.closePosition
  } else {
    if (d.running !== undefined) local.run = !!d.running
  }
}

function applyDeodorizationParameters(parameters, timestamp) {
  if (!Array.isArray(parameters)) return false
  let updated = false
  parameters.forEach((parameter) => {
    const value = parameter.value
    switch (parameter.code) {
      case 'DEO_SYSTEM_ONE_KEY_START_STOP_V2_5':
        deoSystemOneKeyActive.value = normalizePlcBit(value)
        updated = true
        break
      case 'DEO_SYSTEM_MODE_SWITCH_V0_2':
        deoSystemAutoMode.value = normalizePlcBit(value)
        updated = true
        break
      case 'DEO_ESTOP_INDICATOR_V0_0':
        deoEstopIndicatorActive.value = normalizePlcBit(value)
        updated = true
        break
      case 'DEO_FAN_FREQ_VD328': {
        const frequency = Number(value)
        if (Number.isFinite(frequency)) deoFanFrequencyFeedback.value = frequency
        updated = true
        break
      }
      case 'DEO_FAN_FREQ_SET_VD472': {
        const frequency = Number(value)
        if (!deoFanFrequencyEditing.value && Number.isFinite(frequency)) deoFanFrequencySet.value = frequency
        updated = true
        break
      }
      default:
        break
    }
  })
  if (updated) deoLastReadTime.value = timestamp
  return updated
}

async function fetchAll() {
  const endpoints = [
    { url: '/plc/b1/overview', label: 'B1' },
    { url: '/plc/coarse-screen/overview', label: '粗格栅' },
    { url: '/plc/fine-screen/overview', label: '细格栅' },
    { url: '/plc/multilayer-cyclone/overview', label: '多层旋流' },
    { url: '/plc/fan/overview', label: '鼓风机房' },
    { url: '/plc/h1/overview', label: 'H1' },
    { url: '/plc/m3/overview', label: 'M3/P2' },
    { url: '/plc/baike-filter-press/overview', label: '脱水车间' },
    { url: '/plc/deodorization/overview', label: '除臭' },
    { url: '/plc/pac/overview', label: 'PAC' },
    { url: '/plc/pam/overview', label: 'PAM' },
    { url: '/plc/sodium-acetate/overview', label: '乙酸钠' },
    { url: '/plc/sodium-hypochlorite/overview', label: '次氯酸钠' },
    { url: '/plc/baike-fiber-disc/overview', label: '滤布滤池' },
  ]

  // 逐个请求、逐个更新：先到先显示，不用等最慢的 PLC
  const fetchOne = async ({ url, label }) => {
    const plcSt = getOrCreatePlcStatus(url)
    const ts = Date.now()
    let anyDeviceUpdated = false
    try {
      const res = await request.get(url, { timeout: 3000 })
      if (!res.data?.success) {
        // HTTP 200 但 success=false（后端返回错误 JSON）
        logFailure(label, url, res.status || 0, res.data?.message || res.data?.rootCause || 'PLC 返回失败')
      } else if (res.data?.data?.plc?.stale) {
        // 后端返回 stale 缓存：PLC 连接失败但有旧数据
        logFailure(label, url, 0, res.data.data.plc.lastFailureMessage || 'PLC 连接失败（返回旧数据）')
      }
      if (res.data?.success) {
        const { devices, parameters } = res.data.data
        if (label === '除臭' && applyDeodorizationParameters(parameters, ts)) {
          plcSt.connected = true
          plcSt.consecutiveFailures = 0
          anyDeviceUpdated = true
        }
        let remoteStatus = false
        if (label === '粗格栅' && parameters) {
          const p = parameters.find(p => p.code === 'CS_REMOTE_CONTROL')
          if (p) remoteStatus = normalizePlcBit(p.value)
        } else if (label === '细格栅' && parameters) {
          const p = parameters.find(p => p.code === 'FS_REMOTE_CONTROL')
          if (p) remoteStatus = normalizePlcBit(p.value)
        }
        if (devices && devices.length > 0) {
          // 标记该 PLC 连接成功，重置失败计数
          plcSt.connected = true
          plcSt.consecutiveFailures = 0
          devices.forEach(d => {
            // 只更新该 PLC 自己的设备时间戳
            lastReadTime.value[d.code] = ts
            anyDeviceUpdated = true
            if (label === '粗格栅' || label === '细格栅') {
              const local = deviceMap[d.code]
              if (local) local.remote = remoteStatus
            }
            applyDevice(d)
          })
        }
        if (label === '滤布滤池' && parameters) {
          parameters.forEach(p => {
            const mode = bfdModes[p.code]
            if (mode) { mode.modeAuto = !!p.value; mode.read = true }
          })
        }
        return true
      }
      // 响应成功但该 PLC 的设备没有被更新（返回空数据）
      if (res.data?.success && !anyDeviceUpdated) {
        logFailure(label, url, 0, 'PLC 返回成功但无设备数据')
      }
    } catch (e) {
      // 该 PLC 失败，不重置 lastReadTime，3秒后自然显示 ✕
      plcSt.consecutiveFailures++
      if (plcSt.consecutiveFailures >= FAILURE_THRESHOLD) {
        plcSt.connected = false
      }
      // 记录诊断日志
      const statusCode = e.response?.status || 0
      const errorMsg = e.response?.data?.message || e.message || '请求失败'
      const isTransient = e.message?.includes('ERR_NETWORK_CHANGED') || e.message?.includes('ERR_CONNECTION_RESET')
      if (!isTransient) {
        logFailure(label, url, statusCode, errorMsg)
      }
      console.error(`[工艺总控] ${label} 数据读取失败:`, errorMsg, `(${url})`)
    }
    return false
  }

  // 并发发起，不互相等待，每个 PLC 独立完成
  endpoints.forEach(ep => fetchOne(ep).then(() => {
    // 每个 PLC 完成后立即更新总连接状态
    anyConnected.value = Object.values(plcEndpointStatus).some(s => s.connected)
  }))
}

// ====================== 控制指令 ======================

// 风机系统启停：与鼓风机房页面保持相同的保持命令。
async function sendFanCommand(d, start) {
  if (!d) return
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  if (start && d.fault) return ElMessage.warning(`${d.name}存在故障，禁止启动`)
  const label = start ? '启动' : '停止'
  await ElMessageBox.confirm(`确认${label}「${d.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/fan/devices/${d.code}/command`, null, {
      params: { action: start ? 'HOLD_ON' : 'HOLD_OFF', pulseMs: 300 }
    })
    ElMessage.success(`${label}指令已发送`)
    setTimeout(fetchAll, 500)
    setTimeout(fetchAll, 1500)
  } catch (e) { ElMessage.error(`${label}失败`) }
}

async function writeDeoBooleanParameter(code, enabled) {
  await request.post(`/plc/deodorization/parameters/${code}`, null, {
    params: { value: enabled ? 1 : 0 }
  })
}

async function toggleDeoOneKey() {
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  const starting = !deoSystemOneKeyActive.value
  if (starting && !deoSystemAutoMode.value) return ElMessage.warning('请先将除臭系统切换到自动模式')
  if (starting && deoEstopIndicatorActive.value) return ElMessage.warning('急停状态下不能一键启动除臭系统')
  const action = starting ? '启动' : '停止'
  try {
    await ElMessageBox.confirm(`确认一键${action}除臭系统？动作顺序由PLC执行。`, '系统操作确认', {
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deoControlBusy.oneKey = true
  try {
    await writeDeoBooleanParameter('DEO_SYSTEM_ONE_KEY_START_STOP_V2_5', starting)
    ElMessage.success(`除臭系统一键${action}指令已发送`)
    setTimeout(fetchAll, 500)
  } catch (e) {
    console.error(`[工艺总控] 除臭系统一键${action}失败:`, e)
    ElMessage.error(`除臭系统一键${action}失败`)
  } finally {
    deoControlBusy.oneKey = false
  }
}

async function toggleDeoMode() {
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  const targetAuto = !deoSystemAutoMode.value
  const targetMode = targetAuto ? '自动模式' : '手动模式'
  try {
    await ElMessageBox.confirm(`确认切换除臭系统到${targetMode}？`, '模式切换确认', {
      confirmButtonText: '确认切换',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deoControlBusy.mode = true
  try {
    await writeDeoBooleanParameter('DEO_SYSTEM_MODE_SWITCH_V0_2', targetAuto)
    ElMessage.success(`除臭系统${targetMode}切换指令已发送`)
    setTimeout(fetchAll, 500)
  } catch (e) {
    console.error('[工艺总控] 除臭系统模式切换失败:', e)
    ElMessage.error('除臭系统模式切换失败')
  } finally {
    deoControlBusy.mode = false
  }
}

async function writeDeoFanFrequency() {
  if (role !== 'admin') return ElMessage.warning('仅管理员可操作')
  const value = Number(deoFanFrequencySet.value)
  if (!isDeodorizationFanFrequencyInRange(value)) {
    return ElMessage.warning(`除臭风机频率给定范围为 ${DEO_FAN_FREQUENCY_MIN}-${DEO_FAN_FREQUENCY_MAX}Hz`)
  }
  try {
    await ElMessageBox.confirm(`确认将除臭风机频率设定写入为 ${value.toFixed(1)} Hz？`, '参数写入确认', {
      confirmButtonText: '确认写入',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deoControlBusy.frequency = true
  try {
    await request.post('/plc/deodorization/parameters/DEO_FAN_FREQ_SET_VD472', null, { params: { value } })
    ElMessage.success('除臭风机频率已下发')
    deoFanFrequencyEditing.value = false
    setTimeout(fetchAll, 500)
  } catch (e) {
    console.error('[工艺总控] 除臭风机频率写入失败:', e)
    ElMessage.error('除臭风机频率下发失败')
  } finally {
    deoControlBusy.frequency = false
  }
}

// 压滤机一键启动/停止：写参数
async function sendBfpParam(paramCode) {
  const label = paramCode.includes('START') ? '一键启动' : '一键停止'
  const name = paramCode.startsWith('BFP_1') ? '1#带机' : '2#带机'
  await ElMessageBox.confirm(`确认${label}「${name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/baike-filter-press/parameters/${paramCode}`, null, { params: { value: 'true' } })
    ElMessage.success(`${label}指令已发送`)
    setTimeout(fetchAll, 500)
  } catch (e) { ElMessage.error(`${label}失败`) }
}

// M3/H1/B1 系列：需要 action 参数（START/STOP/OPEN/CLOSE）
async function sendM3Cmd(code, action) {
  const d = deviceMap[code]
  if (!d) return
  if (!canControl(d)) return ElMessage.warning('该设备未授权远程控制或未切换手动模式')
  const label = action === 'START' ? '启动' : action === 'STOP' ? '停止' : action === 'OPEN' ? '开启' : '关闭'
  await ElMessageBox.confirm(`确认${label}「${d.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    const base = getM3ApiBase(code)
    await request.post(`${base}/devices/${code}/command`, null, { params: { action, pulseMs: 300 } })
    ElMessage.success(`${label}指令已发送`)
    setTimeout(fetchAll, 500)
    setTimeout(fetchAll, 1500)
  } catch (e) { ElMessage.error(`${label}失败`) }
}

// S7-200 Smart 系列（CS/FS）：使用 parameter 接口发送脉冲信号
async function sendSmartCmd(code) {
  const d = deviceMap[code]
  if (!d) return
  if (!canControlScreen(d)) return ElMessage.warning('该设备需处于远程且手动模式')
  const label = d.run ? '停止' : '启动'
  const paramCode = d.run ? `${code}_STOP` : `${code}_START`
  await ElMessageBox.confirm(`确认${label}「${d.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    const base = getSmartApiBase(code)
    await request.post(`${base}/parameters/${paramCode}`, null, { params: { value: 'true' } })
    ElMessage.success(`${label}指令已发送`)
    setTimeout(fetchAll, 500)
    setTimeout(fetchAll, 1500)
  } catch (e) { ElMessage.error(`${label}失败`) }
}

// 滤布滤池传动电机启停（复用 L1 逻辑）
async function toggleBfdDevice(d) {
  if (!d) return
  const newValue = !d.run
  const label = newValue ? '启动' : '停止'
  await ElMessageBox.confirm(`确认${label}「${d.name}」？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/baike-fiber-disc/parameters/${d.code}_COMMAND`, null, { params: { value: String(newValue) } })
    ElMessage.success(`${label}指令已发送`)
    setTimeout(fetchAll, 500)
  } catch (e) { ElMessage.error(`${label}失败`) }
}

// 滤布滤池手自动切换（复用 L1 逻辑）
async function toggleBfdMode(mode) {
  if (!mode) return
  const newMode = !mode.modeAuto
  const label = newMode ? '自动' : '手动'
  await ElMessageBox.confirm(`确认切换「${mode.name}」为${label}模式？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`/plc/baike-fiber-disc/parameters/${mode.code}`, null, { params: { value: String(newMode) } })
    ElMessage.success(`已切换为${label}模式`)
    setTimeout(fetchAll, 500)
  } catch (e) { ElMessage.error('手自动切换失败') }
}

// ====================== 控制权限判断 ======================
// 所有可控设备都必须同时处于远程和手动模式。
function canControl(d) {
  return canControlInRemoteManual(d)
}

// 手自动模式切换
async function toggleManualMode(d) {
  if (!d.remote) return ElMessage.warning('该设备未在远程模式，无法切换手自动')
  const enabled = manualModeWriteValue(d.code, d.manualMode)
  const label = d.manualMode ? '自动' : '手动'
  const apiBase = d.code.startsWith('B1_') || d.code.startsWith('B2_') ? '/plc/b1'
    : d.code.startsWith('H1_') ? '/plc/h1'
    : d.code.startsWith('M3_') || d.code.startsWith('P2_') ? '/plc/m3'
    : d.code.startsWith('CS') ? '/plc/coarse-screen'
    : d.code.startsWith('FS') ? '/plc/fine-screen'
    : null
  if (!apiBase) return
  await ElMessageBox.confirm(`确认切换「${d.name}」为${label}模式？`, '操作确认', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
  try {
    await request.post(`${apiBase}/devices/${d.code}/manual-mode`, null, { params: { enabled } })
    ElMessage.success(`已切换为${label}模式`)
    setTimeout(fetchAll, 500)
    setTimeout(fetchAll, 1500)
  } catch (e) { ElMessage.error('手自动切换失败') }
}

function canControlScreen(d) {
  return canControlInRemoteManual(d)
}

// ====================== 样式辅助 ======================
function hasValvePositionFeedback(d) {
  return d?.type === 'valve'
    && Object.prototype.hasOwnProperty.call(d, 'open')
    && Object.prototype.hasOwnProperty.call(d, 'close')
}

function isValvePositionActive(d, position) {
  return !shouldShowUnread(d.code) && d[position] === true
}

function cardClass(d) {
  if (d.fault) return 'fault'
  if (d.type === 'valve') return d.open ? 'running' : 'standby'
  return d.run ? 'running' : 'standby'
}

function formatDeoFrequency(value) {
  const frequency = Number(value)
  return Number.isFinite(frequency) ? frequency.toFixed(3) : '--'
}

// ====================== 生命周期 ======================
onMounted(() => { fetchAll(); pollTimer = setInterval(fetchAll, 2000) })
onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.process-control-page {
  padding: 14px 16px 24px;
  background: linear-gradient(180deg, #060d1f 0%, #0a1628 40%, #0d1e36 100%);
  min-height: calc(100vh - var(--nav-height, 113px));
  overflow-y: auto;
  overflow-x: hidden;
  box-sizing: border-box;
}

.page-header { margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; }
.header-title-group { display: flex; align-items: center; gap: 12px; }
.header-dot { width: 9px; height: 9px; background: var(--color-primary); border-radius: 50%; box-shadow: 0 0 8px var(--color-primary); }
.header-title { font-size: 1.625rem; font-weight: 700; color: #fff; margin: 0; letter-spacing: 2px; }
.header-line { flex: 1; height: 1px; background: linear-gradient(90deg, rgba(0,128,255,0.4), transparent); }

.columns-container { display: flex; gap: 10px; width: 100%; }
.columns-container.dosing-row { margin-top: 10px; }

.zone-column { flex: 1; min-width: 0; display: flex; flex-direction: column; background: linear-gradient(135deg, rgba(0,20,60,0.85), rgba(0,30,80,0.65)); border: 1px solid rgba(0,128,255,0.2); border-radius: 6px; }
.zone-column-group { flex: 2; min-width: 0; display: flex; flex-direction: column; background: linear-gradient(135deg, rgba(0,20,60,0.85), rgba(0,30,80,0.65)); border: 1px solid rgba(0,128,255,0.2); border-radius: 6px; }
.group-body { display: flex; flex: 1; gap: 1px; background: rgba(0,128,255,0.1); }
.sub-column { flex: 1; min-width: 0; background: linear-gradient(135deg, rgba(0,20,60,0.85), rgba(0,30,80,0.65)); border: none; border-radius: 0; }

.column-header { padding: 12px 14px; background: linear-gradient(90deg, rgba(0,128,255,0.2), rgba(0,232,255,0.05), transparent); border-bottom: 1px solid rgba(0,128,255,0.25); display: flex; align-items: baseline; gap: 10px; flex-shrink: 0; }

.merged-header { flex-direction: column; gap: 2px; }
.merged-column .merged-section + .merged-section { border-top: 1px solid rgba(0,128,255,0.15); }
.merged-section-title { padding: 8px 14px 0; font-size: 1rem; font-weight: 600; color: rgba(0,232,255,0.7); display: flex; align-items: baseline; gap: 8px; }
.section-read-state { margin-left: auto; font-size: 0.75rem; font-weight: 600; }
.section-read-state.online { color: #7dffd1; }
.section-read-state.offline { color: #ff8080; }
.col-title { font-size: 1.25rem; font-weight: 700; color: #fff; letter-spacing: 1px; white-space: nowrap; }

.device-list { padding: 10px 12px; display: flex; flex-direction: column; gap: 6px; }

.device-card { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 12px; border: 2px solid rgba(100,100,100,0.5); background: rgba(15,23,42,0.6); border-radius: 4px; transition: all 0.3s ease; }
.device-card:has(.card-status-bar) { flex-direction: column; align-items: stretch; gap: 4px; }
.device-card.standby { border-color: rgba(100,100,100,0.4); }
.device-card.running {
  border-color: rgba(48,230,173,0.86);
  background: rgba(48,230,173,0.07);
  box-shadow: inset 0 0 0 2px rgba(48,230,173,0.34), 0 0 16px rgba(48,230,173,0.32), 0 0 30px rgba(48,230,173,0.16);
}
.device-card.fault { border-color: rgba(255,80,80,0.8); box-shadow: 0 0 10px rgba(255,80,80,0.2); background: rgba(255,80,80,0.04); }

.card-main { display: flex; align-items: center; justify-content: space-between; gap: 10px; width: 100%; }
.card-name { font-size: 1.0625rem; font-weight: 600; color: #e8fbff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; min-width: 0; }
.card-unread { font-size: 1.125rem; color: #ff5050; font-weight: 700; flex-shrink: 0; opacity: 0.8; line-height: 1; }

.btn-toggle { padding: 4px 14px; font-size: 0.9375rem; font-weight: 600; border: 1px solid rgba(0,128,255,0.3); background: rgba(20,50,70,0.6); color: #c0eaff; cursor: pointer; border-radius: 3px; transition: all 0.2s ease; white-space: nowrap; flex-shrink: 0; }
.btn-toggle:hover:not(:disabled) { border-color: rgba(0,200,255,0.5); background: rgba(30,70,100,0.8); color: #fff; }
.btn-toggle.btn-on { border-color: rgba(48,230,173,0.5); color: #7dffd1; background: rgba(48,230,173,0.1); }
.btn-toggle:disabled { opacity: 0.4; cursor: not-allowed; }
.card-btn-group { display: flex; gap: 6px; flex-shrink: 0; }
.mode-control-card { border-color: rgba(56,217,255,0.35); }
.deodorization-control-list { gap: 7px; }
.deodorization-frequency-card { align-items: stretch; flex-direction: column; }
.deo-frequency-feedback { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; width: 100%; }
.deo-frequency-feedback strong { color: #8ceeff; font-size: 0.9375rem; font-family: var(--font-data), monospace; white-space: nowrap; }
.deo-frequency-editor { display: flex; align-items: center; justify-content: flex-end; gap: 6px; width: 100%; }
.deo-frequency-input { width: 112px; }
.deo-frequency-unit { color: #84b6d1; font-size: 0.8125rem; }
.btn-start { border-color: rgba(48,230,173,0.4); color: #7dffd1; }
.btn-start:hover { border-color: rgba(48,230,173,0.7); background: rgba(48,230,173,0.15); }
.btn-stop { border-color: rgba(255,120,120,0.4); color: #ff9090; }
.btn-stop:hover { border-color: rgba(255,120,120,0.7); background: rgba(255,120,120,0.15); }




.card-status-bar { display: flex; flex-wrap: wrap; gap: 6px; width: 100%; }
.status-tag { font-size: 0.875rem; font-weight: 600; padding: 3px 8px; border-radius: 2px; white-space: nowrap; line-height: 1.4; }
.status-tag.tag-on { color: #7dffd1; background: rgba(48,230,173,0.12); border: 1px solid rgba(48,230,173,0.3); }
.status-tag.tag-off { color: #ff9090; background: rgba(255,80,80,0.08); border: 1px solid rgba(255,80,80,0.2); }
.status-tag.tag-auto { color: #60c0ff; background: rgba(0,128,255,0.1); border: 1px solid rgba(0,128,255,0.3); }
.status-tag.tag-manual { color: #ffd060; background: rgba(255,180,0,0.1); border: 1px solid rgba(255,180,0,0.3); }
.status-tag.tag-position-on { color: #9affd7; background: rgba(48,230,173,0.14); border: 1px solid rgba(48,230,173,0.48); box-shadow: inset 0 0 8px rgba(48,230,173,0.12); }
.status-tag.tag-position-off { color: rgba(205,225,235,0.48); background: rgba(120,145,160,0.06); border: 1px solid rgba(150,175,190,0.16); }
.status-tag.clickable { cursor: pointer; transition: all 0.2s ease; }
.status-tag.clickable:hover { opacity: 0.8; filter: brightness(1.2); }

</style>
