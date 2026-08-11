import { createRouter, createWebHistory } from 'vue-router'
import { hasAdminCredentials } from '../api/axios.js'

const DeviceList = () => import('../views/DeviceList.vue')
const DeviceDetail = () => import('../views/DeviceDetail.vue')
const RawPackets = () => import('../views/RawPackets.vue')
const Login = () => import('../views/Login.vue')

const routes = [
  { path: '/', redirect: '/devices' },
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/devices', component: DeviceList },
  { path: '/devices/:id', component: DeviceDetail },
  { path: '/packets', component: RawPackets }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  if (!to.meta.public && !hasAdminCredentials()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && hasAdminCredentials()) {
    return '/devices'
  }
  return true
})

export default router
