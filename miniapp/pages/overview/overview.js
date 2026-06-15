const { getOverview } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    data: { device: {}, latestHealth: null, latestLocation: null },
    lastHeartbeat: '-',
    measuredAt: '-',
    locatedAt: '-'
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
    wx.setNavigationBarTitle({ title: deviceNo })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true })
    return getOverview(deviceNo)
      .then(res => {
        this.setData({
          data: res,
          lastHeartbeat: res.device.lastHeartbeatAt ? this.formatTime(res.device.lastHeartbeatAt) : '-',
          measuredAt: res.latestHealth?.measuredAt ? this.formatTime(res.latestHealth.measuredAt) : '-',
          locatedAt: res.latestLocation?.locatedAt ? this.formatTime(res.latestLocation.locatedAt) : '-'
        })
      })
      .catch(err => {
        wx.showToast({ title: err.message, icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  goHealth() {
    wx.navigateTo({ url: `/pages/health/health?deviceNo=${this.data.deviceNo}` })
  },

  goLocation() {
    wx.navigateTo({ url: `/pages/location/location?deviceNo=${this.data.deviceNo}` })
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
