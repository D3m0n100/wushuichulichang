<template>
  <div class="ph1-page">
    <div class="wave-overlay"></div>
    <div class="page-aurora aurora-left"></div>
    <div class="page-aurora aurora-right"></div>
    <div class="page-noise"></div>

    <div class="command-deck">
      <div class="placeholder-card">
        <div class="placeholder-icon">🏗️</div>
        <div class="placeholder-title">一期建筑</div>
        <div class="placeholder-sub">PHASE 1 BUILDING</div>
        <div class="placeholder-divider"></div>
        <div class="placeholder-status">
          <span class="status-dot"></span>
          <span>正在待建</span>
        </div>
        <div class="placeholder-desc">
          该工艺段尚在规划建设中，相关设备与监控功能将在建设完成后上线。
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ph1-page {
  position: relative;
  height: calc(100vh - var(--nav-height, 113px));
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #0F172A 0%, #0F172A 100%);
  overflow: hidden;
}

.wave-overlay {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background: repeating-linear-gradient(0deg, transparent 0 2px, rgba(0, 232, 255, 0.015) 2px 4px);
  mask-image: linear-gradient(180deg, rgba(255,255,255,0.6), transparent 90%);
}

.page-aurora { position: absolute; inset: 0; pointer-events: none; opacity: 0.4; }
.aurora-left { background: radial-gradient(circle at 14% 26%, rgba(43, 229, 255, 0.1), transparent 26%); }
.aurora-right { background: radial-gradient(circle at 88% 18%, rgba(104, 112, 255, 0.1), transparent 24%); }
.page-noise {
  position: absolute; inset: 0; pointer-events: none; opacity: 0.06;
  background-image: linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 42px 42px;
}

.command-deck {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-card {
  text-align: center;
  padding: 48px 64px;
  border: 1px solid rgba(0, 128, 255, 0.22);
  background: rgba(15, 23, 42, 0.88);
  backdrop-filter: saturate(118%);
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow:
    inset 0 0 0 1px rgba(255,255,255,0.04),
    inset 0 8px 24px rgba(120, 214, 255, 0.03),
    0 12px 32px rgba(0, 0, 0, 0.1);
}

.placeholder-icon {
  font-size: 4rem;
  margin-bottom: 16px;
}

.placeholder-title {
  font-size: 2rem;
  font-weight: 700;
  color: #F2F3F5;
  text-shadow: 0 0 18px rgba(79, 214, 255, 0.18);
}

.placeholder-sub {
  font-size: 1rem;
  color: rgba(140, 238, 255, 0.5);
  letter-spacing: 4px;
  margin-top: 8px;
}

.placeholder-divider {
  width: 120px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 128, 255, 0.5), transparent);
  margin: 24px auto;
  box-shadow: 0 0 8px rgba(0, 232, 255, 0.2);
}

.placeholder-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 1.375rem;
  font-weight: 600;
  color: #ffbf66;
  margin-bottom: 16px;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ffbf66;
  box-shadow: 0 0 12px rgba(255, 191, 102, 0.6);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.placeholder-desc {
  font-size: 0.9375rem;
  color: #C9CDD4;
  line-height: 1.6;
  max-width: 360px;
}
</style>
