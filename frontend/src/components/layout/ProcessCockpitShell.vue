<template>
  <div class="cp-shell">
    <div v-if="backgroundModelUrl" class="cp-scene-background">
      <BackgroundGlbScene :model-url="backgroundModelUrl" />
      <div class="cp-scene-shade"></div>
    </div>
    <div class="cp-aurora cp-aurora-left"></div>
    <div class="cp-aurora cp-aurora-right"></div>
    <div class="cp-noise"></div>
    <div class="cp-vignette"></div>
    <div class="cp-gridlines"></div>
    <div class="cp-flare cp-flare-top"></div>
    <div class="cp-flare cp-flare-bottom"></div>
    <div class="cp-edge-rail cp-rail-left"><span></span></div>
    <div class="cp-edge-rail cp-rail-right"><span></span></div>
    <div class="cp-hud-node cp-node-top-left"></div>
    <div class="cp-hud-node cp-node-top-right"></div>
    <div class="cp-hud-node cp-node-bottom-left"></div>
    <div class="cp-hud-node cp-node-bottom-right"></div>

    <div class="cp-command-deck">
      <aside class="cp-side-panel cp-left-panel">
        <section class="cp-panel-block">
          <div class="cp-panel-title">{{ metricsTitle }}</div>
          <div class="cp-metric-grid">
            <article v-for="metric in metricCards" :key="metric.key" class="cp-metric-card">
              <div class="cp-metric-label">{{ metric.name }}</div>
              <div class="cp-metric-value">{{ metric.value }}</div>
              <div class="cp-metric-unit">{{ metric.unit }}</div>
              <div class="cp-metric-status" :class="metric.statusClass || 'ok'">{{ metric.status }}</div>
            </article>
          </div>
        </section>

        <section class="cp-panel-block">
          <div class="cp-panel-title">{{ overviewTitle }}</div>
          <div class="cp-overview-grid">
            <div
              v-for="overview in overviewCards"
              :key="overview.key"
              class="cp-overview-chip"
              :class="{ warning: overview.warning }"
            >
              <span>{{ overview.label }}</span>
              <strong>{{ overview.value }}</strong>
            </div>
          </div>
        </section>

        <section class="cp-panel-block cp-roster-block">
          <div class="cp-panel-title">{{ rosterTitle }}</div>
          <div class="cp-device-roster">
            <button
              v-for="item in rosterItems"
              :key="item.key"
              class="cp-roster-item"
              :class="{ active: selectedRosterKey === item.key, fault: item.fault }"
              @click="$emit('select-roster', item.key)"
            >
              <div>
                <div class="cp-roster-name">{{ item.name }}</div>
                <div class="cp-roster-meta">{{ item.meta }}</div>
              </div>
              <div class="cp-roster-state">
                <span class="cp-state-dot" :class="item.stateClass"></span>
                {{ item.state }}
              </div>
            </button>
          </div>
        </section>
      </aside>

      <main class="cp-stage-panel">
        <section class="cp-hero-stage">
          <div class="cp-stage-frame">
            <div class="cp-stage-canopy"></div>
            <div class="cp-stage-orbit cp-orbit-left"></div>
            <div class="cp-stage-orbit cp-orbit-right"></div>
            <div class="cp-stage-spectrum cp-spectrum-left"></div>
            <div class="cp-stage-spectrum cp-spectrum-right"></div>
            <div class="cp-stage-hud-grid"></div>
            <div class="cp-stage-axis-label cp-axis-x">工艺方向</div>
            <div class="cp-stage-axis-label cp-axis-y">单元方向</div>
            <div class="cp-frame-corner cp-top-left"></div>
            <div class="cp-frame-corner cp-top-right"></div>
            <div class="cp-frame-corner cp-bottom-left"></div>
            <div class="cp-frame-corner cp-bottom-right"></div>

            <div v-if="stageBadges.length" class="cp-stage-badges">
              <div v-for="badge in stageBadges" :key="badge.key" class="cp-stage-badge">
                <span>{{ badge.label }}</span>
                <strong>{{ badge.value }}</strong>
              </div>
            </div>

            <div v-if="statusChips.length" class="cp-stage-status-stack">
              <div
                v-for="chip in statusChips"
                :key="chip.key"
                class="cp-status-chip"
                :class="chip.variant || 'blue'"
              >
                <span>{{ chip.label }}</span>
                <strong>{{ chip.value }}</strong>
              </div>
            </div>

            <div class="cp-stage-content">
              <slot name="stage" />
            </div>
          </div>
        </section>

        <section class="cp-footer-strip">
          <slot name="footer" />
        </section>
      </main>

      <aside class="cp-side-panel cp-right-panel">
        <slot name="right" />
      </aside>
    </div>
  </div>
</template>

<script setup>
defineProps({
  metricsTitle: { type: String, default: '工艺指标' },
  overviewTitle: { type: String, default: '运行概况' },
  rosterTitle: { type: String, default: '设备清单' },
  metricCards: { type: Array, default: () => [] },
  overviewCards: { type: Array, default: () => [] },
  rosterItems: { type: Array, default: () => [] },
  selectedRosterKey: { type: String, default: '' },
  stageBadges: { type: Array, default: () => [] },
  statusChips: { type: Array, default: () => [] }
})

defineEmits(['select-roster'])
</script>

<style>
.cp-shell {
  position: relative;
  height: calc(100vh - var(--nav-height, 113px));
  display: flex;
  flex-direction: column;
  padding: 12px 14px 14px;
  background:
    radial-gradient(circle at 50% -10%, rgba(88, 214, 255, 0.30), transparent 36%),
    radial-gradient(circle at 16% 18%, rgba(72, 232, 255, 0.16), transparent 28%),
    radial-gradient(circle at 84% 20%, rgba(103, 152, 255, 0.16), transparent 28%),
    linear-gradient(180deg, #1E293B 0%, #1E293B 54%, #1E293B 100%);
  overflow: hidden;
}

.cp-aurora,
.cp-noise,
.cp-vignette,
.cp-gridlines,
.cp-flare,
.cp-edge-rail,
.cp-hud-node {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.cp-aurora {
  opacity: 0.52;
}

.cp-aurora-left {
  background: radial-gradient(circle at 14% 26%, rgba(43, 229, 255, 0.12), transparent 26%);
}

.cp-aurora-right {
  background: radial-gradient(circle at 88% 74%, rgba(97, 109, 255, 0.14), transparent 26%);
}

.cp-noise {
  opacity: 0.08;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, rgba(255, 255, 255, 0.8), transparent 90%);
}

.cp-vignette {
  background:
    radial-gradient(circle at 50% 48%, transparent 0%, rgba(16, 56, 88, 0.04) 54%, rgba(16, 56, 88, 0.10) 100%),
    linear-gradient(90deg, rgba(11, 42, 68, 0.14), transparent 16%, transparent 84%, rgba(11, 42, 68, 0.14));
  z-index: 0;
}

.cp-gridlines {
  opacity: 0.16;
  background-image:
    linear-gradient(rgba(111, 221, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(111, 221, 255, 0.045) 1px, transparent 1px),
    linear-gradient(135deg, rgba(82, 202, 255, 0.03) 0, rgba(82, 202, 255, 0.03) 2px, transparent 2px, transparent 54px);
  background-size: 64px 64px, 64px 64px, 54px 54px;
  mask-image: radial-gradient(circle at 50% 52%, rgba(255, 255, 255, 0.36), transparent 92%);
  animation: cp-grid-drift 24s linear infinite;
  z-index: 0;
}

.cp-flare {
  inset: auto 10% auto;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(103, 230, 255, 0.7), transparent);
  box-shadow:
    0 0 18px rgba(103, 230, 255, 0.2),
    0 0 40px rgba(53, 148, 255, 0.12);
  opacity: 0.7;
  z-index: 0;
}

.cp-flare-top {
  top: 14px;
  animation: cp-horizon-pulse 7s ease-in-out infinite;
}

.cp-flare-bottom {
  bottom: 12px;
  opacity: 0.45;
  animation: cp-horizon-pulse 9s ease-in-out infinite reverse;
}

.cp-edge-rail {
  inset: 20px auto 20px;
  width: 10px;
  border-radius: 999px;
  background: linear-gradient(180deg, transparent, rgba(67, 211, 255, 0.08), transparent);
  box-shadow:
    0 0 18px rgba(67, 211, 255, 0.08),
    inset 0 0 10px rgba(255, 255, 255, 0.03);
  overflow: hidden;
  z-index: 0;
}

.cp-edge-rail span {
  position: absolute;
  inset: -18% 1px auto;
  height: 20%;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(163, 244, 255, 0), rgba(163, 244, 255, 0.9), rgba(163, 244, 255, 0));
  box-shadow:
    0 0 12px rgba(94, 229, 255, 0.4),
    0 0 22px rgba(94, 229, 255, 0.2);
}

.cp-rail-left { left: 6px; }
.cp-rail-right { right: 6px; }

.cp-hud-node {
  inset: auto;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px solid rgba(88, 219, 255, 0.1);
  background:
    radial-gradient(circle at center, rgba(116, 236, 255, 0.14), transparent 42%),
    conic-gradient(from 0deg, rgba(116, 236, 255, 0), rgba(116, 236, 255, 0.48), rgba(116, 236, 255, 0));
  box-shadow:
    0 0 18px rgba(71, 200, 255, 0.14),
    inset 0 0 18px rgba(71, 200, 255, 0.08);
  opacity: 0.52;
  animation: cp-node-pulse 5.8s ease-in-out infinite;
  z-index: 0;
}

.cp-hud-node::before,
.cp-hud-node::after {
  content: '';
  position: absolute;
  inset: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
}

.cp-hud-node::before {
  width: 8px;
  height: 8px;
  background: #8ceeff;
  box-shadow: 0 0 16px rgba(122, 232, 255, 0.7);
}

.cp-hud-node::after {
  width: 100%;
  height: 100%;
  border: 1px solid rgba(122, 232, 255, 0.18);
  animation: cp-radar-ping 4.2s ease-out infinite;
}

.cp-node-top-left { left: 18px; top: 18px; }
.cp-node-top-right { right: 18px; top: 18px; animation-delay: 1.2s; }
.cp-node-bottom-left { left: 18px; bottom: 18px; animation-delay: 2.2s; }
.cp-node-bottom-right { right: 18px; bottom: 18px; animation-delay: 3.1s; }

.cp-command-deck {
  position: relative;
  flex: 1;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr) 332px;
  gap: 14px;
  min-height: 0;
}

.cp-side-panel,
.cp-stage-panel {
  min-height: 0;
}

.cp-side-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  z-index: 1;
  isolation: isolate;
}

.cp-left-panel::before,
.cp-right-panel::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 14px;
  width: 1px;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(85, 220, 255, 0.44), transparent);
  box-shadow: 0 0 16px rgba(85, 220, 255, 0.18);
  animation: cp-rail-sweep 6s linear infinite;
}

.cp-left-panel::before { right: 2px; }
.cp-right-panel::before { left: 2px; }

.cp-stage-panel {
  position: relative;
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(220px, 28%);
  gap: 14px;
  min-width: 0;
  z-index: 1;
}

.cp-panel-block,
.cp-stage-frame {
  position: relative;
  border: 1px solid rgba(0, 128, 255, 0.22);
  background:
    radial-gradient(circle at top left, rgba(0, 128, 255, 0.10), transparent 42%),
    linear-gradient(145deg, rgba(74, 168, 214, 0.16), rgba(34, 102, 142, 0.06) 48%, rgba(18, 58, 84, 0.04));
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.06),
    inset 0 8px 18px rgba(120, 214, 255, 0.03),
    0 8px 20px rgba(0, 0, 0, 0.03),
    0 0 0 1px rgba(147, 228, 255, 0.04),
    0 0 22px rgba(91, 182, 255, 0.05);
  backdrop-filter: saturate(122%);
  overflow: hidden;
  clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 14px, 100% 100%, 14px 100%, 0 calc(100% - 14px));
}

.cp-panel-block::before,
.cp-stage-frame::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(170, 240, 255, 0.10), transparent);
  transform: translateX(-100%);
  animation: cp-sweep 14s linear infinite;
}

.cp-panel-block::after,
.cp-stage-frame::after {
  content: '';
  position: absolute;
  inset: 1px;
  pointer-events: none;
  border: 1px solid rgba(168, 240, 255, 0.14);
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
  box-shadow:
    inset 0 0 22px rgba(87, 194, 255, 0.04),
    inset 0 1px 0 rgba(143, 228, 255, 0.08);
}

.cp-panel-title {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 14px 16px 0;
  padding-right: 8px;
  font-size: 1rem;
  font-weight: 700;
  color: #C9CDD4;
  letter-spacing: 0.04em;
  text-shadow:
    0 0 14px rgba(74, 210, 255, 0.18),
    0 0 22px rgba(74, 210, 255, 0.05);
}

.cp-panel-title::before {
  content: '';
  width: 26px;
  height: 3px;
  background: linear-gradient(90deg, #00e8ff, #86f8c8 55%, rgba(134, 248, 200, 0.2));
  box-shadow: 0 0 14px rgba(44, 225, 255, 0.46);
}

.cp-panel-title::after {
  content: '';
  width: 126px;
  height: 12px;
  margin-left: 8px;
  border-top: 1px solid rgba(123, 203, 243, 0.26);
  border-bottom: 1px solid rgba(123, 203, 243, 0.08);
  background:
    linear-gradient(90deg, rgba(77, 141, 214, 0.36), rgba(105, 177, 236, 0.16), transparent),
    linear-gradient(90deg, transparent, rgba(139, 238, 255, 0.68), transparent);
  background-size: 100% 100%, 64px 100%;
  background-repeat: no-repeat;
  animation: cp-title-scan 5.4s linear infinite;
  clip-path: polygon(0 0, 100% 0, 94% 100%, 0 100%);
}

.cp-metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.cp-metric-card,
.cp-overview-chip,
.cp-roster-item,
.cp-alarm-item,
.cp-detail-item {
  position: relative;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
}

.cp-metric-card {
  padding: 16px 15px 15px;
  min-height: 118px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background:
    radial-gradient(circle at top right, rgba(0, 128, 255, 0.10), transparent 40%),
    linear-gradient(180deg, rgba(72, 170, 214, 0.12), rgba(18, 60, 86, 0.03));
  backdrop-filter: saturate(118%);
}

.cp-metric-label {
  display: block;
  min-height: 20px;
  font-size: 0.875rem;
  line-height: 1.45;
  color: #C9CDD4;
  white-space: nowrap;
}

.cp-metric-value,
.cp-overview-chip strong,
.cp-status-chip strong {
  font-family: var(--font-display);
  font-variant-numeric: tabular-nums;
  color: transparent;
  -webkit-background-clip: text;
  background-clip: text;
}

.cp-metric-value {
  margin-top: 8px;
  font-size: 2rem;
  line-height: 1;
  font-weight: 700;
  letter-spacing: 0.02em;
  background-image: linear-gradient(180deg, #F2F3F5 0%, #8ceeff 58%, #0080ff 100%);
}

.cp-metric-unit {
  margin-top: 6px;
  font-size: 0.8125rem;
  color: #8ceeff;
}

.cp-metric-status {
  display: inline-flex;
  align-items: center;
  margin-top: 10px;
  padding: 4px 9px;
  font-size: 0.6875rem;
  border-radius: 999px;
  border: 1px solid transparent;
  white-space: nowrap;
}

.cp-metric-status.ok {
  background: rgba(33, 209, 141, 0.14);
  color: #76ffc8;
  border-color: rgba(92, 255, 188, 0.16);
}

.cp-metric-status.warn {
  background: rgba(255, 110, 110, 0.12);
  color: #ff9898;
  border-color: rgba(255, 143, 143, 0.16);
}

.cp-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 16px;
}

.cp-overview-chip {
  padding: 14px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background:
    radial-gradient(circle at top right, rgba(0, 128, 255, 0.10), transparent 36%),
    linear-gradient(180deg, rgba(70, 164, 206, 0.10), rgba(16, 54, 78, 0.03));
  backdrop-filter: saturate(118%);
}

.cp-overview-chip span {
  display: block;
  font-size: 0.8125rem;
  color: #C9CDD4;
}

.cp-overview-chip strong {
  display: block;
  margin-top: 10px;
  font-size: 1.875rem;
  letter-spacing: 0.03em;
  background-image: linear-gradient(180deg, #F2F3F5 0%, #8ceeff 52%, #0080ff 100%);
}

.cp-overview-chip.warning strong { background-image: linear-gradient(180deg, #fff1ec 0%, #ffc7b5 56%, #ff7d6a 100%); }

.cp-roster-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cp-device-roster {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  overflow: auto;
}

.cp-roster-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 15px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background:
    linear-gradient(90deg, rgba(0, 128, 255, 0.06), transparent 36%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.10), rgba(16, 54, 78, 0.03));
  color: #F2F3F5;
  cursor: pointer;
  text-align: left;
  backdrop-filter: saturate(118%);
}

.cp-roster-item.active,
.cp-roster-item:hover {
  border-color: rgba(94, 225, 255, 0.42);
  box-shadow:
    0 10px 24px rgba(0, 0, 0, 0.18),
    inset 0 0 18px rgba(60, 191, 255, 0.08),
    0 0 0 1px rgba(105, 225, 255, 0.08);
}

.cp-roster-item.fault { border-color: rgba(255, 112, 112, 0.38); }

.cp-roster-name {
  font-size: 1rem;
  font-weight: 600;
  color: #F2F3F5;
}

.cp-roster-meta {
  margin-top: 4px;
  font-size: 0.8125rem;
  color: #C9CDD4;
}

.cp-roster-state {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8125rem;
  color: #C9CDD4;
}

.cp-state-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.cp-state-dot.running { background: #30e6ad; box-shadow: 0 0 12px rgba(48, 230, 173, 0.6); }
.cp-state-dot.idle { background: #0080ff; box-shadow: 0 0 12px rgba(0, 128, 255, 0.5); }
.cp-state-dot.fault { background: #ff7a7a; box-shadow: 0 0 12px rgba(255, 122, 122, 0.6); }

.cp-hero-stage,
.cp-footer-strip {
  min-height: 0;
}

.cp-stage-frame {
  height: 100%;
  background:
    radial-gradient(circle at 50% 0%, rgba(100, 221, 255, 0.10), transparent 28%),
    linear-gradient(180deg, rgba(46, 118, 162, 0.08), rgba(18, 54, 80, 0.03)),
    linear-gradient(180deg, rgba(42, 112, 156, 0.06), rgba(14, 42, 64, 0.02));
}

.cp-stage-content {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
}

.cp-stage-content canvas {
  width: 100%;
  height: 100%;
  display: block;
  background:
    radial-gradient(circle at 50% 18%, rgba(69, 156, 255, 0.04), transparent 26%),
    linear-gradient(180deg, rgba(20, 58, 86, 0.36), rgba(16, 48, 72, 0.22));
}

.cp-mobile-stage-copy {
  position: absolute;
  inset: auto 18px 18px 18px;
  z-index: 4;
  padding: 16px;
  border: 1px solid rgba(0, 128, 255, 0.22);
  background: rgba(30, 41, 59, 0.82);
  backdrop-filter: none;
  color: #C9CDD4;
}

.cp-mobile-stage-title {
  font-size: 1rem;
  font-weight: 700;
}

.cp-mobile-stage-text {
  margin-top: 8px;
  font-size: 0.8125rem;
  line-height: 1.6;
  color: #C9CDD4;
}

.cp-stage-pin {
  position: absolute;
  z-index: 6;
  min-width: 0;
  padding: 4px 8px 5px 7px;
  border: 1px solid rgba(92, 211, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(10, 36, 56, 0.14), transparent 40%),
    linear-gradient(180deg, rgba(5, 17, 29, 0.88), rgba(3, 12, 23, 0.8));
  color: #F2F3F5;
  transform: translate(-50%, -50%);
  cursor: pointer;
  transition: 0.2s ease;
  backdrop-filter: none;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 140px;
}

.cp-stage-pin:hover,
.cp-stage-pin.active {
  border-color: rgba(117, 236, 255, 0.42);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.18), 0 0 18px rgba(56, 200, 255, 0.12);
}

.cp-stage-pin.fault { border-color: rgba(255, 126, 126, 0.65); }

.cp-stage-pin::after {
  content: '';
  position: absolute;
  left: 14px;
  bottom: -14px;
  width: 1px;
  height: 14px;
  background: linear-gradient(180deg, rgba(117, 236, 255, 0.75), transparent);
}

.cp-pin-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  flex: none;
  background: #0080ff;
  box-shadow: 0 0 10px rgba(0, 128, 255, 0.55);
}

.cp-pin-status-dot.running { background: #30e6ad; box-shadow: 0 0 10px rgba(48, 230, 173, 0.6); }
.cp-pin-status-dot.idle { background: #0080ff; box-shadow: 0 0 10px rgba(0, 128, 255, 0.5); }
.cp-pin-status-dot.fault { background: #ff7a7a; box-shadow: 0 0 10px rgba(255, 122, 122, 0.62); }

.cp-pin-name {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.04em;
  color: #F2F3F5;
  white-space: nowrap;
}

.cp-pin-divider {
  width: 1px;
  height: 12px;
  background: linear-gradient(180deg, rgba(102, 223, 255, 0.15), rgba(102, 223, 255, 0.75), rgba(102, 223, 255, 0.15));
  flex: none;
}

.cp-pin-value {
  display: block;
  font-size: 0.625rem;
  line-height: 1;
  color: rgba(127, 205, 234, 0.88);
  white-space: nowrap;
}

.cp-stage-canopy,
.cp-stage-orbit,
.cp-stage-spectrum,
.cp-stage-prism,
.cp-stage-pulse,
.cp-stage-data-stream,
.cp-stage-radar,
.cp-stage-hud-grid,
.cp-stage-scanline,
.cp-stage-energy-ring,
.cp-stage-axis-label,
.cp-stage-status-stack {
  position: absolute;
  pointer-events: none;
}

.cp-stage-canopy {
  top: 0;
  left: 16%;
  right: 16%;
  height: 88px;
  z-index: 2;
  background:
    radial-gradient(circle at 50% 0%, rgba(133, 224, 255, 0.16), transparent 58%),
    linear-gradient(180deg, rgba(53, 166, 255, 0.1), transparent 75%);
  clip-path: polygon(0 0, 20% 0, 24% 18px, 76% 18px, 80% 0, 100% 0, 92% 100%, 8% 100%);
  border-top: 1px solid rgba(113, 221, 255, 0.46);
}

.cp-stage-prism {
  z-index: 2;
  width: 110px;
  height: 220px;
  opacity: 0.24;
  background:
    linear-gradient(180deg, rgba(91, 228, 255, 0.18), rgba(91, 228, 255, 0.02)),
    linear-gradient(90deg, rgba(91, 228, 255, 0), rgba(91, 228, 255, 0.48), rgba(91, 228, 255, 0));
  clip-path: polygon(20% 0, 100% 10%, 82% 100%, 0 90%);
}

.cp-prism-left { left: 42px; top: 108px; transform: skewY(-16deg); animation: cp-prism-float 9s ease-in-out infinite; }
.cp-prism-right { right: 56px; bottom: 110px; transform: skewY(16deg); animation: cp-prism-float-right 11s ease-in-out infinite; }

.cp-stage-orbit {
  z-index: 2;
  border-radius: 50%;
  border: 1px dashed rgba(85, 220, 255, 0.12);
}

.cp-orbit-left { left: -72px; bottom: 48px; width: 220px; height: 220px; }
.cp-orbit-right { right: -84px; top: 92px; width: 300px; height: 300px; }

.cp-stage-spectrum {
  z-index: 2;
  width: 120px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(92, 228, 255, 0.54), transparent);
  box-shadow: 0 0 14px rgba(92, 228, 255, 0.18);
  opacity: 0.75;
}

.cp-spectrum-left { left: 26px; top: 96px; transform: rotate(-16deg); }
.cp-spectrum-right { right: 30px; bottom: 92px; transform: rotate(16deg); }

.cp-stage-data-stream {
  z-index: 2;
  width: 156px;
  height: 18px;
  border-top: 1px solid rgba(96, 223, 255, 0.22);
  border-bottom: 1px solid rgba(96, 223, 255, 0.08);
  background:
    repeating-linear-gradient(90deg, rgba(108, 230, 255, 0) 0 10px, rgba(108, 230, 255, 0.16) 10px 18px),
    linear-gradient(90deg, transparent, rgba(108, 230, 255, 0.54), transparent);
  opacity: 0.74;
  overflow: hidden;
}

.cp-stage-data-stream::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(202, 247, 255, 0.92), transparent);
  transform: translateX(-100%);
}

.cp-stream-left { left: 18px; top: 132px; clip-path: polygon(0 0, calc(100% - 14px) 0, 100% 50%, calc(100% - 14px) 100%, 0 100%, 10px 50%); }
.cp-stream-right { right: 18px; bottom: 118px; clip-path: polygon(14px 0, 100% 0, calc(100% - 10px) 50%, 100% 100%, 14px 100%, 0 50%); }

.cp-stage-radar {
  z-index: 2;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  border: 1px solid rgba(88, 221, 255, 0.14);
  background:
    radial-gradient(circle at center, rgba(88, 221, 255, 0.08), transparent 44%),
    conic-gradient(from 0deg, rgba(88, 221, 255, 0), rgba(88, 221, 255, 0.46), rgba(88, 221, 255, 0));
  opacity: 0.46;
}

.cp-stage-radar::after {
  content: '';
  position: absolute;
  inset: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 1px solid rgba(128, 234, 255, 0.14);
  animation: cp-radar-ping 3.8s ease-out infinite;
}

.cp-radar-left { left: 26px; bottom: 42px; }
.cp-radar-right { right: 30px; top: 152px; }

.cp-stage-pulse {
  z-index: 2;
  border-radius: 50%;
  border: 1px solid rgba(91, 229, 255, 0.12);
  opacity: 0.42;
}

.cp-pulse-one { width: 180px; height: 180px; left: 18%; top: 22%; animation: cp-soft-pulse 6.5s ease-in-out infinite; }
.cp-pulse-two { width: 230px; height: 230px; right: 14%; bottom: 18%; animation: cp-soft-pulse 8s ease-in-out infinite reverse; }

.cp-stage-hud-grid {
  inset: 0;
  z-index: 3;
  background-image:
    linear-gradient(rgba(92, 229, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(92, 229, 255, 0.04) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: radial-gradient(circle at 50% 52%, rgba(255, 255, 255, 0.5), transparent 88%);
}

.cp-stage-scanline {
  inset: -20% 0 auto;
  z-index: 3;
  height: 160px;
  background: linear-gradient(180deg, transparent, rgba(101, 228, 255, 0.08), transparent);
  opacity: 0.44;
  animation: cp-sweep-vertical 10s linear infinite;
}

.cp-stage-energy-ring {
  z-index: 3;
  border-radius: 50%;
  border: 1px solid rgba(82, 214, 255, 0.08);
}

.cp-ring-one { width: 420px; height: 420px; right: -80px; top: 110px; border-style: dashed; animation: cp-rotate 24s linear infinite; }
.cp-ring-two { width: 260px; height: 260px; left: -60px; bottom: 60px; opacity: 0.7; animation: cp-rotate-reverse 20s linear infinite; }

.cp-stage-axis-label {
  z-index: 3;
  font-size: 0.6875rem;
  letter-spacing: 0.18em;
  color: rgba(148, 227, 255, 0.52);
}

.cp-axis-x { right: 32px; bottom: 24px; }
.cp-axis-y { left: 24px; bottom: 120px; writing-mode: vertical-rl; text-orientation: mixed; }

.cp-frame-corner {
  position: absolute;
  width: 34px;
  height: 34px;
  border: 2px solid rgba(79, 218, 255, 0.68);
  z-index: 5;
  opacity: 0.58;
}

.cp-top-left { top: 12px; left: 12px; border-right: none; border-bottom: none; }
.cp-top-right { top: 12px; right: 12px; border-left: none; border-bottom: none; }
.cp-bottom-left { bottom: 12px; left: 12px; border-right: none; border-top: none; }
.cp-bottom-right { bottom: 12px; right: 12px; border-left: none; border-top: none; }

.cp-stage-badges {
  position: absolute;
  left: 18px;
  top: 18px;
  z-index: 4;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cp-stage-badge {
  min-width: 156px;
  padding: 10px 12px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background:
    linear-gradient(90deg, rgba(59, 198, 255, 0.06), transparent 42%),
    linear-gradient(180deg, rgba(18, 62, 92, 0.18), rgba(10, 34, 52, 0.08));
  backdrop-filter: none;
  box-shadow:
    0 8px 18px rgba(0, 0, 0, 0.10),
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    inset 0 0 18px rgba(74, 198, 255, 0.04);
}

.cp-stage-badge span { display: block; font-size: 0.75rem; color: #C9CDD4; }
.cp-stage-badge strong { display: block; margin-top: 6px; font-size: 0.875rem; color: #F2F3F5; }

.cp-stage-status-stack {
  top: 18px;
  right: 18px;
  display: grid;
  gap: 10px;
  z-index: 5;
}

.cp-status-chip {
  min-width: 110px;
  padding: 10px 12px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  background:
    linear-gradient(90deg, rgba(77, 202, 255, 0.06), transparent 42%),
    linear-gradient(180deg, rgba(18, 62, 92, 0.18), rgba(10, 34, 52, 0.08));
  backdrop-filter: none;
  box-shadow:
    inset 0 0 18px rgba(0, 232, 255, 0.03),
    0 8px 18px rgba(0, 0, 0, 0.10);
}

.cp-status-chip span {
  display: block;
  font-size: 0.6875rem;
  letter-spacing: 0.16em;
  color: rgba(179, 229, 255, 0.66);
}

.cp-status-chip strong {
  display: block;
  margin-top: 6px;
  font-size: 1.5rem;
  letter-spacing: 0.04em;
  background-image: linear-gradient(180deg, #F2F3F5 0%, #8ceeff 56%, #0080ff 100%);
}

.cp-status-chip.cyan { box-shadow: inset 3px 0 0 #00e8ff; }
.cp-status-chip.green { box-shadow: inset 3px 0 0 #2fe4a6; }
.cp-status-chip.amber { box-shadow: inset 3px 0 0 #ffbf66; }
.cp-status-chip.blue { box-shadow: inset 3px 0 0 #6aa8ff; }
.cp-status-chip.red { box-shadow: inset 3px 0 0 #ff6e8c; }

.cp-footer-strip {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1.1fr;
  gap: 14px;
  min-height: 0;
}

.cp-footer-strip > * {
  min-height: 0;
}

.cp-shell .cp-mini-table-block,
.cp-shell .cp-control-block,
.cp-shell .cp-alarm-block,
.cp-shell .cp-summary-block,
.cp-shell .cp-list-block,
.cp-shell .cp-chart-block {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cp-shell .cp-detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 10px;
  position: relative;
}

.cp-shell .cp-detail-head::after {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(87, 218, 255, 0.42), rgba(87, 218, 255, 0.06), transparent);
}

.cp-shell .cp-detail-name {
  font-size: 1.375rem;
  font-weight: 700;
  color: #F2F3F5;
}

.cp-shell .cp-detail-type {
  margin-top: 6px;
  font-size: 0.8125rem;
  color: #C9CDD4;
}

.cp-shell .cp-detail-pill,
.cp-shell .cp-state-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(115, 203, 244, 0.16);
  background: rgba(30, 41, 59, 0.12);
  color: #F2F3F5;
  font-size: 0.8125rem;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.06em;
  text-align: center;
}

.cp-shell .cp-detail-pill.running,
.cp-shell .cp-state-badge.running { color: #88ffd8; border-color: rgba(48,230,173,.36); background: linear-gradient(135deg, rgba(35,153,116,.28), rgba(13,73,56,.22)); }
.cp-shell .cp-detail-pill.idle,
.cp-shell .cp-state-badge.idle { color: #8ad9ff; border-color: rgba(0,128,255,.34); background: linear-gradient(135deg, rgba(33,100,156,.26), rgba(11,52,83,.2)); }
.cp-shell .cp-detail-pill.fault,
.cp-shell .cp-state-badge.fault { color: #ffaeae; border-color: rgba(255,122,122,.36); background: linear-gradient(135deg, rgba(144,38,57,.32), rgba(89,18,34,.26)); }
.cp-shell .cp-state-badge.ready { color: #95f6c9; border-color: rgba(80,235,184,.32); background: linear-gradient(135deg, rgba(28,122,92,.24), rgba(10,63,48,.2)); }
.cp-shell .cp-state-badge.blue { color: #9fdfff; border-color: rgba(86,193,255,.34); background: linear-gradient(135deg, rgba(33,100,156,.26), rgba(11,52,83,.2)); }
.cp-shell .cp-state-badge.amber { color: #ffab00; border-color: rgba(255,190,90,.36); background: linear-gradient(135deg, rgba(135,96,22,.28), rgba(77,54,11,.24)); }
.cp-shell .cp-state-badge.off { color: #C9CDD4; border-color: rgba(102,153,180,.2); background: linear-gradient(135deg, rgba(20,49,69,.22), rgba(11,28,42,.2)); }

.cp-shell .cp-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 16px;
}

.cp-shell .cp-detail-item {
  padding: 12px;
  border: 1px solid rgba(0, 128, 255, 0.22);
  background: linear-gradient(90deg, rgba(0, 128, 255, 0.06), transparent 45%), rgba(30, 41, 59, 0.08);
  backdrop-filter: saturate(116%);
}

.cp-shell .cp-detail-item span {
  display: block;
  font-size: 0.8125rem;
  color: #C9CDD4;
}

.cp-shell .cp-detail-item strong {
  display: block;
  margin-top: 8px;
  font-size: 1.0625rem;
  color: #F2F3F5;
}

.cp-shell .cp-mini-table {
  flex: 1;
  min-height: 0;
  padding: 12px 14px 14px;
  overflow: auto;
}

.cp-shell .cp-mini-row {
  display: grid;
  grid-template-columns: 1.5fr 0.8fr 0.8fr 0.8fr;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(91, 192, 255, 0.12);
  font-size: 0.9375rem;
  align-items: center;
  color: #F2F3F5;
}

.cp-shell .cp-mini-row.header {
  color: #C9CDD4;
  font-weight: 700;
  font-size: 1rem;
}

.cp-shell .cp-alarm-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px 14px;
  overflow-y: auto;
}

.cp-shell .cp-alarm-item {
  padding: 13px 14px;
  border: 1px solid rgba(0, 128, 255, 0.18);
  border-left: 3px solid rgba(0, 128, 255, 0.3);
  background:
    linear-gradient(90deg, rgba(0, 128, 255, 0.06), transparent 42%),
    linear-gradient(180deg, rgba(30, 41, 59, 0.10), rgba(30, 41, 59, 0.03));
  backdrop-filter: saturate(116%);
}

.cp-shell .cp-alarm-item.ok { border-left-color: rgba(69, 214, 170, 0.7); }
.cp-shell .cp-alarm-item.info { border-left-color: rgba(88, 171, 255, 0.7); }
.cp-shell .cp-alarm-item.warn { border-left-color: rgba(255, 123, 123, 0.8); }

.cp-shell .cp-alarm-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 1rem;
  font-weight: 600;
  color: #F2F3F5;
}

.cp-shell .cp-alarm-desc {
  margin-top: 8px;
  font-size: 0.875rem;
  line-height: 1.5;
  color: #C9CDD4;
}

.cp-shell .cp-list-grid {
  display: grid;
  gap: 10px;
  padding: 12px 14px 14px;
  overflow: auto;
}

.cp-shell .cp-list-item {
  padding: 12px 13px;
  border: 1px solid rgba(0, 128, 255, 0.14);
  background: linear-gradient(90deg, rgba(58, 203, 255, 0.05), transparent 45%), rgba(26, 84, 114, 0.10);
  color: #F2F3F5;
  backdrop-filter: saturate(116%);
}

.cp-shell .cp-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 12px 14px 14px;
}

.cp-shell .cp-summary-card {
  padding: 14px 12px;
  border: 1px solid rgba(0, 128, 255, 0.14);
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.12), rgba(18, 58, 84, 0.04));
  backdrop-filter: saturate(116%);
}

.cp-shell .cp-summary-card span {
  display: block;
  font-size: 0.75rem;
  color: #C9CDD4;
}

.cp-shell .cp-summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 1.625rem;
  font-family: var(--font-display);
  color: #F2F3F5;
}

.cp-device-roster::-webkit-scrollbar,
.cp-shell .cp-mini-table::-webkit-scrollbar,
.cp-shell .cp-alarm-list::-webkit-scrollbar,
.cp-shell .cp-list-grid::-webkit-scrollbar {
  width: 5px;
}

.cp-device-roster::-webkit-scrollbar-thumb,
.cp-shell .cp-mini-table::-webkit-scrollbar-thumb,
.cp-shell .cp-alarm-list::-webkit-scrollbar-thumb,
.cp-shell .cp-list-grid::-webkit-scrollbar-thumb {
  background: rgba(80, 200, 255, 0.25);
  border-radius: 999px;
}

@keyframes cp-sweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes cp-rail-sweep {
  0% { transform: translateY(-130%); opacity: 0; }
  12% { opacity: 1; }
  88% { opacity: 1; }
  100% { transform: translateY(620%); opacity: 0; }
}

@keyframes cp-sweep-vertical {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(100%); }
}

</style>
