const { getOverview, updateOwnerName, getLatestFallAlert } = require('../../utils/api')

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
    saving: false,
    hasNewAlert: false,
    latestAlert: null,
    latestAlertTime: '-'
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
    wx.setNavigationBarTitle({ title: deviceNo })
    this.load(deviceNo)
  },

  onShow() {
    if (this.data.deviceNo) {
      this.checkLatestAlert(this.data.deviceNo)
    }
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo)
      .then(() => this.checkLatestAlert(this.data.deviceNo))
      .finally(() => wx.stopPullDownRefresh())
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

  goAlerts() {
    const alert = this.data.latestAlert
    if (alert && alert.alertedAt) {
      wx.setStorageSync('lastSeenAlertAt_' + this.data.deviceNo, alert.alertedAt)
    }
    this.setData({ hasNewAlert: false })
    wx.navigateTo({ url: `/pages/alerts/alerts?deviceNo=${this.data.deviceNo}` })
  },

  checkLatestAlert(deviceNo) {
    return getLatestFallAlert(deviceNo)
      .then(alert => {
        if (!alert || !alert.alertedAt) {
          this.setData({ hasNewAlert: false, latestAlert: null, latestAlertTime: '-' })
          return
        }

        const lastSeen = wx.getStorageSync('lastSeenAlertAt_' + deviceNo)
        const isNew = alert.alertedAt !== lastSeen
        this.setData({
          hasNewAlert: isNew,
          latestAlert: alert,
          latestAlertTime: this.formatTime(alert.alertedAt)
        })

        if (isNew) {
          wx.showModal({
            title: '跌倒警报',
            content: `检测到疑似跌倒事件\n时间：${this.formatTime(alert.alertedAt)}`,
            confirmText: '查看',
            cancelText: '稍后',
            success: res => {
              if (res.confirm) {
                this.goAlerts()
              }
            }
          })
        }
      })
      .catch(() => {
        // 告警轮询不阻塞概览页主数据展示
      })
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
