import { createRouter, createWebHashHistory } from 'vue-router'
import {
  clearInvalidAuthSession,
  getTokenState,
  scheduleTokenLogout
} from '@/utils/authSession'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    redirect: '/login',
    meta: { requiresAuth: false }
  },
  {
    path: '/',           // ⭐ 确保有这个路由
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/process',
    name: 'Process',
    component: () => import('@/views/Process.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/History.vue'),
    meta: { requiresAuth: true }
  },
 
  {
   path: '/B1',
   name: 'B1',
   component: () => import('@/views/B1.vue'),
   meta: { requiresAuth: true }
  },
   {
   path: '/H1',
   name: 'H1',
   component: () => import('@/views/H1.vue'),
   meta: { requiresAuth: true }
  }
  ,
   {
   path: '/M3',
   name: 'M3',
   component: () => import('@/views/M3.vue'),
   meta: { requiresAuth: true }
  }
  ,
   {
   path: '/alarm',
   name: 'alarm',
   component: () => import('@/views/AlarmPage.vue'),
   meta: { requiresAuth: true }
  }
  ,
   {
   path: '/P2',
   name: 'P2',
   component: () => import('@/views/P2.vue'),
   meta: { requiresAuth: true }
  }
  ,
   {
   path: '/B2',
   name: 'B2',
   component: () => import('@/views/B2.vue'),
   meta: { requiresAuth: true }
  }
  ,
   {
   path: '/T3',
   name: 'T3',
   component: () => import('@/views/T3.vue'),
   meta: { requiresAuth: true }
  }
  ,
   {
   path: '/PH1',
   name: 'PH1',
   component: () => import('@/views/PH1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/phase1',
   name: 'Phase1',
   component: () => import('@/views/Phase1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/X1',
   name: 'X1',
   component: () => import('@/views/X1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/L1',
   name: 'L1',
   component: () => import('@/views/L1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/G1',
   name: 'G1',
   component: () => import('@/views/G1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/J1',
   name: 'J1',
   component: () => import('@/views/J1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/J2',
   name: 'J2',
   component: () => import('@/views/J2.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/S1',
   name: 'S1',
   component: () => import('@/views/S1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/D1',
   name: 'D1',
   component: () => import('@/views/D1.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/P3',
   name: 'P3',
   component: () => import('@/views/P3.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/process-flow',
   name: 'ProcessFlow',
   component: () => import('@/views/ProcessFlow.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/process-params',
   name: 'ProcessParams',
   component: () => import('@/views/ProcessParams.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/process-control',
   name: 'ProcessControl',
   component: () => import('@/views/ProcessControl.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/plc-diagnostics',
   name: 'PlcDiagnostics',
   component: () => import('@/views/PlcDiagnostics.vue'),
   meta: { requiresAuth: true }
  },
  {
   path: '/inflow-monitor',
   name: 'InflowMonitor',
   component: () => import('@/views/InflowMonitor.vue'),
   meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  let token = localStorage.getItem('token')
  if (token) {
    const tokenState = getTokenState(token)
    if (!tokenState.valid) {
      clearInvalidAuthSession(tokenState.reason)
      token = null
    } else {
      scheduleTokenLogout(token)
    }
  }
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

export default router
