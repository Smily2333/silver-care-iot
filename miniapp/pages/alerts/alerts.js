const { getFallAlerts } = require('../../utils/api')
const { getDemoAlerts, isDemoMode, demoDeviceNo } = require('../../utils/demo')

Page({
  data: {
    deviceNo: '',
    demo: false,
    loading: true,
    alerts: [],
    errorMsg: ''
  },

  onLoad(options) {
    const demo = isDemoMode(options)
    const deviceNo = demo ? demoDeviceNo : (options.deviceNo || '')
    this.setData({ deviceNo, demo })
    wx.setNavigationBarTitle({ title: demo ? '安全事件（示例）' : '安全事件' })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true, errorMsg: '' })
    const request = this.data.demo ? Promise.resolve(getDemoAlerts()) : getFallAlerts(deviceNo, 20)
    return request
      .then(alerts => {
        const formatted = alerts.map(alert => ({
          ...alert,
          alertedAtStr: this.formatTime(alert.alertedAt),
          locationText: this.formatLocation(alert),
          displayLatitude: alert.mapLatitude ?? alert.latitude,
          displayLongitude: alert.mapLongitude ?? alert.longitude,
          hasLocation: (alert.mapLatitude ?? alert.latitude) != null && (alert.mapLongitude ?? alert.longitude) != null
        }))

        if (!this.data.demo && formatted.length > 0) {
          wx.setStorageSync('lastSeenAlertAt_' + deviceNo, formatted[0].alertedAt)
        }

        this.setData({ alerts: formatted })
      })
      .catch(err => {
        this.setData({ errorMsg: err.message || '暂时无法加载安全事件' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  retryLoad() {
    this.load(this.data.deviceNo)
  },

  goLocation(e) {
    const query = this.data.demo ? ['demo=1'] : [`deviceNo=${encodeURIComponent(this.data.deviceNo)}`]
    const { lat, lng } = e.currentTarget.dataset
    if (lat != null && lng != null) {
      query.push(`lat=${lat}`)
      query.push(`lng=${lng}`)
    }
    wx.navigateTo({ url: `/pages/location/location?${query.join('&')}` })
  },

  formatLocation(alert) {
    if (alert.latitude == null || alert.longitude == null) {
      return '暂无位置坐标'
    }
    return `${alert.latitude}, ${alert.longitude}`
  },

  formatTime(isoStr) {
    if (!isoStr) return '-'
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
