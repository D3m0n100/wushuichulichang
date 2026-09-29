<template>
  <div class="nav-wrapper">
    <!-- Mobile overlay -->
    <div v-if="menuOpen" class="menu-mask" @click="closeMenu"></div>

    <!-- ========== Main Header ========== -->
    <div class="header-main">
      <div class="header-glow"></div>
      <div class="header-energy energy-left"></div>
      <div class="header-energy energy-right"></div>
      <div class="header-node node-left"></div>
      <div class="header-node node-right"></div>

      <div class="main-left">
        <!-- Hamburger (mobile) -->
        <div class="hamburger" @click="toggleMenu" :class="{ active: menuOpen }">
          <span></span><span></span><span></span>
        </div>

        <div class="logo-area">
          <img src="/logo_white_text_transparent.png" alt="logo" class="logo-img" />
        </div>
      </div>

      <!-- Centered System Title -->
      <div class="system-title">
        <div class="title-decoration left"></div>
        <div class="title-content">
          <div class="title-main">纪南污水处理厂智能化运营管理系统</div>
          <div class="title-sub">JINAN  Wastewater Treatment Plant Management System</div>
        </div>
        <div class="title-decoration right"></div>
      </div>

      <div class="main-right">
        <!-- Clock -->
        <div class="time-weather-group">
          <div class="time-box">
            <div class="time-date">{{ currentDate }}</div>
            <div class="time-main">{{ currentTime }}</div>
            <div class="time-week">{{ currentWeek }}</div>
          </div>
        </div>

        <div v-if="showUserInfo" class="user-info">
          <el-button type="info" size="small" @click="toggleFullscreen" class="icon-btn">
            {{ isFullscreen ? '退出全屏' : '全屏' }}
          </el-button>
          <div class="user-badge">
            <span class="username">{{ username }}</span>
          </div>
          <button @click="logout" class="logout-btn">
            <span class="logout-icon">⎋</span>
            <span class="logout-text">退出</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== Sub Navigation ========== -->
    <div v-if="showSubNav" class="header-sub">
      <div class="notch-bg">
        <div class="notch-pulse pulse-left"></div>
        <div class="notch-pulse pulse-right"></div>
        <el-menu
            :default-active="activeIndex"
            class="nav-menu"
            :class="{ 'menu-open': menuOpen }"
            mode="horizontal"
            :router="true"
            :ellipsis="false"
            background-color="transparent"
            text-color="#fff"
            active-text-color="#00e8ff"
        >
          <div class="menu-items" :style="{ display: menuOpen ? 'flex' : '' }">
            <el-menu-item index="/" @click="closeMenu">
              <span class="nav-icon">•</span> 首页
            </el-menu-item>
            <el-menu-item index="/process-control" @click="closeMenu">
              <span class="nav-icon">•</span> 工艺总控
            </el-menu-item>
            <el-sub-menu index="/process-group" :class="{ 'is-active-parent': isProcessActive }" popper-class="process-dropdown">
              <template #title>
                <span class="nav-icon">•</span> 工艺监控
              </template>
              <el-menu-item index="/inflow-monitor" @click="closeMenu">进水监测室</el-menu-item>
              <el-menu-item index="/B1" @click="closeMenu">粗格栅、调节池及提升泵房</el-menu-item>
              <el-menu-item index="/X1" @click="closeMenu">细格栅及旋流沉砂池</el-menu-item>
              <el-menu-item index="/H1" @click="closeMenu">AAO生物池</el-menu-item>
              <el-menu-item index="/M3" @click="closeMenu">高效沉淀池</el-menu-item>
              <el-menu-item index="/P2" @click="closeMenu">消毒池</el-menu-item>
              <el-menu-item index="/T3" @click="closeMenu">污泥浓缩池</el-menu-item>
              <el-menu-item index="/L1" @click="closeMenu">滤布滤池</el-menu-item>
              <el-menu-item index="/G1" @click="closeMenu">鼓风机房</el-menu-item>
              <el-menu-item index="/J1" @click="closeMenu">PAC及乙酸钠加药间</el-menu-item>
              <el-menu-item index="/J2" @click="closeMenu">PAM及次氯酸钠加药间</el-menu-item>
              <el-menu-item index="/S1" @click="closeMenu">生物除臭系统</el-menu-item>
              <el-menu-item index="/D1" @click="closeMenu">脱水车间</el-menu-item>
              <el-menu-item index="/P3" @click="closeMenu">巴氏计量槽</el-menu-item>
              <el-menu-item index="/process-flow" @click="closeMenu">工艺流程图</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/history" @click="closeMenu">
              <span class="nav-icon">•</span> 历史数据
            </el-menu-item>
            <el-menu-item index="/alarm" @click="closeMenu">
              <span class="nav-icon">•</span> 设备报警
            </el-menu-item>
            <el-menu-item index="/process-params" @click="closeMenu">
              <span class="nav-icon">•</span> 工艺参数设置
            </el-menu-item>
          </div>
        </el-menu>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import jwt_decode from 'jwt-decode'
import screenfull from 'screenfull'
import { ElMessage } from 'element-plus'
import { clearAuthSession } from '@/utils/authSession'

const menuOpen = ref(false)
const toggleMenu = () => { menuOpen.value = !menuOpen.value }
const closeMenu = () => { menuOpen.value = false }

const handleResize = () => {
  if (window.innerWidth > 768 && menuOpen.value) menuOpen.value = false
}

const isFullscreen = ref(false)
const toggleFullscreen = () => {
  if (screenfull.isEnabled) {
    screenfull.toggle()
    isFullscreen.value = screenfull.isFullscreen
  } else {
    ElMessage.warning('褰撳墠娴忚鍣ㄤ笉鏀寔鍏ㄥ睆')
  }
}

// Clock
const currentDate = ref('')
const currentTime = ref('')
const currentWeek = ref('')
const weekNames = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

const updateTime = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  currentDate.value = `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`
  currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  currentWeek.value = weekNames[now.getDay()]
}
let timeTimer = null

// Sub-nav
const route = useRoute()
const router = useRouter()

const showSubNav = computed(() => route.path !== '/login' && route.path !== '/register')

const updateNavHeight = () => {
  nextTick(() => {
    const mainHeight = 80
    const subHeight = showSubNav.value ? 52 : 0
    document.documentElement.style.setProperty('--nav-height', (mainHeight + subHeight) + 'px')
  })
}

watch(showSubNav, updateNavHeight)

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  updateNavHeight()
  if (screenfull.isEnabled) {
    screenfull.on('change', () => { isFullscreen.value = screenfull.isFullscreen })
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  window.removeEventListener('resize', handleResize)
  document.documentElement.style.removeProperty('--nav-height')
})

const getUsernameFromToken = () => {
  const token = localStorage.getItem('token')
  if (!token) return ''
  try { return jwt_decode(token).sub || '' } catch { return '' }
}

const getRoleFromToken = () => {
  const token = localStorage.getItem('token')
  if (!token) return 'user'
  try { return jwt_decode(token).role || 'user' } catch { return 'user' }
}

const processRoutes = ['/inflow-monitor', '/B1', '/X1', '/H1', '/M3', '/P2', '/T3', '/L1', '/G1', '/J1', '/J2', '/S1', '/D1', '/P3', '/process-flow', '/process']
const isProcessActive = computed(() => processRoutes.includes(route.path))
const activeIndex = computed(() => route.path)
const username = computed(() => getUsernameFromToken())
const role = computed(() => getRoleFromToken())

const showUserInfo = computed(() => route.path !== '/login' && route.path !== '/register')

const logout = () => {
  clearAuthSession()
  router.push('/login')
}
</script>

<style scoped>
/* ========== Wrapper ========== */
.nav-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  overflow: visible;
}

/* ========== Logo Area (spans both header bars) ========== */
.logo-area {
  position: absolute;
  top: 80px;
  left: 0;
  transform: translateY(-50%);
  height: 164px;
  display: flex;
  align-items: center;
  padding-left: 10px;
  z-index: 200;
  pointer-events: none;
}

/* ========== Header Glow Line ========== */
.header-glow {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
  transparent 0%,
  var(--color-primary-dim) 20%,
  var(--color-primary) 50%,
  var(--color-primary-dim) 80%,
  transparent 100%
  );
  box-shadow: 0 0 6px var(--color-primary-dim);
  opacity: 0.6;
}

/* ========== Main Header ========== */
.header-main {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 370px;
  background: linear-gradient(180deg,
  rgba(15, 23, 42, 0.96) 0%,
  rgba(15, 23, 42, 0.92) 100%
  );
  position: relative;
  z-index: 100;
  backdrop-filter: saturate(125%);
  box-shadow:
      inset 0 1px 0 rgba(140, 238, 255, 0.04),
      inset 0 -1px 0 rgba(0, 232, 255, 0.06);
  overflow: visible;
}

.header-main::before,
.header-main::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.header-main::before {
  inset: 0 16px auto;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 232, 255, 0.2), transparent);
  box-shadow: 0 0 8px rgba(0, 232, 255, 0.08);
}

.header-main::after {
  left: 50%;
  top: 0;
  width: min(700px, 50vw);
  height: 72px;
  transform: translateX(-50%);
  border-top: 1px solid rgba(0, 128, 255, 0.15);
  border-left: 1px solid rgba(0, 128, 255, 0.04);
  border-right: 1px solid rgba(0, 128, 255, 0.04);
  clip-path: polygon(0 18px, 12% 18px, 16% 0, 84% 0, 88% 18px, 100% 18px, 94% 100%, 6% 100%);
  box-shadow: 0 0 12px rgba(0, 232, 255, 0.06);
}

.header-energy,
.header-node {
  position: absolute;
  pointer-events: none;
  z-index: 1;
}

.header-energy {
  top: 6px;
  width: 220px;
  height: 42px;
  background:
      linear-gradient(90deg, transparent, rgba(0, 128, 255, 0.4), transparent),
      linear-gradient(180deg, rgba(0, 232, 255, 0.06), transparent);
  opacity: 0.3;
}

.energy-left {
  left: 18%;
  clip-path: polygon(0 100%, 18% 30%, 100% 0, 76% 100%);
  animation: none;
}

.energy-right {
  right: 18%;
  clip-path: polygon(24% 0, 100% 30%, 82% 100%, 0 100%);
  animation: none;
}

.header-node {
  top: 10px;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: 1px solid rgba(0, 128, 255, 0.08);
  background:
      radial-gradient(circle at center, rgba(0, 128, 255, 0.08), transparent 42%),
      conic-gradient(from 0deg, rgba(0, 128, 255, 0), rgba(0, 128, 255, 0.22), rgba(0, 128, 255, 0));
  box-shadow:
      0 0 10px rgba(0, 128, 255, 0.06),
      inset 0 0 10px rgba(0, 128, 255, 0.04);
  opacity: 0.25;
  animation: none;
}

.header-node::before,
.header-node::after {
  content: '';
  position: absolute;
  inset: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
}

.header-node::before {
  width: 5px;
  height: 5px;
  background: rgba(140, 238, 255, 0.6);
  box-shadow: 0 0 8px rgba(140, 238, 255, 0.4);
}

.header-node::after {
  width: 100%;
  height: 100%;
  border: 1px solid rgba(140, 238, 255, 0.08);
  animation: none;
}

.node-left {
  left: 23.5%;
}

.node-right {
  right: 23.5%;
  animation-delay: 1.8s;
}

/* Left */
.main-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.logo-img {
  height: 164px;
  width: auto;
  max-width: 246px;
  object-fit: contain;
  transform: scale(1.12);
  transform-origin: left center;
  filter: drop-shadow(0 0 10px rgba(0, 232, 255, 0.3));
  transition: filter 0.3s ease, transform 0.3s ease;
}

.logo-img:hover {
  transform: scale(1.14);
  filter: drop-shadow(0 0 12px rgba(0, 232, 255, 0.35));
}

/* Center Title */
.system-title {
  position: absolute;
  left: 50%;
  top: calc(50% + 2px);
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 16px;
  pointer-events: none;
  z-index: 10;
  padding: 10px 36px 12px;
  background:
      radial-gradient(circle at 50% 0%, rgba(140, 238, 255, 0.08), transparent 58%),
      linear-gradient(180deg, rgba(34, 116, 156, 0.10), rgba(12, 38, 58, 0.04));
  border: 1px solid rgba(0, 128, 255, 0.10);
  box-shadow:
      inset 0 1px 0 rgba(140, 238, 255, 0.06),
      inset 0 0 16px rgba(0, 232, 255, 0.03),
      0 0 16px rgba(0, 232, 255, 0.04);
  backdrop-filter: saturate(135%);
  clip-path: polygon(0 18px, 10% 18px, 13% 0, 87% 0, 90% 18px, 100% 18px, 96% 100%, 4% 100%);
  isolation: isolate;
}

.system-title::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(140, 238, 255, 0.2), transparent);
  transform: translateX(-120%);
  animation: none;
  pointer-events: none;
  z-index: -1;
}

.title-content {
  text-align: center;
}

.title-main {
  font-family: var(--font-body);
  font-size: 2.625rem;
  line-height: 1.06;
  font-weight: 700;
  color: transparent;
  background: linear-gradient(180deg, #F2F3F5 0%, #8ceeff 52%, #0080ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow:
      0 0 10px rgba(0, 232, 255, 0.12),
      0 0 20px rgba(0, 232, 255, 0.06);
  letter-spacing: 6px;
  white-space: nowrap;
}

.title-sub {
  font-family: var(--font-display);
  font-size: 0.8125rem;
  font-weight: 600;
  color: rgba(140, 238, 255, 0.6);
  letter-spacing: 2px;
  margin-top: 4px;
  text-shadow: 0 0 8px rgba(0, 128, 255, 0.08);
  white-space: nowrap;
}

.title-decoration {
  width: 100px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 128, 255, 0.5));
  position: relative;
  box-shadow: 0 0 8px rgba(0, 232, 255, 0.12);
}

.title-decoration.right {
  background: linear-gradient(90deg, rgba(0, 128, 255, 0.9), transparent);
}

.title-decoration::after {
  content: '';
  position: absolute;
  top: -2px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(140, 238, 255, 0.6);
  box-shadow: 0 0 6px rgba(140, 238, 255, 0.4);
}

.title-decoration.left::after {
  right: 0;
}

.title-decoration.right::after {
  left: 0;
}

/* Right */
.main-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* ========== Clock ========== */
.time-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.15;
  user-select: none;
}

.time-date {
  font-family: var(--font-data);
  font-size: 1.125rem;
  font-weight: 400;
  color: rgba(0, 232, 255, 0.6);
  letter-spacing: 1px;
}

.time-main {
  font-family: var(--font-display);
  font-size: 2.125rem;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 2px;
  line-height: 1.1;
  margin: 1px 0;
  text-shadow:
      0 0 6px rgba(0, 232, 255, 0.3),
      0 0 14px rgba(0, 232, 255, 0.1);
}

.time-week {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-warning);
  letter-spacing: 2px;
  text-shadow: 0 0 6px rgba(255, 171, 0, 0.3);
}

/* ========== Time + Weather Group ========== */
.time-weather-group {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* ========== User Info ========== */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  color: rgba(0, 232, 255, 0.7);
  font-size: 1rem;
  font-family: var(--font-body);
}

.role-tag {
  padding: 4px 9px;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-family: var(--font-display);
  font-weight: 600;
  letter-spacing: 1px;
}

.role-tag.admin {
  background: linear-gradient(135deg, rgba(255, 171, 0, 0.2), rgba(255, 171, 0, 0.1));
  border: 1px solid rgba(255, 171, 0, 0.4);
  color: var(--color-warning);
  text-shadow: 0 0 6px rgba(255, 171, 0, 0.4);
}

.role-tag.user {
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.2), rgba(0, 230, 118, 0.1));
  border: 1px solid rgba(0, 230, 118, 0.4);
  color: var(--color-success);
  text-shadow: 0 0 6px rgba(0, 230, 118, 0.4);
}

.icon-btn {
  padding: 6px 9px !important;
  font-size: 1rem !important;
  background: var(--color-primary-ghost) !important;
  border: var(--border-subtle) !important;
  color: rgba(0, 232, 255, 0.7) !important;
  transition: all 0.3s ease !important;
  border-radius: var(--radius-sm) !important;
}

.icon-btn:hover {
  background: rgba(0, 232, 255, 0.15) !important;
  box-shadow: var(--glow-primary) !important;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: linear-gradient(135deg, rgba(255, 23, 68, 0.15), rgba(255, 23, 68, 0.08));
  border: 1px solid rgba(255, 23, 68, 0.35);
  color: var(--color-danger);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: rgba(255, 23, 68, 0.25);
  box-shadow: var(--glow-danger);
  transform: translateY(-1px);
}

.logout-icon {
  font-size: 0.8125rem;
}

/* ========== Sub Navigation ========== */
.header-sub {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 99;
  background: transparent;
  padding: 0;
  overflow: visible;
}

.notch-bg {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 22px;
  background:
      linear-gradient(rgba(15, 23, 42, 0.92), rgba(15, 23, 42, 0.88)) padding-box,
      linear-gradient(90deg, var(--color-primary), var(--color-secondary), var(--color-primary), var(--color-secondary), var(--color-primary)) border-box;
  background-size: 100% 100%, 200% 100%;
  border: 1.5px solid transparent;
  border-radius: 0;
  box-shadow:
      0 4px 24px rgba(0, 232, 255, 0.06),
      inset 0 0 20px rgba(0, 232, 255, 0.02);
  position: relative;
  backdrop-filter: saturate(125%);
  animation: none;
  clip-path: polygon(0 0, 4% 0, 5.2% 10px, 94.8% 10px, 96% 0, 100% 0, 100% 100%, 96% 100%, 94.8% calc(100% - 10px), 5.2% calc(100% - 10px), 4% 100%, 0 100%);
  overflow: hidden;
  isolation: isolate;
}

@keyframes notch-border-flow {
  0% { background-position: 0 0, 0% 0; }
  100% { background-position: 0 0, 200% 0; }
}

/* Energy line under notch */
.notch-bg::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-primary) 30%, var(--color-secondary) 70%, transparent);
  box-shadow: 0 0 6px var(--color-primary-dim);
  animation: none;
  border-radius: 1px;
  opacity: 0.5;
}

.notch-bg::before {
  content: '';
  position: absolute;
  inset: 1px;
  clip-path: inherit;
  border: 1px solid rgba(0, 128, 255, 0.04);
  pointer-events: none;
}

.notch-core-beam,
.notch-pulse {
  position: absolute;
  pointer-events: none;
  z-index: 0;
}

.notch-core-beam {
  left: 18%;
  right: 18%;
  top: 50%;
  height: 18px;
  transform: translateY(-50%);
  background:
      linear-gradient(90deg, transparent, rgba(0, 128, 255, 0.24), transparent),
      linear-gradient(90deg, rgba(0, 128, 255, 0), rgba(0, 128, 255, 0.58), rgba(0, 128, 255, 0));
  box-shadow:
      0 0 18px rgba(0, 128, 255, 0.12),
      inset 0 0 14px rgba(0, 128, 255, 0.08);
  opacity: 0.36;
}

.notch-core-beam::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(140, 238, 255, 0.95), transparent);
  transform: translateX(-100%);
  animation: beamSweep 4.2s linear infinite;
}

.notch-pulse {
  top: 50%;
  width: 74px;
  height: 20px;
  transform: translateY(-50%);
  background: linear-gradient(90deg, transparent, rgba(0, 128, 255, 0.3), transparent);
  opacity: 0.18;
}

.pulse-left {
  left: 8%;
  animation: none;
}

.pulse-right {
  right: 8%;
  animation: none;
}

@keyframes energy-flow {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* Nav menu */
.nav-menu {
  width: auto;
  height: auto;
  border-bottom: none !important;
  background: transparent !important;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.menu-items {
  display: flex;
  gap: 6px;
  align-items: center;
  position: relative;
  z-index: 2;
}

/* ========== Navigation Items ========== */
:deep(.el-menu--horizontal .el-menu-item) {
  height: 48px !important;
  line-height: 46px !important;
  border-bottom: none !important;
  background:
      linear-gradient(90deg, rgba(0, 232, 255, 0.08), transparent 38%),
      rgba(0, 232, 255, 0.10) !important;
  border: 1px solid rgba(0, 232, 255, 0.15) !important;
  border-radius: 0 !important;
  padding: 0 24px !important;
  margin: 0 !important;
  font-size: 1.375rem !important;
  font-family: var(--font-body);
  color: rgba(0, 232, 255, 0.65) !important;
  letter-spacing: 1px;
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1) !important;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
  box-shadow:
      inset 0 1px 0 rgba(0, 128, 255, 0.04),
      inset 0 0 14px rgba(0, 232, 255, 0.02),
      0 0 0 1px rgba(0, 232, 255, 0.03);
}

.nav-icon {
  font-size: 0.875rem;
  margin-right: 4px;
  opacity: 0.5;
  transition: all 0.3s ease;
}

:deep(.el-menu--horizontal .el-menu-item:hover) {
  background:
      linear-gradient(90deg, rgba(0, 232, 255, 0.08), transparent 42%),
      rgba(0, 232, 255, 0.06) !important;
  border-color: rgba(0, 232, 255, 0.25) !important;
  color: #ffffff !important;
  box-shadow:
      0 0 10px rgba(0, 232, 255, 0.08),
      inset 0 0 10px rgba(0, 232, 255, 0.03);
  transform: none;
}

:deep(.el-menu--horizontal .el-menu-item:hover .nav-icon) {
  opacity: 1;
  color: var(--color-primary);
}

:deep(.el-menu--horizontal .el-menu-item.is-active) {
  background:
      linear-gradient(90deg, rgba(0, 232, 255, 0.12), transparent 42%),
      linear-gradient(135deg, rgba(0, 232, 255, 0.10), rgba(0, 232, 255, 0.06)) !important;
  border-color: rgba(0, 232, 255, 0.5) !important;
  color: #ffffff !important;
  font-weight: 600;
  box-shadow:
      0 0 12px rgba(0, 232, 255, 0.10),
      inset 0 0 10px rgba(0, 232, 255, 0.03),
      0 0 18px rgba(0, 232, 255, 0.06) !important;
  text-shadow: 0 0 6px rgba(0, 232, 255, 0.3);
}

:deep(.el-menu--horizontal .el-menu-item.is-active .nav-icon) {
  opacity: 1;
  color: var(--color-primary);
  text-shadow: 0 0 6px var(--color-primary);
}

/* ========== Sub-menu (dropdown) ========== */
:deep(.el-sub-menu__title) {
  height: 48px !important;
  line-height: 46px !important;
  border-bottom: none !important;
  background:
      linear-gradient(90deg, rgba(0, 232, 255, 0.08), transparent 38%),
      rgba(0, 232, 255, 0.10) !important;
  border: 1px solid rgba(0, 232, 255, 0.15) !important;
  border-radius: 0 !important;
  padding: 0 40px 0 24px !important;
  margin: 0 !important;
  font-size: 1.375rem !important;
  font-family: var(--font-body);
  color: rgba(0, 232, 255, 0.65) !important;
  letter-spacing: 1px;
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1) !important;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
  box-shadow:
      inset 0 1px 0 rgba(0, 128, 255, 0.04),
      inset 0 0 14px rgba(0, 232, 255, 0.02),
      0 0 0 1px rgba(0, 232, 255, 0.03);
}

:deep(.el-sub-menu__title:hover) {
  background:
      linear-gradient(90deg, rgba(0, 232, 255, 0.08), transparent 42%),
      rgba(0, 232, 255, 0.06) !important;
  border-color: rgba(0, 232, 255, 0.25) !important;
  color: #ffffff !important;
  box-shadow:
      0 0 10px rgba(0, 232, 255, 0.08),
      inset 0 0 10px rgba(0, 232, 255, 0.03);
  transform: none;
}

:deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: rgba(0, 232, 255, 0.65) !important;
  font-size: 0.875rem;
  right: 10px !important;
}

/* Active parent state */
:deep(.is-active-parent > .el-sub-menu__title) {
  background:
      linear-gradient(90deg, rgba(0, 232, 255, 0.12), transparent 42%),
      linear-gradient(135deg, rgba(0, 232, 255, 0.10), rgba(0, 232, 255, 0.06)) !important;
  border-color: rgba(0, 232, 255, 0.5) !important;
  color: #ffffff !important;
  font-weight: 600;
  box-shadow:
      0 0 12px rgba(0, 232, 255, 0.10),
      inset 0 0 10px rgba(0, 232, 255, 0.03),
      0 0 18px rgba(0, 232, 255, 0.06) !important;
  text-shadow: 0 0 6px rgba(0, 232, 255, 0.3);
}

:deep(.is-active-parent > .el-sub-menu__title .nav-icon) {
  opacity: 1;
  color: var(--color-primary);
  text-shadow: 0 0 6px var(--color-primary);
}

/* Dropdown popup styles moved to unscoped block below */

/* Shine sweep on hover — disabled */

/* ========== Hamburger ========== */
.hamburger {
  display: none;
  flex-direction: column;
  justify-content: space-between;
  width: 24px;
  height: 18px;
  cursor: pointer;
  z-index: 101;
}

.hamburger span {
  display: block;
  height: 2px;
  width: 100%;
  background: linear-gradient(90deg, rgba(0, 232, 255, 0.6), var(--color-primary));
  border-radius: 1px;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px var(--color-primary-dim);
}

.hamburger.active span:nth-child(1) { transform: translateY(8px) rotate(45deg); }
.hamburger.active span:nth-child(2) { opacity: 0; }
.hamburger.active span:nth-child(3) { transform: translateY(-8px) rotate(-45deg); }

/* ========== Menu Mask ========== */
.menu-mask {
  position: fixed;
  top: var(--nav-height, 132px);
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 5, 15, 0.7);
  backdrop-filter: none;
  z-index: 98;
}

@keyframes headerSweepLeft {
  0% { transform: translateX(-18px); opacity: 0.2; }
  50% { transform: translateX(16px); opacity: 0.78; }
  100% { transform: translateX(-18px); opacity: 0.2; }
}

@keyframes headerSweepRight {
  0% { transform: translateX(18px); opacity: 0.2; }
  50% { transform: translateX(-16px); opacity: 0.78; }
  100% { transform: translateX(18px); opacity: 0.2; }
}

@keyframes navNodePulse {
  0%, 100% { opacity: 0.28; transform: scale(0.95); }
  50% { opacity: 0.62; transform: scale(1.03); }
}

@keyframes navPing {
  0% { transform: translate(-50%, -50%) scale(0.72); opacity: 0.52; }
  80% { opacity: 0.12; }
  100% { transform: translate(-50%, -50%) scale(1.45); opacity: 0; }
}

@keyframes titleSweep {
  0% { transform: translateX(-120%); opacity: 0; }
  12% { opacity: 1; }
  100% { transform: translateX(120%); opacity: 0; }
}

@keyframes beamSweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes pulseTravel {
  0%, 100% { transform: translateY(-50%) scaleX(0.9); opacity: 0.28; }
  50% { transform: translateY(-50%) scaleX(1.08); opacity: 0.82; }
}

/* ========== Nav Menu Container ========== */
.nav-menu {
  width: auto;
  height: auto;
  border-bottom: none !important;
  background: transparent !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-items {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* ========== Mobile ========== */
@media (max-width: 768px) {
  .header-main {
    height: 60px;
    padding: 0 12px 0 100px;
  }

  .hamburger { display: flex; }

  .logo-area {
    height: 60px;
    left: 36px;
    padding-left: 4px;
  }

  .logo-img { height: 31px; max-width: 84px; }

  .title-main {
    font-size: 1.125rem;
    letter-spacing: 2px;
  }

  .title-sub { display: none; }
  .title-decoration { display: none; }

  .time-weather-group { gap: 10px; }
  .time-box { align-items: center; margin-right: 4px; }
  .time-date { font-size: 0.8125rem; }
  .time-main { font-size: 1.375rem; letter-spacing: 1px; }
  .time-week { font-size: 0.8125rem; }

  .username, .role-tag, .logout-text { display: none; }

  .logout-btn { padding: 4px 8px; }
  .icon-btn { padding: 4px 6px !important; font-size: 0.8125rem !important; }

  .header-sub {
    height: auto;
    background: rgba(15, 23, 42, 0.95);
    border-bottom: 1px solid rgba(0, 232, 255, 0.1);
  }

  .notch-bg {
    width: 100%;
    border-radius: 0;
    border: none;
    box-shadow: none;
    background: transparent;
    padding: 3px 10px;
    animation: none;
  }

  .notch-bg::after { display: none; }

  .header-energy,
  .header-node,
  .notch-core-beam,
  .notch-pulse {
    display: none;
  }

  .nav-menu { width: 100%; }

  .menu-items {
    gap: 6px;
    flex-wrap: wrap;
    justify-content: center;
  }

  :deep(.el-menu--horizontal .el-menu-item) {
    height: 28px !important;
    line-height: 26px !important;
    padding: 0 10px !important;
    font-size: 0.8125rem !important;
    border-radius: 14px !important;
  }

  .menu-items.menu-open,
  .nav-menu.menu-open .menu-items {
    position: fixed;
    top: 60px;
    left: 0;
    width: 72%;
    height: calc(100vh - 52px);
    background: rgba(15, 23, 42, 0.97);
    backdrop-filter: none;
    flex-direction: column;
    justify-content: flex-start;
    padding: 16px;
    transition: left 0.3s ease;
    z-index: 100;
    box-shadow: 2px 0 24px rgba(0, 232, 255, 0.1);
    overflow-y: auto;
    border-right: 1px solid rgba(0, 232, 255, 0.15);
    gap: 10px;
  }

  :deep(.menu-items.menu-open .el-menu-item) {
    height: 42px !important;
    line-height: 40px !important;
    border-radius: var(--radius-pill) !important;
    padding: 0 18px !important;
    font-size: 1rem !important;
  }

  :deep(.menu-items.menu-open .el-sub-menu__title) {
    height: 42px !important;
    line-height: 40px !important;
    border-radius: var(--radius-pill) !important;
    padding: 0 18px !important;
    font-size: 1rem !important;
  }

  :deep(.menu-items.menu-open .el-sub-menu .el-menu--popup) {
    position: static !important;
    background: rgba(0, 232, 255, 0.03) !important;
    border: none !important;
    box-shadow: none !important;
    padding: 4px 0 4px 16px !important;
    visibility: visible !important;
    display: block !important;
  }

  :deep(.menu-items.menu-open .el-sub-menu .el-menu--popup .el-menu-item) {
    height: 36px !important;
    line-height: 34px !important;
    font-size: 0.9375rem !important;
    padding: 0 14px !important;
    border-radius: var(--radius-pill) !important;
  }

  .menu-mask { top: 60px; }
}

/* PC */
@media (min-width: 769px) {
  .hamburger { display: none; }

  .menu-items {
    display: flex !important;
    position: static;
    width: auto;
    height: auto;
    background: transparent;
    flex-direction: row;
    padding: 0;
    box-shadow: none;
    border: none;
  }
}
</style>

<!-- Unscoped: popup is teleported to <body>, scoped styles can't reach it -->
<style>
.process-dropdown {
  background: linear-gradient(180deg, #0a1a3a 0%, #0d2244 100%) !important;
  border: 1px solid rgba(0, 128, 255, 0.3) !important;
  border-radius: 0 !important;
  box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.5),
      0 0 20px rgba(0, 128, 255, 0.12),
      inset 0 0 20px rgba(0, 128, 255, 0.04) !important;
}

.process-dropdown .el-menu {
  background: transparent !important;
  padding: 4px 0 !important;
}

.process-dropdown .el-menu--popup {
  background: transparent !important;
}

.process-dropdown .el-menu-item {
  height: 40px !important;
  line-height: 38px !important;
  padding: 0 28px !important;
  font-size: 1.125rem !important;
  font-family: var(--font-body);
  color: #8ceeff !important;
  background: transparent !important;
  border: none !important;
  border-radius: 0 !important;
  letter-spacing: 1px;
  transition: all 0.3s ease !important;
  white-space: nowrap;
}

.process-dropdown .el-menu-item:hover {
  background: rgba(0, 128, 255, 0.2) !important;
  color: #ffffff !important;
  padding-left: 34px !important;
}

.process-dropdown .el-menu-item.is-active {
  background: rgba(0, 128, 255, 0.25) !important;
  color: #ffffff !important;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(0, 232, 255, 0.5);
}
</style>
