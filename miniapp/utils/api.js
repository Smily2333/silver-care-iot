const { getApiBaseUrl } = require('../config')
const ACCESS_TOKEN_KEY = 'miniappAccessToken'

let loginPromise = null

class ApiError extends Error {
  constructor(message, statusCode) {
    super(message)
    this.statusCode = statusCode
  }
}

function wxRequest(path, method, data, accessToken) {
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    if (accessToken) {
      header.Authorization = `Bearer ${accessToken}`
    }
    wx.request({
      url: getApiBaseUrl() + path,
      data: data || {},
      method,
      header,
      success: resolve,
      fail: err => reject(new Error(err.errMsg || '网络错误'))
    })
  })
}

function loginCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: result => result.code ? resolve(result.code) : reject(new Error('微信登录失败')),
      fail: err => reject(new Error(err.errMsg || '微信登录失败'))
    })
  })
}

export function loginMiniapp(force = false) {
  if (force) {
    wx.removeStorageSync(ACCESS_TOKEN_KEY)
  }
  const existingToken = wx.getStorageSync(ACCESS_TOKEN_KEY)
  if (existingToken) return Promise.resolve(existingToken)
  if (loginPromise) return loginPromise

  loginPromise = loginCode()
    .then(code => wxRequest('/api/miniapp/auth/login', 'POST', { code }))
    .then(res => {
      if (res.statusCode !== 200 || !res.data?.accessToken) {
        throw responseError(res)
      }
      wx.setStorageSync(ACCESS_TOKEN_KEY, res.data.accessToken)
      return res.data.accessToken
    })
    .finally(() => {
      loginPromise = null
    })
  return loginPromise
}

function request(path, method = 'GET', data = {}, retried = false) {
  return loginMiniapp()
    .then(accessToken => wxRequest(path, method, data, accessToken))
    .then(res => {
      if (res.statusCode >= 200 && res.statusCode < 300) {
        return res.statusCode === 204 ? null : res.data
      }
      if (res.statusCode === 401 && !retried) {
        return loginMiniapp(true).then(() => request(path, method, data, true))
      }
      throw responseError(res)
    })
}

function responseError(res) {
  const messages = {
    400: '提交的数据不正确',
    401: '微信登录已失效，请重试',
    403: '设备尚未绑定到当前微信',
    404: '设备不存在',
    409: '设备已被其他账号绑定',
    502: '微信登录服务暂时不可用',
    503: '服务器尚未配置微信登录'
  }
  return new ApiError(messages[res.statusCode] || `请求失败：${res.statusCode}`, res.statusCode)
}

function devicePath(deviceNo, suffix) {
  return `/api/miniapp/devices/${encodeURIComponent(deviceNo)}${suffix}`
}

export function bindDevice(deviceNo, ownerName) {
  return request('/api/miniapp/devices/bind', 'POST', { deviceNo, ownerName })
}

export function getOverview(deviceNo) {
  return request(devicePath(deviceNo, '/overview'))
}

export function getHealthRecords(deviceNo, size = 20) {
  return request(devicePath(deviceNo, '/health-records'), 'GET', { size })
}

export function getLocationRecords(deviceNo, size = 20) {
  return request(devicePath(deviceNo, '/location-records'), 'GET', { size })
}

export function updateOwnerName(deviceNo, ownerName) {
  return request(devicePath(deviceNo, '/owner-name'), 'PATCH', { ownerName })
}

export function getFallAlerts(deviceNo, size = 20) {
  return request(devicePath(deviceNo, '/fall-alerts'), 'GET', { size })
}

export function getLatestFallAlert(deviceNo) {
  return request(devicePath(deviceNo, '/fall-alerts/latest'))
}
