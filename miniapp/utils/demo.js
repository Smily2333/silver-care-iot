const DEMO_DEVICE_NO = 'DEMO-DEVICE'

function isoMinutesAgo(minutes) {
  return new Date(Date.now() - minutes * 60 * 1000).toISOString()
}

export function isDemoMode(options = {}) {
  return options.demo === '1' || options.demo === 1 || options.demo === true
}

export function getDemoOverview() {
  const health = getDemoHealthRecords()[0]
  const location = getDemoLocationRecords()[0]
  return {
    device: {
      deviceNo: DEMO_DEVICE_NO,
      ownerName: '演示佩戴人',
      model: '智能健康手表（示例）',
      status: 'ONLINE',
      batteryLevel: 82,
      stepCount: 3680,
      lastOnlineAt: isoMinutesAgo(2)
    },
    latestHealth: health,
    latestLocation: location
  }
}

export function getDemoHealthRecords() {
  return [
    { id: 'demo-health-1', heartRate: 76, systolicPressure: 122, diastolicPressure: 78, bodyTemperature: 36.5, oxygenSaturation: 98, measuredAt: isoMinutesAgo(8) },
    { id: 'demo-health-2', heartRate: 72, systolicPressure: 119, diastolicPressure: 76, bodyTemperature: 36.4, oxygenSaturation: 97, measuredAt: isoMinutesAgo(68) },
    { id: 'demo-health-3', heartRate: 79, systolicPressure: 124, diastolicPressure: 80, bodyTemperature: 36.6, oxygenSaturation: 98, measuredAt: isoMinutesAgo(188) }
  ]
}

export function getDemoLocationRecords() {
  return [
    demoLocation('demo-location-1', 39.90465, 116.40842, 12),
    demoLocation('demo-location-2', 39.90392, 116.40688, 42),
    demoLocation('demo-location-3', 39.90296, 116.40571, 72)
  ]
}

function demoLocation(id, latitude, longitude, minutesAgo) {
  return {
    id,
    latitude,
    longitude,
    mapLatitude: latitude,
    mapLongitude: longitude,
    gpsValid: true,
    satelliteCount: 8,
    gsmSignal: 24,
    speed: 0,
    locatedAt: isoMinutesAgo(minutesAgo)
  }
}

export function getDemoAlerts() {
  return [
    {
      id: 'demo-alert-1',
      alertedAt: isoMinutesAgo(36),
      latitude: 39.90392,
      longitude: 116.40688,
      mapLatitude: 39.90392,
      mapLongitude: 116.40688,
      gpsValid: true
    }
  ]
}

export function getDemoLatestAlert() {
  return getDemoAlerts()[0]
}

export const demoDeviceNo = DEMO_DEVICE_NO
