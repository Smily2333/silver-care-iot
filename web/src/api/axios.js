import axios from 'axios'

const ADMIN_AUTH_KEY = 'silverCareAdminAuth'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

instance.interceptors.request.use(config => {
  const authorization = sessionStorage.getItem(ADMIN_AUTH_KEY)
  if (authorization) {
    config.headers.Authorization = authorization
  }
  return config
})

instance.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      clearAdminCredentials()
      if (window.location.pathname !== '/login') {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.assign(`/login?redirect=${redirect}`)
      }
    }
    return Promise.reject(error)
  }
)

export function setAdminCredentials(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  let binary = ''
  bytes.forEach(byte => { binary += String.fromCharCode(byte) })
  sessionStorage.setItem(ADMIN_AUTH_KEY, `Basic ${btoa(binary)}`)
}

export function hasAdminCredentials() {
  return Boolean(sessionStorage.getItem(ADMIN_AUTH_KEY))
}

export function clearAdminCredentials() {
  sessionStorage.removeItem(ADMIN_AUTH_KEY)
}

export default instance
