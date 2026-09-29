import axios from 'axios'
import {
  expireAuthSession,
  getTokenState,
  scheduleTokenLogout
} from '@/utils/authSession'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  // 默认走同源 /api；开发环境由 vite 代理到后端主机。
  baseURL: apiBaseURL,
  timeout: 10000
})

const isLoginRequest = config => String(config?.url || '').includes('/auth/login')

const isStalePlcOverview = response => {
  const method = String(response?.config?.method || 'get').toLowerCase()
  const url = String(response?.config?.url || '').split('?')[0]
  if (method !== 'get' || !/\/plc\/[^/]+\/overview\/?$/.test(url)) return false

  const plc = response?.data?.data?.plc
  return plc?.stale === true
    || plc?.stale === 1
    || String(plc?.stale).toLowerCase() === 'true'
    || String(plc?.state).toUpperCase() === 'STALE'
}

// 请求拦截器：自动添加 token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    
    if (token && !isLoginRequest(config)) {
      const tokenState = getTokenState(token)
      if (!tokenState.valid) {
        expireAuthSession(tokenState.reason)
        return Promise.reject(new Error('登录密码已更新，请重新登录'))
      }
      scheduleTokenLogout(token)
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器：处理 401 未授权
request.interceptors.response.use(
  response => {
    // PLC 断线后后端可能返回 success=true 的旧缓存；页面不能把它当作实时状态。
    if (isStalePlcOverview(response)) {
      const error = new Error('PLC实时数据已过期')
      error.code = 'ERR_PLC_STALE'
      error.config = response.config
      error.response = response
      return Promise.reject(error)
    }
    return response
  },
  error => Promise.reject(error)
)

const existingToken = localStorage.getItem('token')
if (existingToken) scheduleTokenLogout(existingToken)

export default request
