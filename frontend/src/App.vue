<template>
  <div class="app">
    <!-- Ambient particles -->
    <div class="ambient-particles">
      <div v-for="n in 20" :key="n" class="particle" :style="particleStyle(n)"></div>
    </div>

    <!-- 左侧隐藏式导航栏 -->
    <NavMenu />
    <!-- 全局设备故障告警弹窗 -->
    <GlobalAlarm />
    <!-- 路由页面出口 -->
    <div class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import NavMenu from '@/components/layout/NavMenu.vue'
import GlobalAlarm from '@/components/GlobalAlarm.vue'

const particleStyle = (n) => {
  const seed = n * 7.3
  const left = ((seed * 13.7) % 100)
  const top = ((seed * 17.3) % 100)
  const size = 1 + (n % 3)
  const delay = (n * 0.8) % 20
  const duration = 15 + (n % 10) * 2
  const opacity = 0.08 + (n % 5) * 0.04
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    opacity
  }
}
</script>

<style>
/* ========== CSS Variables - Design Tokens ========== */
:root {
  --font-display: 'Microsoft YaHei', 'Source Han Sans SC', sans-serif;
  --font-data: 'Microsoft YaHei', 'Source Han Sans SC', sans-serif;
  --font-body: 'Microsoft YaHei', 'Source Han Sans SC', sans-serif;

  --color-bg-deep: #0F172A;
  --color-bg-main: #0F172A;
  --color-bg-card: rgba(30, 41, 59, 0.85);
  --color-bg-card-hover: rgba(30, 41, 59, 0.95);

  --color-primary: #00e8ff;
  --color-primary-dim: rgba(0, 232, 255, 0.5);
  --color-primary-ghost: rgba(0, 232, 255, 0.08);
  --color-secondary: #0080ff;
  --color-accent: #7b61ff;
  --color-success: #00e676;
  --color-warning: #ffab00;
  --color-danger: #ff1744;

  --glow-primary: 0 0 20px rgba(0, 232, 255, 0.3), 0 0 40px rgba(0, 232, 255, 0.1);
  --glow-success: 0 0 15px rgba(0, 230, 118, 0.4);
  --glow-danger: 0 0 15px rgba(255, 23, 68, 0.4);
  --glow-warning: 0 0 15px rgba(255, 171, 0, 0.4);

  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 16px;
  --radius-pill: 999px;

  --border-subtle: 1px solid rgba(0, 232, 255, 0.12);
  --border-glow: 1px solid rgba(0, 232, 255, 0.3);
  --border-bright: 1px solid rgba(0, 232, 255, 0.6);

  --glass-bg: rgba(30, 41, 59, 0.75);
  --glass-blur: none;
  --glass-border: 1px solid rgba(0, 232, 255, 0.15);
}

/* ========== Global Reset ========== */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  font-size: 16px;
}

@media (min-width: 1800px) and (max-width: 1920px) and (min-height: 900px) and (max-height: 1080px) {
  html {
    font-size: 18px;
  }
}

html, body {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

#app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

body {
  font-family: var(--font-body);
  background: var(--color-bg-deep);
  color: #F2F3F5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.main-content {
  margin-left: 0;
  margin-top: var(--nav-height, 132px);
  flex: 1;
  min-height: calc(100vh - var(--nav-height, 132px));
  overflow-y: auto;
}

@media (max-width: 768px) {
  .main-content {
    margin-top: 0;
  }
}

/* ========== Ambient Floating Particles ========== */
.ambient-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: var(--color-primary);
  animation: particle-drift linear infinite;
}

@keyframes particle-drift {
  0% { transform: translate(0, 0) scale(1); opacity: 0; }
  10% { opacity: var(--particle-opacity, 0.1); }
  50% { transform: translate(30px, -80px) scale(1.5); }
  90% { opacity: var(--particle-opacity, 0.1); }
  100% { transform: translate(-20px, -160px) scale(0.5); opacity: 0; }
}

/* ========== Page Transition ========== */
.page-fade-enter-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.page-fade-leave-active {
  transition: opacity 0.2s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
}

/* ========== Custom Scrollbar ========== */
::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--color-primary), var(--color-secondary));
  border-radius: 3px;
  box-shadow: 0 0 6px var(--color-primary-dim);
}
::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #33eeff, #0080ff);
  box-shadow: 0 0 10px var(--color-primary-dim);
}

/* ========== Element Plus Overrides ========== */
.el-table {
  background-color: transparent !important;
  --el-table-border-color: rgba(0, 232, 255, 0.1) !important;
}

.el-table__header th {
  background-color: transparent !important;
  color: var(--color-primary) !important;
  border-bottom: var(--border-glow) !important;
  font-family: var(--font-body);
  font-weight: 600;
}

.el-table__row {
  background-color: transparent !important;
}

.el-table__row td {
  background-color: transparent !important;
  border-bottom: 1px solid rgba(0, 232, 255, 0.05) !important;
  transition: all 0.25s ease;
}

.el-table__row:hover td {
  background: var(--color-primary-ghost) !important;
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(15, 23, 42, 0.2) !important;
}

.el-table .cell {
  padding: 0 10px !important;
  line-height: 1.6 !important;
  font-family: var(--font-data);
  font-size: 0.75rem !important;
}

.el-table td, .el-table th {
  font-size: 0.75rem !important;
}

/* ========== DataV Overrides ========== */
.dv-border-box-1, .dv-border-box-2, .dv-border-box-3,
.dv-border-box-4, .dv-border-box-5, .dv-border-box-6,
.dv-border-box-7, .dv-border-box-8, .dv-border-box-9,
.dv-border-box-10, .dv-border-box-11, .dv-border-box-12,
.dv-border-box-13 {
  background-color: var(--glass-bg) !important;
}

.dv-decoration-1, .dv-decoration-2, .dv-decoration-3,
.dv-decoration-4, .dv-decoration-5 {
  color: var(--color-primary);
}

/* ========== Selection ========== */
::selection {
  background: rgba(0, 232, 255, 0.25);
  color: #fff;
}
</style>
