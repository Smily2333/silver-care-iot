const { getHealthRecords } = require('../../utils/api')
const { getDemoHealthRecords, isDemoMode, demoDeviceNo } = require('../../utils/demo')
const { enableSharing, getShareAppMessage, getShareTimeline } = require('../../utils/share')

Page({
  data: {
    deviceNo: '',
    demo: false,
    loading: true,
    records: [],
    errorMsg: ''
  },

  onLoad(options) {
    enableSharing()
    const demo = isDemoMode(options)
    const deviceNo = demo ? demoDeviceNo : (options.deviceNo || '')
    this.setData({ deviceNo, demo })
    wx.setNavigationBarTitle({ title: demo ? '健康趋势（示例）' : '健康趋势' })
    this.load(deviceNo)
  },

  onShareAppMessage() {
    return getShareAppMessage()
  },

  onShareTimeline() {
    return getShareTimeline()
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true, errorMsg: '' })
    const request = this.data.demo ? Promise.resolve(getDemoHealthRecords()) : getHealthRecords(deviceNo, 20)
    return request
      .then(records => {
        const formatted = records.map(r => ({
          ...r,
          measuredAtStr: this.formatTime(r.measuredAt)
        }))
        this.setData({ records: formatted })
      })
      .catch(err => {
        this.setData({ errorMsg: err.message || '暂时无法加载健康记录' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  retryLoad() {
    this.load(this.data.deviceNo)
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    if (Number.isNaN(d.getTime())) return '-'
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
