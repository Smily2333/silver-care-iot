import http from './axios.js'

export function listDevices(page = 0, size = 20) {
  return http.get('/admin/devices', { params: { page, size, sort: 'id,desc' } })
}

export function getDevice(id) {
  return http.get(`/admin/devices/${id}`)
}

export function getHealthRecords(id) {
  return http.get(`/admin/devices/${id}/health-records`)
}

export function getLocationRecords(id) {
  return http.get(`/admin/devices/${id}/location-records`)
}

export function sendCommand(id, content) {
  return http.post(`/admin/devices/${id}/send-command`, { content })
}
