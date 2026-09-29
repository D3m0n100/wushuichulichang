<template>
  <div class="login-container">
    <div class="ambient-layer"></div>
    <div class="login-card">
      <div class="card-header">
        <div class="logo-ring">
          <img src="/连际VI基础标识(1)_01(1).png" alt="logo" class="brand-logo" />
        </div>
        <h2 class="sys-name">荆州纪南污水厂智能化运营管理系统可视化平台</h2>
        <p class="sys-sub">数字孪生控制系统</p>
      </div>

      <form class="card-body" @submit.prevent="login">
        <div class="input-wrap">
          <span class="input-icon">P</span>
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            autocomplete="current-password"
            placeholder="密码"
            class="field-input"
          />
          <button type="button" class="eye-toggle" @click="showPassword = !showPassword">
            {{ showPassword ? '隐藏' : '显示' }}
          </button>
        </div>

        <button
          type="submit"
          class="login-btn"
          :disabled="loading"
        >
          <span v-if="!loading">进入系统</span>
          <span v-else class="spinner"></span>
        </button>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import {
  getLastAuthLogout,
  getTokenState,
  storeAuthSession,
  syncAuthClockFromToken
} from '@/utils/authSession'

const password = ref('')
const logoutMessages = {
  'password-rotated': '密码使用阶段已切换，请使用当前阶段密码重新登录',
  'server-session-invalid-10': '后台连续10次确认登录状态失效，请重新登录',
  'outdated-auth-version': '前后端认证版本不一致，请强制刷新页面',
  'invalid-token-issuer': '登录令牌来源无效，请重新登录',
  'legacy-token': '旧版登录令牌已失效，请重新登录',
  'expired-token': '登录令牌已过期，请重新登录',
  'invalid-password-stage-time': '密码阶段时间无效，请重新登录',
  'missing-password-stage-time': '密码阶段信息缺失，请重新登录',
  'invalid-token': '登录令牌无效，请重新登录'
}
const lastLogout = getLastAuthLogout()
const errorMsg = ref(logoutMessages[lastLogout?.reason] || '')
const showPassword = ref(false)
const loading = ref(false)
const router = useRouter()

const login = async () => {
  errorMsg.value = ''

  if (!password.value) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    const response = await request.post('/auth/login', {
      username: 'admin',
      password: password.value
    })
    const data = response.data
    if (!data?.success || !data?.token) {
      errorMsg.value = data?.message === 'invalid password' ? '密码错误' : '登录失败'
      return
    }

    syncAuthClockFromToken(data.token)
    const tokenState = getTokenState(data.token)
    if (!tokenState.valid) {
      errorMsg.value = tokenState.reason === 'outdated-auth-version'
        ? '前后端认证版本不一致，请强制刷新页面'
        : '登录状态无效，请强制刷新页面'
      return
    }
    if (!storeAuthSession(data.token, data.username || 'admin')) {
      errorMsg.value = '登录状态无效，请强制刷新页面'
      return
    }
    await router.replace('/')
  } catch (error) {
    errorMsg.value = error.response ? '登录失败' : '无法连接后台服务'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(0, 182, 255, 0.18), transparent 32%),
    linear-gradient(180deg, #03111f 0%, #041a2d 48%, #02101e 100%);
}

.ambient-layer {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(33, 186, 255, 0.16), transparent 22%),
    radial-gradient(circle at 80% 78%, rgba(0, 120, 255, 0.14), transparent 24%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(760px, calc(100vw - 48px));
  border: 1px solid rgba(42, 203, 255, 0.35);
  border-radius: 24px;
  padding: 28px 32px 26px;
  background: rgba(4, 22, 38, 0.72);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.32), inset 0 0 32px rgba(0, 168, 255, 0.08);
  backdrop-filter: none;
}

.card-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-ring {
  width: 78px;
  height: 78px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: radial-gradient(circle, rgba(0, 232, 255, 0.18), rgba(0, 0, 0, 0));
  border: 1px solid rgba(0, 232, 255, 0.3);
}

.brand-logo {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.sys-name {
  margin: 0;
  font-size: 1.625rem;
  line-height: 1.2;
  color: #f2fbff;
  white-space: nowrap;
}

.sys-sub {
  margin: 8px 0 0;
  font-size: 0.875rem;
  letter-spacing: 0.32em;
  color: rgba(93, 214, 255, 0.82);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  height: 56px;
  border-radius: 14px;
  border: 1px solid rgba(57, 198, 255, 0.2);
  background: rgba(6, 26, 46, 0.68);
}

.input-icon {
  color: #70deff;
  font-size: 0.9375rem;
  font-weight: 700;
}

.field-input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: #f3fbff;
  font-size: 1.125rem;
}

.field-input::placeholder {
  color: rgba(183, 224, 255, 0.45);
}

.eye-toggle {
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  color: #7de4ff;
  cursor: pointer;
  font-size: 0.9375rem;
}

.login-btn,
.text-btn {
  min-height: 52px;
  border-radius: 14px;
  border: 1px solid rgba(47, 206, 255, 0.32);
  cursor: pointer;
  font-size: 1.125rem;
  font-weight: 700;
}

.login-btn {
  color: #F2F3F5;
  background: linear-gradient(90deg, rgba(0, 122, 255, 0.9), rgba(0, 197, 255, 0.9));
}

.text-btn {
  color: #aeeeff;
  background: rgba(4, 34, 56, 0.72);
  font-size: 1rem;
}

.login-btn:disabled,
.text-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.error-msg,
.success-msg {
  margin: -4px 0 0;
  text-align: center;
  font-size: 1rem;
}

.error-msg {
  color: #ff6b7e;
}

.success-msg {
  color: #45f2a6;
}

.spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.38);
  border-top-color: #fff;
  display: inline-block;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
