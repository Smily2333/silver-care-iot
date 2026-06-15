const { getHealthRecords } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    records: []
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
    wx.setNavigationBarTitle({ title: '健康数据' })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true })
    return getHealthRecords(deviceNo, 20)
      .then(records => {
        const formatted = records.map(r => ({
          ...r,
          measuredAtStr: this.formatTime(r.measuredAt)
        }))
        this.setData({ records: formatted })
      })
      .catch(err => {
        wx.showToast({ title: err.message, icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
