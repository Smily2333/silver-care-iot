const { getOverview, updateOwnerName } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    data: { device: {}, latestHealth: null, latestLocation: null },
    lastHeartbeat: '-',
    measuredAt: '-',
    locatedAt: '-',
    displayName: '',
    editing: false,
    editValue: '',
    saving: false
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
        const displayName = res.device.ownerName || deviceNo
        this.setData({
          data: res,
          displayName,
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

  startEdit() {
    this.setData({ editing: true, editValue: this.data.data.device.ownerName || '' })
  },

  onEditInput(e) {
    this.setData({ editValue: e.detail.value })
  },

  saveOwnerName() {
    const name = this.data.editValue.trim()
    if (!name || this.data.saving) return
    this.setData({ saving: true })
    updateOwnerName(this.data.deviceNo, name)
      .then(updatedDevice => {
        const newData = { ...this.data.data, device: updatedDevice }
        this.setData({
          data: newData,
          displayName: updatedDevice.ownerName || this.data.deviceNo,
          editing: false,
          saving: false
        })
        wx.showToast({ title: '保存成功', icon: 'success' })
      })
      .catch(err => {
        this.setData({ saving: false })
        wx.showToast({ title: err.message, icon: 'none' })
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
