const { getOverview, updateOwnerName, getLatestFallAlert } = require('../../utils/api')
const { getDemoOverview, getDemoLatestAlert, isDemoMode, demoDeviceNo } = require('../../utils/demo')
const { enableSharing, getShareAppMessage, getShareTimeline } = require('../../utils/share')

Page({
  data: {
    deviceNo: '',
    demo: false,
    loading: true,
    errorMsg: '',
    data: { device: {}, latestHealth: null, latestLocation: null },
    lastHeartbeat: '-',
    measuredAt: '-',
    locatedAt: '-',
    displayName: '',
    editing: false,
    editValue: '',
    canSave: false,
    saving: false,
    hasNewAlert: false,
    latestAlert: null,
    latestAlertTime: '-'
  },

  onLoad(options) {
    enableSharing()
    const demo = isDemoMode(options)
    const deviceNo = demo ? demoDeviceNo : (options.deviceNo || '')
    this.setData({ deviceNo, demo })
    wx.setNavigationBarTitle({ title: demo ? '守护中心（示例）' : deviceNo })
    this.load(deviceNo)
  },

  onShareAppMessage() {
    return getShareAppMessage()
  },

  onShareTimeline() {
    return getShareTimeline()
  },

  onShow() {
    if (!this.data.deviceNo) return
    if (this.data.demo) {
      this._applyLatestAlert(getDemoLatestAlert())
      return
    }
    this.checkLatestAlert(this.data.deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo)
      .then(() => this.data.demo ? null : this.checkLatestAlert(this.data.deviceNo))
      .finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true, errorMsg: '' })
    const request = this.data.demo ? Promise.resolve(getDemoOverview()) : getOverview(deviceNo)
    return request
      .then(res => {
        const displayName = res.device.ownerName || deviceNo
        this.setData({
          data: res,
          displayName,
          lastHeartbeat: res.device.lastHeartbeatAt ? this.formatTime(res.device.lastHeartbeatAt) : '-',
          measuredAt: res.latestHealth?.measuredAt ? this.formatTime(res.latestHealth.measuredAt) : '-',
          locatedAt: res.latestLocation?.locatedAt ? this.formatTime(res.latestLocation.locatedAt) : '-'
        })
        if (this.data.demo) this._applyLatestAlert(getDemoLatestAlert())
      })
      .catch(err => {
        const message = err.message || '暂时无法加载设备数据'
        this.setData({ errorMsg: message })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  retryLoad() {
    this.load(this.data.deviceNo)
  },

  startEdit() {
    if (this.data.demo) {
      wx.showToast({ title: '示例模式为只读', icon: 'none' })
      return
    }
    const editValue = this.data.data.device.ownerName || ''
    this.setData({ editing: true, editValue, canSave: Boolean(editValue.trim()) })
  },

  onEditInput(e) {
    const editValue = e.detail.value
    this.setData({ editValue, canSave: Boolean(editValue.trim()) })
  },

  saveOwnerName() {
    if (this.data.demo) return
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
          canSave: false,
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
    wx.navigateTo({ url: this._pageUrl('/pages/health/health') })
  },

  goLocation() {
    wx.navigateTo({ url: this._pageUrl('/pages/location/location') })
  },

  goAlerts() {
    const alert = this.data.latestAlert
    if (!this.data.demo && alert && alert.alertedAt) {
      wx.setStorageSync('lastSeenAlertAt_' + this.data.deviceNo, alert.alertedAt)
    }
    this.setData({ hasNewAlert: false })
    wx.navigateTo({ url: this._pageUrl('/pages/alerts/alerts') })
  },

  _pageUrl(path) {
    return this.data.demo
      ? `${path}?demo=1`
      : `${path}?deviceNo=${encodeURIComponent(this.data.deviceNo)}`
  },

  checkLatestAlert(deviceNo) {
    return getLatestFallAlert(deviceNo)
      .then(alert => {
        if (!alert || !alert.alertedAt) {
          this.setData({ hasNewAlert: false, latestAlert: null, latestAlertTime: '-' })
          return
        }
        const lastSeen = wx.getStorageSync('lastSeenAlertAt_' + deviceNo)
        this._applyLatestAlert(alert, alert.alertedAt !== lastSeen)

        if (alert.alertedAt !== lastSeen) {
          wx.showModal({
            title: '疑似跌倒事件',
            content: `设备上报疑似跌倒事件\n时间：${this.formatTime(alert.alertedAt)}\n请结合实际情况及时确认。`,
            confirmText: '查看记录',
            cancelText: '稍后处理',
            success: res => {
              if (res.confirm) this.goAlerts()
            }
          })
        }
      })
      .catch(() => {
        // 告警轮询不阻塞概览页主要数据展示。
      })
  },

  _applyLatestAlert(alert, isNew = true) {
    this.setData({
      hasNewAlert: Boolean(alert && isNew),
      latestAlert: alert || null,
      latestAlertTime: alert?.alertedAt ? this.formatTime(alert.alertedAt) : '-'
    })
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    if (Number.isNaN(d.getTime())) return '-'
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
