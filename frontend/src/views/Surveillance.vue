<template>
  <div class="surveillance-page">
    <div class="wave-overlay"></div>

    <div class="page-header">
      <div class="header-title-group">
        <span class="header-dot"></span>
        <h2 class="header-title">视频监控</h2>
        <span class="header-line"></span>
        <span class="header-sub">VIDEO SURVEILLANCE</span>
      </div>
      <div class="header-stats">
        <div class="stat-chip online">
          <span class="stat-num">{{ onlineCount }}</span>
          <span class="stat-label">在线</span>
        </div>
        <div class="stat-chip offline">
          <span class="stat-num">{{ offlineCount }}</span>
          <span class="stat-label">离线</span>
        </div>
        <div class="stat-chip alarm">
          <span class="stat-num">{{ alarmCount }}</span>
          <span class="stat-label">告警</span>
        </div>
      </div>
    </div>

    <div class="camera-grid">
      <div
        v-for="cam in cameras"
        :key="cam.id"
        class="camera-card"
        :class="cam.status"
      >
        <div class="frame-corner tl"></div>
        <div class="frame-corner tr"></div>
        <div class="frame-corner bl"></div>
        <div class="frame-corner br"></div>

        <div class="camera-header">
          <span class="cam-id">{{ cam.id }}</span>
          <span class="cam-status-dot" :class="cam.status"></span>
          <span class="cam-status-text">{{ statusText(cam.status) }}</span>
        </div>

        <div class="camera-video">
          <video
            v-if="cam.status !== 'offline'"
            :src="'/1.mp4'"
            muted
            autoplay
            loop
            playsinline
            class="video-feed"
          ></video>
          <div v-else class="offline-placeholder">
            <span class="offline-icon">&#x1f4f7;</span>
            <span class="offline-text">信号中断</span>
          </div>
          <div v-if="cam.status === 'alarm'" class="alarm-overlay">
            <span class="alarm-badge">ALARM</span>
          </div>
        </div>

        <div class="camera-footer">
          <span class="cam-name">{{ cam.name }}</span>
          <span class="cam-area">{{ cam.area }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const cameras = ref([
  { id: 'CAM-01', name: '粗格栅进水口', area: '粗格栅间', status: 'online' },
  { id: 'CAM-02', name: '细格栅出水', area: '粗格栅间', status: 'online' },
  { id: 'CAM-03', name: '提升泵房全景', area: '提升泵房', status: 'online' },
  { id: 'CAM-04', name: '调节池东侧', area: '调节池', status: 'alarm' },
  { id: 'CAM-05', name: '厌氧池入口', area: 'A2O生化池', status: 'online' },
  { id: 'CAM-06', name: '缺氧池搅拌', area: 'A2O生化池', status: 'online' },
  { id: 'CAM-07', name: '好氧池曝气', area: 'A2O生化池', status: 'online' },
  { id: 'CAM-08', name: '二沉池刮泥', area: 'A2O生化池', status: 'online' },
  { id: 'CAM-09', name: '高效沉淀池', area: '深度处理', status: 'online' },
  { id: 'CAM-10', name: '消毒池出口', area: '消毒车间', status: 'online' },
  { id: 'CAM-11', name: '污泥浓缩池', area: '污泥处理', status: 'offline' },
  { id: 'CAM-12', name: '脱水机房', area: '污泥处理', status: 'online' },
  { id: 'CAM-13', name: '加药间', area: '药剂车间', status: 'online' },
  { id: 'CAM-14', name: '配电室', area: '高低压配电', status: 'online' },
  { id: 'CAM-15', name: '厂区大门', area: '厂区外围', status: 'online' },
  { id: 'CAM-16', name: '中控室', area: '综合楼', status: 'alarm' }
])

const onlineCount = computed(() => cameras.value.filter(c => c.status === 'online').length)
const offlineCount = computed(() => cameras.value.filter(c => c.status === 'offline').length)
const alarmCount = computed(() => cameras.value.filter(c => c.status === 'alarm').length)

const statusText = (status) => {
  const map = { online: '在线', offline: '离线', alarm: '告警' }
  return map[status] || status
}
</script>

<style scoped>
.surveillance-page {
  position: relative;
  height: calc(100vh - var(--nav-height, 132px));
  background: #0F172A;
  padding: 12px 14px 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

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
  z-index: 0;
}

.page-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00e8ff;
  box-shadow: 0 0 8px rgba(0, 232, 255, 0.6);
}

.header-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: #e0eaf5;
  letter-spacing: 2px;
  margin: 0;
}

.header-line {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, rgba(0, 232, 255, 0.4), transparent);
}

.header-sub {
  font-size: 0.875rem;
  color: rgba(0, 232, 255, 0.5);
  letter-spacing: 2px;
  font-family: var(--font-display);
}

.header-stats {
  display: flex;
  gap: 10px;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 4px;
  border: 1px solid rgba(0, 232, 255, 0.15);
  background: rgba(15, 23, 42, 0.88);
}

.stat-num {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 700;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(224, 234, 245, 0.7);
}

.stat-chip.online .stat-num { color: #30e6ad; text-shadow: 0 0 8px rgba(48, 230, 173, 0.4); }
.stat-chip.offline .stat-num { color: #8ceeff; }
.stat-chip.alarm .stat-num { color: #ff7a7a; text-shadow: 0 0 8px rgba(255, 122, 122, 0.4); }

.camera-grid {
  position: relative;
  z-index: 1;
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr);
  gap: 10px;
  min-height: 0;
}

.camera-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.88);
  border: 1px solid rgba(0, 232, 255, 0.15);
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

.camera-card:hover {
  border-color: rgba(0, 232, 255, 0.35);
  box-shadow: 0 0 16px rgba(0, 232, 255, 0.08);
}

.camera-card.alarm {
  border-color: rgba(255, 80, 80, 0.4);
  box-shadow: 0 0 12px rgba(255, 80, 80, 0.08);
}

.camera-card.offline {
  opacity: 0.6;
}

.frame-corner {
  position: absolute;
  width: 10px;
  height: 10px;
  z-index: 3;
  pointer-events: none;
}

.frame-corner::before, .frame-corner::after {
  content: '';
  position: absolute;
  background: #00e8ff;
  border-radius: 1px;
  box-shadow: 0 0 4px #00e8ff;
}

.frame-corner.tl { top: 0; left: 0; }
.frame-corner.tl::before { top: 0; left: 0; width: 10px; height: 1.5px; }
.frame-corner.tl::after { top: 0; left: 0; width: 1.5px; height: 10px; }

.frame-corner.tr { top: 0; right: 0; }
.frame-corner.tr::before { top: 0; right: 0; width: 10px; height: 1.5px; }
.frame-corner.tr::after { top: 0; right: 0; width: 1.5px; height: 10px; }

.frame-corner.bl { bottom: 0; left: 0; }
.frame-corner.bl::before { bottom: 0; left: 0; width: 10px; height: 1.5px; }
.frame-corner.bl::after { bottom: 0; left: 0; width: 1.5px; height: 10px; }

.frame-corner.br { bottom: 0; right: 0; }
.frame-corner.br::before { bottom: 0; right: 0; width: 10px; height: 1.5px; }
.frame-corner.br::after { bottom: 0; right: 0; width: 1.5px; height: 10px; }

.camera-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-bottom: 1px solid rgba(0, 232, 255, 0.08);
  flex-shrink: 0;
}

.cam-id {
  font-family: var(--font-display);
  font-size: 0.8125rem;
  font-weight: 600;
  color: #00e8ff;
  letter-spacing: 0.5px;
}

.cam-status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.cam-status-dot.online { background: #30e6ad; box-shadow: 0 0 6px rgba(48, 230, 173, 0.6); }
.cam-status-dot.offline { background: #666; }
.cam-status-dot.alarm { background: #ff7a7a; box-shadow: 0 0 6px rgba(255, 122, 122, 0.6); animation: alarm-blink 1.2s ease-in-out infinite; }

.cam-status-text {
  font-size: 0.75rem;
  color: rgba(224, 234, 245, 0.6);
}

.camera-video {
  flex: 1;
  position: relative;
  min-height: 0;
  background: rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.video-feed {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: contrast(1.05) saturate(1.1);
}

.offline-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.5);
}

.offline-icon {
  font-size: 1.625rem;
  opacity: 0.3;
}

.offline-text {
  font-size: 0.8125rem;
  color: rgba(224, 234, 245, 0.3);
}

.alarm-overlay {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 2;
}

.alarm-badge {
  display: inline-block;
  padding: 1px 6px;
  font-size: 0.6875rem;
  font-family: var(--font-display);
  font-weight: 700;
  color: #ff4d6a;
  background: rgba(255, 23, 68, 0.15);
  border: 1px solid rgba(255, 23, 68, 0.4);
  border-radius: 2px;
  letter-spacing: 1px;
  animation: alarm-blink 1.2s ease-in-out infinite;
}

.camera-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-top: 1px solid rgba(0, 232, 255, 0.08);
  flex-shrink: 0;
}

.cam-name {
  font-size: 0.8125rem;
  color: #e0eaf5;
  font-weight: 500;
}

.cam-area {
  font-size: 0.75rem;
  color: rgba(0, 232, 255, 0.5);
}

@keyframes alarm-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
