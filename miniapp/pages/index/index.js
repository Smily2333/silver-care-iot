const { bindDevice, getOverview } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: false,
    errorMsg: '',
    recentList: []
  },

  onLoad() {
    const raw = wx.getStorageSync('recentDevices') || []
    // 兼容旧格式（纯字符串）自动迁移为对象格式
    const recent = raw.map(item =>
      typeof item === 'string' ? { deviceNo: item, ownerName: '' } : item
    )
    this.setData({ recentList: this._withDisplay(recent) })
  },

  onInput(e) {
    this.setData({ deviceNo: e.detail.value, errorMsg: '' })
  },

  onQuery() {
    const no = this.data.deviceNo.trim()
    if (!no) {
      this.setData({ errorMsg: '请输入设备编号' })
      return
    }
    this.setData({ loading: true, errorMsg: '' })
    getOverview(no)
      .then(res => {
        const ownerName = (res && res.device && res.device.ownerName) || ''
        this._saveRecent(no, ownerName)
        wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${encodeURIComponent(no)}` })
      })
      .catch(err => {
        if (err.statusCode === 403) {
          return this._confirmBinding(no)
        }
        this.setData({ errorMsg: err.message })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  _confirmBinding(no) {
    return new Promise(resolve => {
      wx.showModal({
        title: '绑定设备',
        content: '请输入佩戴人的姓名，确认后该设备将绑定到当前微信。',
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
        this.setData({ errorMsg: '请输入佩戴人姓名' })
        return
      }
      return bindDevice(no, ownerName)
        .then(device => {
          this._saveRecent(no, device.ownerName || ownerName)
          wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${encodeURIComponent(no)}` })
        })
        .catch(err => {
          this.setData({ errorMsg: err.message })
        })
    })
  },

  onRecentTap(e) {
    const no = e.currentTarget.dataset.no
    this.setData({ deviceNo: no })
    this.onQuery()
  },

  _saveRecent(no, ownerName) {
    let list = wx.getStorageSync('recentDevices') || []
    // 兼容旧格式
    list = list.map(item =>
      typeof item === 'string' ? { deviceNo: item, ownerName: '' } : item
    )
    list = [{ deviceNo: no, ownerName: ownerName || '' }, ...list.filter(item => item.deviceNo !== no)].slice(0, 5)
    wx.setStorageSync('recentDevices', list)
    this.setData({ recentList: this._withDisplay(list) })
  },

  _withDisplay(list) {
    return list.map(item => {
      const displayName = item.ownerName || item.deviceNo
      return { ...item, displayName, displayInitial: displayName[0] }
    })
  }
})
