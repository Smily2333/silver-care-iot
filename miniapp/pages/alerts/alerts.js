const { getFallAlerts } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    alerts: []
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
    wx.setNavigationBarTitle({ title: '跌倒警报' })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true })
    return getFallAlerts(deviceNo, 20)
      .then(alerts => {
        const formatted = alerts.map(alert => ({
          ...alert,
          alertedAtStr: this.formatTime(alert.alertedAt),
          locationText: this.formatLocation(alert),
          displayLatitude: alert.mapLatitude ?? alert.latitude,
          displayLongitude: alert.mapLongitude ?? alert.longitude,
          hasLocation: (alert.mapLatitude ?? alert.latitude) != null && (alert.mapLongitude ?? alert.longitude) != null
        }))

        if (formatted.length > 0) {
          wx.setStorageSync('lastSeenAlertAt_' + deviceNo, formatted[0].alertedAt)
        }

        this.setData({ alerts: formatted })
      })
      .catch(err => {
        wx.showToast({ title: err.message, icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  goLocation(e) {
    const query = [`deviceNo=${this.data.deviceNo}`]
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
