const { getLocationRecords } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    focusPoint: null,
    records: [],
    center: { lat: 39.984120, lng: 116.307484 },
    markers: [],
    polyline: []
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    const lat = Number(options.lat)
    const lng = Number(options.lng)
    const focusPoint = Number.isFinite(lat) && Number.isFinite(lng) ? { lat, lng } : null
    this.setData({ deviceNo, focusPoint })
    wx.setNavigationBarTitle({ title: '位置轨迹' })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true })
    return getLocationRecords(deviceNo, 20)
      .then(records => {
        const formatted = records.map(r => ({
          ...r,
          locatedAtStr: this.formatTime(r.locatedAt)
        }))

        const mapData = this._buildMapData(formatted)
        this.setData({ records: formatted, ...mapData })
      })
      .catch(err => {
        wx.showToast({ title: err.message, icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
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
      title: '跌倒警报位置',
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
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
