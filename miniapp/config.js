import { API_ENVIRONMENT } from './environment.js'

const API_BASE_URLS = {
  development: 'http://127.0.0.1:8080',
  production: 'https://api.nkucare.cloud'
}

export function getApiBaseUrl() {
  let envVersion = 'develop'
  try {
    envVersion = wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  } catch (_) {
    // Older developer tools use the development service by default.
  }
  const automaticEnvironment = envVersion === 'develop' ? 'development' : 'production'
  const selectedEnvironment = API_ENVIRONMENT === 'auto' ? automaticEnvironment : API_ENVIRONMENT
  const baseUrl = API_BASE_URLS[selectedEnvironment]
  if (!baseUrl) {
    throw new Error(`未知的小程序 API 环境：${selectedEnvironment}`)
  }
  return baseUrl
}
