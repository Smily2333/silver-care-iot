const { getOverview } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: false,
    errorMsg: '',
    recentList: []
  },

  onLoad() {
    const recent = wx.getStorageSync('recentDevices') || []
    this.setData({ recentList: recent })
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
      .then(() => {
        this._saveRecent(no)
        wx.navigateTo({ url: `/pages/overview/overview?deviceNo=${no}` })
      })
      .catch(err => {
        this.setData({ errorMsg: err.message })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  onRecentTap(e) {
    const no = e.currentTarget.dataset.no
    this.setData({ deviceNo: no })
    this.onQuery()
  },

  _saveRecent(no) {
    let list = wx.getStorageSync('recentDevices') || []
    list = [no, ...list.filter(n => n !== no)].slice(0, 5)
    wx.setStorageSync('recentDevices', list)
    this.setData({ recentList: list })
  }
})
