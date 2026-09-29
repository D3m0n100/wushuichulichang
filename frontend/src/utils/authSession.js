import jwt_decode from 'jwt-decode'

const loginPath = '/#/login'
const maxTimerDelay = 2147483647
const expectedAuthVersion = 3
const expectedIssuer = 'jinan-water-backend'
const sessionCheckInterval = 60000
const sessionCheckTimeout = 10000
const requiredInvalidSessionChecks = 10
const apiBaseURL = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const authClockKey = 'authServerClock'
const authLogoutKey = 'authLastLogout'

let rotationTimer = null
let scheduledToken = null
let sessionCheckTimer = null
let sessionCheckToken = null
let sessionCheckInFlight = false
let consecutiveInvalidSessionChecks = 0

const clearRotationTimer = () => {
  if (rotationTimer) clearTimeout(rotationTimer)
  rotationTimer = null
  scheduledToken = null
}

const clearSessionCheckTimer = () => {
  if (sessionCheckTimer) clearTimeout(sessionCheckTimer)
  sessionCheckTimer = null
  sessionCheckToken = null
  sessionCheckInFlight = false
  consecutiveInvalidSessionChecks = 0
}

export const clearAuthSession = () => {
  clearRotationTimer()
  clearSessionCheckTimer()
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem(authClockKey)
}

export const redirectToLogin = () => {
  if (window.location.hash !== '#/login') window.location.replace(loginPath)
}

const recordAuthLogout = reason => {
  localStorage.setItem(authLogoutKey, JSON.stringify({
    reason: reason || 'unknown',
    time: Date.now()
  }))
}

export const getLastAuthLogout = () => {
  try {
    const value = JSON.parse(localStorage.getItem(authLogoutKey) || 'null')
    return value && typeof value.reason === 'string' ? value : null
  } catch {
    return null
  }
}

export const clearInvalidAuthSession = reason => {
  recordAuthLogout(reason)
  clearAuthSession()
}

export const expireAuthSession = reason => {
  clearInvalidAuthSession(reason)
  redirectToLogin()
}

const saveAuthClock = (token, serverTime) => {
  const numericServerTime = Number(serverTime)
  if (!token || !Number.isFinite(numericServerTime) || numericServerTime <= 0) return false
  localStorage.setItem(authClockKey, JSON.stringify({
    token,
    serverTime: numericServerTime,
    clientTime: Date.now()
  }))
  return true
}

export const syncAuthClockFromToken = token => {
  try {
    const issuedAt = Number(jwt_decode(token)?.iat) * 1000
    return saveAuthClock(token, issuedAt)
  } catch {
    return false
  }
}

const getAuthNow = token => {
  try {
    const clock = JSON.parse(localStorage.getItem(authClockKey) || 'null')
    if (clock?.token !== token) return Date.now()
    const serverTime = Number(clock.serverTime)
    const clientTime = Number(clock.clientTime)
    if (!Number.isFinite(serverTime) || !Number.isFinite(clientTime)) return Date.now()
    return serverTime + Math.max(0, Date.now() - clientTime)
  } catch {
    return Date.now()
  }
}

export const getTokenState = (token, now) => {
  if (!token) return { valid: false, reason: 'missing-token' }
  try {
    const decoded = jwt_decode(token)
    const currentTime = Number.isFinite(now) ? now : getAuthNow(token)
    const authVersion = Number(decoded.authVersion)
    if (authVersion !== expectedAuthVersion) {
      return { valid: false, reason: 'outdated-auth-version' }
    }
    if (decoded.iss !== expectedIssuer) {
      return { valid: false, reason: 'invalid-token-issuer' }
    }
    const passwordStage = Number(decoded.passwordStage)
    if (!Number.isInteger(passwordStage) || passwordStage < 0 || passwordStage > 4) {
      return { valid: false, reason: 'legacy-token' }
    }

    if (decoded.exp != null) {
      const expiration = Number(decoded.exp) * 1000
      if (!Number.isFinite(expiration) || expiration <= 0 || currentTime >= expiration) {
        return { valid: false, reason: 'expired-token' }
      }
    }

    const passwordValidUntil = decoded.passwordValidUntil == null
      ? 0
      : Number(decoded.passwordValidUntil)
    if (!Number.isFinite(passwordValidUntil) || passwordValidUntil < 0) {
      return { valid: false, reason: 'invalid-password-stage-time' }
    }
    if (passwordStage < 4 && passwordValidUntil <= 0) {
      return { valid: false, reason: 'missing-password-stage-time' }
    }
    if (passwordValidUntil > 0 && currentTime >= passwordValidUntil) {
      return { valid: false, reason: 'password-rotated' }
    }

    return { valid: true, decoded, passwordStage, passwordValidUntil }
  } catch {
    return { valid: false, reason: 'invalid-token' }
  }
}

const readServerSessionState = async (token) => {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), sessionCheckTimeout)
  try {
    const response = await fetch(`${apiBaseURL}/auth/session`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
      signal: controller.signal
    })
    if (response.status === 401 || response.status === 403) {
      return 'invalid'
    }
    if (response.ok) {
      const data = await response.json()
      saveAuthClock(token, data?.serverTime)
      const state = getTokenState(token)
      if (!data?.success
        || Number(data.authVersion) !== expectedAuthVersion
        || Number(data.passwordStage) !== state.passwordStage) {
        return 'invalid'
      }
      return 'valid'
    }
    return 'unavailable'
  } catch {
    // A transient network outage must not destroy a valid local session.
    return 'unavailable'
  } finally {
    clearTimeout(timeout)
  }
}

const checkServerSession = async (token) => {
  sessionCheckTimer = null
  if (sessionCheckToken !== token || localStorage.getItem('token') !== token) return

  sessionCheckInFlight = true
  const state = await readServerSessionState(token)
  if (sessionCheckToken !== token || localStorage.getItem('token') !== token) return

  if (state === 'invalid') {
    consecutiveInvalidSessionChecks += 1
  } else {
    consecutiveInvalidSessionChecks = 0
  }
  if (sessionCheckToken === token) sessionCheckInFlight = false

  if (consecutiveInvalidSessionChecks >= requiredInvalidSessionChecks) {
    expireAuthSession('server-session-invalid-10')
    return
  }

  if (sessionCheckToken === token && localStorage.getItem('token') === token) {
    sessionCheckTimer = setTimeout(() => checkServerSession(token), sessionCheckInterval)
  }
}

const scheduleServerSessionCheck = (token) => {
  if (sessionCheckToken === token && (sessionCheckTimer || sessionCheckInFlight)) return
  clearSessionCheckTimer()
  sessionCheckToken = token
  sessionCheckTimer = setTimeout(() => checkServerSession(token), sessionCheckInterval)
}

export const isTokenUsable = (token) => getTokenState(token).valid

const scheduleAt = (token, validUntil) => {
  if (scheduledToken !== token) return
  const remaining = validUntil - getAuthNow(token)
  if (remaining <= 0) {
    expireAuthSession('password-rotated')
    return
  }
  rotationTimer = setTimeout(
    () => scheduleAt(token, validUntil),
    Math.min(remaining, maxTimerDelay)
  )
}

export const scheduleTokenLogout = (token) => {
  clearRotationTimer()
  const state = getTokenState(token)
  if (!state.valid) {
    expireAuthSession(state.reason)
    return false
  }

  scheduledToken = token
  if (state.passwordValidUntil > 0) scheduleAt(token, state.passwordValidUntil)
  scheduleServerSessionCheck(token)
  return true
}

export const storeAuthSession = (token, username) => {
  localStorage.removeItem('localAccessPassword')
  localStorage.removeItem(authLogoutKey)
  localStorage.setItem('token', token)
  localStorage.setItem('username', username || 'admin')
  syncAuthClockFromToken(token)
  return scheduleTokenLogout(token)
}
