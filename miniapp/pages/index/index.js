const { bindDevice, getOverview, getLatestFallAlert } = require('../../utils/api')
const { enableSharing, getShareAppMessage, getShareTimeline } = require('../../utils/share')
const MAX_RECENT_DEVICES = 4

Page({
  data: {
    deviceNo: '',
    loading: false,
    errorMsg: '',
    recentList: [],
    otherRecent: [],
    hasDevices: false,
    primarySummary: null,
    summaryLoading: false,
    summaryError: '',
    bindingExpanded: false,
    inputFocus: false
  },

  onLoad() {
    enableSharing()
    this._loadRecent()
  },

  onShareAppMessage() {
    return getShareAppMessage()
  },

  onShareTimeline() {
    return getShareTimeline()
  },

  onShow() {
    this._refreshHome()
  },

  onPullDownRefresh() {
    this._refreshHome().finally(() => wx.stopPullDownRefresh())
  },

  _refreshHome() {
    const recent = this._loadRecent()
    if (recent.length === 0) {
      this.setData({ summaryLoading: false, summaryError: '', primarySummary: null })
      return Promise.resolve()
    }

    const primary = recent[0]
    this.setData({
      summaryLoading: true,
      summaryError: '',
      primarySummary: this._summaryFromCache(primary)
    })

    return Promise.all([
      getOverview(primary.deviceNo),
      getLatestFallAlert(primary.deviceNo).catch(() => null)
    ])
      .then(([overview, alert]) => {
        this.setData({
          primarySummary: this._summaryFromOverview(primary, overview, alert),
          summaryError: ''
        })
      })
      .catch(err => {
        this.setData({ summaryError: err.message || '暂时无法同步设备状态' })
      })
      .finally(() => {
        this.setData({ summaryLoading: false })
      })
  },

  retrySummary() {
    this._refreshHome()
  },

  showBinding() {
    this.setData({ bindingExpanded: true, inputFocus: true, errorMsg: '' }, () => {
      wx.pageScrollTo({ selector: '#device-binding', duration: 260 })
    })
  },

  cancelBinding() {
    this.setData({ bindingExpanded: false, inputFocus: false, deviceNo: '', errorMsg: '' })
  },

  onInput(e) {
    this.setData({ deviceNo: e.detail.value, errorMsg: '' })
  },

  onQuery() {
    const no = this.data.deviceNo.trim()
    if (!no) {
      this.setData({ errorMsg: '请输入手表背面或包装上的设备编号', inputFocus: true })
      return
    }
    if (this.data.loading) return

    this.setData({ loading: true, errorMsg: '' })
    getOverview(no)
      .then(res => {
        const ownerName = (res && res.device && res.device.ownerName) || ''
        this._saveRecent(no, ownerName)
        this.setData({ bindingExpanded: false })
        wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${encodeURIComponent(no)}` })
      })
      .catch(err => {
        if (err.statusCode === 403) return this._confirmBinding(no)
        this.setData({ errorMsg: err.message || '暂时无法连接服务，请稍后重试' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  _confirmBinding(no) {
    return new Promise(resolve => {
      wx.showModal({
        title: '绑定智能手表',
        content: '请在获得佩戴人授权后输入称呼。绑定后，当前微信可查看该设备上传的健康、位置和疑似跌倒事件。',
        editable: true,
        placeholderText: '例如：张奶奶',
        confirmText: '确认绑定',
        success: resolve,
        fail: () => resolve({ confirm: false })
      })
    }).then(result => {
      if (!result.confirm) return
      const ownerName = (result.content || '').trim()
      if (!ownerName) {
        this.setData({ errorMsg: '请输入佩戴人称呼' })
        return
      }
      return bindDevice(no, ownerName)
        .then(device => {
          this._saveRecent(no, device.ownerName || ownerName)
          this.setData({ bindingExpanded: false })
          wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${encodeURIComponent(no)}` })
        })
        .catch(err => {
          this.setData({ errorMsg: err.message || '设备绑定失败，请稍后重试' })
        })
    })
  },

  openPrimary() {
    const no = this.data.primarySummary?.deviceNo
    if (no) wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${encodeURIComponent(no)}` })
  },

  goPrimaryHealth() {
    this._goPrimaryPage('/pages/health/health')
  },

  goPrimaryLocation() {
    this._goPrimaryPage('/pages/location/location')
  },

  goPrimaryAlerts() {
    this._goPrimaryPage('/pages/alerts/alerts')
  },

  _goPrimaryPage(path) {
    const no = this.data.primarySummary?.deviceNo
    if (no) wx.navigateTo({ url: `${path}?deviceNo=${encodeURIComponent(no)}` })
  },

  onRecentTap(e) {
    const no = e.currentTarget.dataset.no
    wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${encodeURIComponent(no)}` })
  },

  enterDemo() {
    wx.navigateTo({ url: '/pages/overview/overview?demo=1' })
  },

  openGuide() {
    wx.navigateTo({ url: '/pages/guide/guide' })
  },

  openPrivacy() {
    wx.navigateTo({ url: '/pages/privacy/privacy' })
  },

  clearRecent() {
    wx.showModal({
      title: '清除本机记录',
      content: '仅清除本机快捷入口，不会解除设备与微信账号的绑定。',
      confirmText: '确认清除',
      success: result => {
        if (!result.confirm) return
        wx.removeStorageSync('recentDevices')
        this.setData({
          recentList: [],
          otherRecent: [],
          hasDevices: false,
          primarySummary: null,
          deviceNo: ''
        })
      }
    })
  },

  _loadRecent() {
    const raw = wx.getStorageSync('recentDevices') || []
    const recent = raw.map(item =>
      typeof item === 'string' ? { deviceNo: item, ownerName: '' } : item
    ).filter(item => item && item.deviceNo).slice(0, MAX_RECENT_DEVICES)
    const displayList = this._withDisplay(recent)
    this.setData({
      recentList: displayList,
      otherRecent: displayList.slice(1),
      hasDevices: displayList.length > 0,
      primarySummary: recent.length > 0 ? this._summaryFromCache(recent[0]) : null
    })
    return recent
  },

  _saveRecent(no, ownerName) {
    let list = wx.getStorageSync('recentDevices') || []
    list = list.map(item =>
      typeof item === 'string' ? { deviceNo: item, ownerName: '' } : item
    )
    list = [{ deviceNo: no, ownerName: ownerName || '' }, ...list.filter(item => item.deviceNo !== no)]
      .slice(0, MAX_RECENT_DEVICES)
    wx.setStorageSync('recentDevices', list)
    this._loadRecent()
  },

  _withDisplay(list) {
    return list.map(item => {
      const displayName = item.ownerName || item.deviceNo
      return { ...item, displayName, displayInitial: displayName[0] || '设' }
    })
  },

  _summaryFromCache(device) {
    const displayName = device.ownerName || device.deviceNo
    return {
      deviceNo: device.deviceNo,
      displayName,
      displayInitial: displayName[0] || '设',
      online: false,
      statusText: '同步中',
      batteryText: '--',
      stepText: '--',
      lastSyncText: '正在获取设备状态',
      healthValue: '--',
      healthHint: '健康趋势',
      locationValue: '--',
      locationHint: '位置记录',
      alertValue: '--',
      alertHint: '安全事件',
      hasNewAlert: false
    }
  },

  _summaryFromOverview(cached, overview, alert) {
    const device = overview.device || {}
    const health = overview.latestHealth
    const location = overview.latestLocation
    const displayName = device.ownerName || cached.ownerName || cached.deviceNo
    const safeAlert = this._isPlausibleAlert(alert) ? alert : null
    const lastSeen = wx.getStorageSync('lastSeenAlertAt_' + cached.deviceNo)
    const hasNewAlert = Boolean(safeAlert?.alertedAt && safeAlert.alertedAt !== lastSeen)
    return {
      deviceNo: cached.deviceNo,
      displayName,
      displayInitial: displayName[0] || '设',
      online: device.status === 'ONLINE',
      statusText: device.status === 'ONLINE' ? '在线' : '离线',
      batteryText: device.batteryLevel != null ? `${device.batteryLevel}%` : '--',
      stepText: device.stepCount != null ? String(device.stepCount) : '--',
      lastSyncText: device.lastHeartbeatAt ? `最后同步 ${this._relativeTime(device.lastHeartbeatAt)}` : '暂无同步时间',
      healthValue: health?.heartRate != null ? `${health.heartRate} bpm` : '--',
      healthHint: health?.measuredAt ? this._relativeTime(health.measuredAt) : '暂无健康数据',
      locationValue: location ? '已定位' : '--',
      locationHint: location?.locatedAt ? this._relativeTime(location.locatedAt) : '暂无位置记录',
      alertValue: hasNewAlert ? '有新事件' : (safeAlert ? '查看记录' : '暂无记录'),
      alertHint: safeAlert?.alertedAt ? this._relativeTime(safeAlert.alertedAt) : '安全事件',
      hasNewAlert
    }
  },

  _isPlausibleAlert(alert) {
    if (!alert?.alertedAt) return false
    const time = new Date(alert.alertedAt).getTime()
    return Number.isFinite(time) && time <= Date.now() + 30 * 24 * 60 * 60 * 1000
  },

  _relativeTime(isoStr) {
    const time = new Date(isoStr).getTime()
    if (!Number.isFinite(time)) return '时间未知'
    const diffMinutes = Math.floor((Date.now() - time) / 60000)
    if (diffMinutes < 1) return '刚刚'
    if (diffMinutes < 60) return `${diffMinutes}分钟前`
    if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)}小时前`
    if (diffMinutes < 43200) return `${Math.floor(diffMinutes / 1440)}天前`
    const date = new Date(time)
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
})
