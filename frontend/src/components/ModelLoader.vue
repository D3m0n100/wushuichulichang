<template>
  <Transition name="fade-loader">
    <div v-if="!modelLoaded" class="model-loader">
      <div class="loader-bar-wrapper">
        <div class="loader-bar-track">
          <div class="loader-bar-fill" :style="{ width: loadingProgress + '%' }">
            <div class="loader-bar-glow"></div>
          </div>
        </div>
        <div class="loader-bar-info">
          <span class="loader-label">模型加载中</span>
          <span class="loader-percent">{{ loadingProgress }}%</span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  modelLoaded: {
    type: Boolean,
    required: true
  },
  loadingProgress: {
    type: Number,
    required: true
  }
})
</script>

<style scoped>
.model-loader {
  position: absolute;
  inset: 0;
  z-index: 10;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loader-bar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.loader-bar-track {
  width: 280px;
  height: 3px;
  border-radius: 2px;
  background: rgba(56, 217, 255, 0.1);
  overflow: visible;
  position: relative;
}

.loader-bar-fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, #38d9ff, #7dffd1);
  transition: width 0.3s ease;
  position: relative;
}

.loader-bar-glow {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translate(50%, -50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7dffd1;
  box-shadow: 0 0 8px rgba(125, 255, 209, 0.8), 0 0 20px rgba(56, 217, 255, 0.4);
}

.loader-bar-info {
  display: flex;
  justify-content: space-between;
  width: 280px;
}

.loader-label {
  font-size: 0.75rem;
  color: rgba(140, 238, 255, 0.6);
  letter-spacing: 0.06em;
}

.loader-percent {
  font-size: 0.75rem;
  font-family: var(--font-data), monospace;
  color: #8ceeff;
  letter-spacing: 0.04em;
}

.fade-loader-enter-active,
.fade-loader-leave-active {
  transition: opacity 0.5s ease;
}

.fade-loader-enter-from,
.fade-loader-leave-to {
  opacity: 0;
}
</style>
