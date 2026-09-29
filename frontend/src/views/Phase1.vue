<template>
  <div class="phase1-page">
    <div class="wave-overlay"></div>
    <div class="placeholder-center">
      <div class="placeholder-icon">
        <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="8" y="24" width="48" height="32" rx="2" stroke="currentColor" stroke-width="2" fill="none" />
          <path d="M16 24V16a16 16 0 0 1 32 0v8" stroke="currentColor" stroke-width="2" fill="none" />
          <rect x="24" y="36" width="16" height="8" rx="1" stroke="currentColor" stroke-width="2" fill="rgba(0,232,255,0.1)" />
          <line x1="32" y1="36" x2="32" y2="44" stroke="currentColor" stroke-width="1.5" />
        </svg>
      </div>
      <div class="placeholder-title">一期建筑</div>
      <div class="placeholder-sub">Phase 1 Building</div>
      <div class="placeholder-badge">内容待建中</div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.phase1-page {
  position: relative;
  min-height: calc(100vh - var(--nav-height, 106px));
  background:
    radial-gradient(ellipse at 20% 50%, rgba(0, 232, 255, 0.03) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(0, 232, 255, 0.03) 0%, transparent 50%),
    linear-gradient(180deg, var(--color-bg-deep) 0%, var(--color-bg-main) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.wave-overlay {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(0deg, transparent, transparent 3px, rgba(0, 232, 255, 0.006) 3px, rgba(0, 232, 255, 0.006) 4px);
  pointer-events: none;
  z-index: 0;
}

.placeholder-center {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  color: rgba(0, 232, 255, 0.3);
  animation: float 3s ease-in-out infinite;
}

.placeholder-icon svg {
  width: 100%;
  height: 100%;
}

.placeholder-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: rgba(0, 232, 255, 0.85);
  letter-spacing: 4px;
  text-shadow: 0 0 20px rgba(0, 232, 255, 0.3);
}

.placeholder-sub {
  font-size: 0.8125rem;
  color: rgba(0, 232, 255, 0.35);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.placeholder-badge {
  margin-top: 12px;
  padding: 6px 20px;
  font-size: 0.75rem;
  color: rgba(0, 232, 255, 0.6);
  border: 1px solid rgba(0, 232, 255, 0.15);
  border-radius: 20px;
  background: rgba(0, 232, 255, 0.04);
  letter-spacing: 2px;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
</style>
