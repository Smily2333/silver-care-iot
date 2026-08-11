const { getLocationRecords } = require('../../utils/api')
const { getDemoLocationRecords, isDemoMode, demoDeviceNo } = require('../../utils/demo')
const { enableSharing, getShareAppMessage, getShareTimeline } = require('../../utils/share')

Page({
  data: {
    deviceNo: '',
    demo: false,
    loading: true,
    errorMsg: '',
    focusPoint: null,
    records: [],
    center: { lat: 39.984120, lng: 116.307484 },
    markers: [],
    polyline: []
  },

  onLoad(options) {
    enableSharing()
    const demo = isDemoMode(options)
    const deviceNo = demo ? demoDeviceNo : (options.deviceNo || '')
    const lat = Number(options.lat)
    const lng = Number(options.lng)
    const focusPoint = Number.isFinite(lat) && Number.isFinite(lng) ? { lat, lng } : null
    this.setData({ deviceNo, demo, focusPoint })
    wx.setNavigationBarTitle({ title: demo ? '位置记录（示例）' : '位置记录' })
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
    const request = this.data.demo ? Promise.resolve(getDemoLocationRecords()) : getLocationRecords(deviceNo, 20)
    return request
      .then(records => {
        const formatted = records.map(r => ({
          ...r,
          locatedAtStr: this.formatTime(r.locatedAt)
        }))

        const mapData = this._buildMapData(formatted)
        this.setData({ records: formatted, ...mapData })
      })
      .catch(err => {
        this.setData({ errorMsg: err.message || '暂时无法加载位置记录' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  retryLoad() {
    this.load(this.data.deviceNo)
  },

  _buildMapData(records) {
    if (!records.length && this.data.focusPoint) {
      return {
        center: this.data.focusPoint,
        markers: [this._focusMarker(this.data.focusPoint)],
        polyline: []
      }
    }
    if (!records.length) return { markers: [], polyline: [], center: this.data.center }

    const latest = records[0]
    const latestPoint = this._mapPoint(latest)
    const center = this.data.focusPoint || latestPoint

    const markers = [{
      id: 1,
      latitude: latestPoint.lat,
      longitude: latestPoint.lng,
      title: '当前位置',
      iconPath: '/images/marker.png',
      width: 32,
      height: 40
    }]
    if (this.data.focusPoint) {
      markers.unshift(this._focusMarker(this.data.focusPoint))
    }

    // records are newest-first; reverse for chronological polyline
    const points = [...records].reverse()
      .map(r => this._mapPoint(r))
      .filter(point => Number.isFinite(point.lat) && Number.isFinite(point.lng))
      .map(point => ({ latitude: point.lat, longitude: point.lng }))

    const polyline = [{
      points,
      color: '#3399FFCC',
      width: 4,
      arrowLine: true
    }]

    return { center, markers, polyline }
  },

  _focusMarker(point) {
    return {
      id: 99,
      latitude: point.lat,
      longitude: point.lng,
      title: '疑似跌倒事件位置',
      iconPath: '/images/marker.png',
      width: 36,
      height: 45
    }
  },

  _mapPoint(record) {
    return {
      lat: Number(record.mapLatitude ?? record.latitude),
      lng: Number(record.mapLongitude ?? record.longitude)
    }
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    if (Number.isNaN(d.getTime())) return '-'
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
