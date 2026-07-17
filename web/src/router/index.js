import { createRouter, createWebHistory } from 'vue-router'
import DeviceList from '../views/DeviceList.vue'
import DeviceDetail from '../views/DeviceDetail.vue'
import RawPackets from '../views/RawPackets.vue'
import Login from '../views/Login.vue'
import { hasAdminCredentials } from '../api/axios.js'

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
