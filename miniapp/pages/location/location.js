const { getLocationRecords } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    records: [],
    center: { lat: 39.984120, lng: 116.307484 },
    markers: [],
    polyline: []
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
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
    if (!records.length) return { markers: [], polyline: [], center: this.data.center }

    const latest = records[0]
    const center = { lat: Number(latest.latitude), lng: Number(latest.longitude) }

    const markers = [{
      id: 1,
      latitude: Number(latest.latitude),
      longitude: Number(latest.longitude),
      title: '当前位置',
      iconPath: '/images/marker.png',
      width: 32,
      height: 40
    }]

    // records are newest-first; reverse for chronological polyline
    const points = [...records].reverse().map(r => ({
      latitude: Number(r.latitude),
      longitude: Number(r.longitude)
    }))

    const polyline = [{
      points,
      color: '#3399FFCC',
      width: 4,
      arrowLine: true
    }]

    return { center, markers, polyline }
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
