import { createRouter, createWebHistory } from 'vue-router'
import DeviceList from '../views/DeviceList.vue'
import DeviceDetail from '../views/DeviceDetail.vue'
import RawPackets from '../views/RawPackets.vue'

const routes = [
  { path: '/', redirect: '/devices' },
  { path: '/devices', component: DeviceList },
  { path: '/devices/:id', component: DeviceDetail },
  { path: '/packets', component: RawPackets }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
