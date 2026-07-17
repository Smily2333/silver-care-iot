const API_BASE_URLS = {
  develop: 'http://120.53.225.169:8080',
  trial: '',
  release: ''
}

export function getApiBaseUrl() {
  let envVersion = 'develop'
  try {
    envVersion = wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  } catch (_) {
    // Older developer tools use the development service by default.
  }
  const baseUrl = API_BASE_URLS[envVersion]
  if (!baseUrl) {
    throw new Error('正式服务域名尚未配置')
  }
  return baseUrl
}
