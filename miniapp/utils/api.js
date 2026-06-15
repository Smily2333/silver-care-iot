// 替换为实际后端地址
const BASE_URL = 'http://120.53.225.169:8080'

function request(path, params) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      data: params || {},
      method: 'GET',
      header: { 'Content-Type': 'application/json' },
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 404) {
          reject(new Error('设备不存在'))
        } else {
          reject(new Error('请求失败：' + res.statusCode))
        }
      },
      fail: err => reject(new Error(err.errMsg || '网络错误'))
    })
  })
}

module.exports = {
  getOverview(deviceNo) {
    return request(`/api/miniapp/devices/${deviceNo}/overview`)
  },
  getHealthRecords(deviceNo, size = 20) {
    return request(`/api/miniapp/devices/${deviceNo}/health-records`, { size })
  },
  getLocationRecords(deviceNo, size = 20) {
    return request(`/api/miniapp/devices/${deviceNo}/location-records`, { size })
  }
}
