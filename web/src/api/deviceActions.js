import http from './axios.js'

export function getActionCapabilities(deviceId) {
  return http.get(`/admin/devices/${deviceId}/actions/capabilities`)
}

export function createDeviceAction(deviceId, type) {
  return http.post(`/admin/devices/${deviceId}/actions`, { type })
}

export function getDeviceAction(deviceId, actionId) {
  return http.get(`/admin/devices/${deviceId}/actions/${actionId}`)
}

export function listDeviceActions(deviceId) {
  return http.get(`/admin/devices/${deviceId}/actions`)
}
