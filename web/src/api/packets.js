import http from './axios.js'

export function listPackets(page = 0, size = 20) {
  return http.get('/admin/raw-packets', { params: { page, size, sort: 'id,desc' } })
}
