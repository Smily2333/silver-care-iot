export function normalizeLocationRecord(record) {
  return {
    ...record,
    displayLatitude: Number(record?.latitude),
    displayLongitude: Number(record?.longitude)
  }
}

export function isValidWgs84Coordinate(latitude, longitude) {
  return Number.isFinite(latitude) && Number.isFinite(longitude) &&
    latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
}

export function getValidLocationRecords(records = []) {
  return records
    .map(normalizeLocationRecord)
    .filter(record => record.gpsValid === true &&
      isValidWgs84Coordinate(record.displayLatitude, record.displayLongitude))
}

export function sortLocationsAscending(records = []) {
  return [...records].sort((left, right) => {
    const leftTime = Date.parse(left.locatedAt || '')
    const rightTime = Date.parse(right.locatedAt || '')
    if (!Number.isFinite(leftTime) && !Number.isFinite(rightTime)) return 0
    if (!Number.isFinite(leftTime)) return -1
    if (!Number.isFinite(rightTime)) return 1
    return leftTime - rightTime
  })
}

export function getLatestValidLocation(records = []) {
  const sorted = sortLocationsAscending(getValidLocationRecords(records))
  return sorted.at(-1) || null
}

function pointFeature(record, isLatest = false) {
  return {
    type: 'Feature',
    geometry: {
      type: 'Point',
      coordinates: [record.displayLongitude, record.displayLatitude]
    },
    properties: {
      recordId: String(record.id ?? ''),
      isLatest,
      locatedAt: record.locatedAt || '',
      approximateAddress: record.approximateAddress || '',
      speed: record.speed ?? '',
      batteryLevel: record.batteryLevel ?? ''
    }
  }
}

export function buildPointGeoJson(records = []) {
  const validRecords = sortLocationsAscending(getValidLocationRecords(records))
  const latestIndex = validRecords.length - 1
  return {
    type: 'FeatureCollection',
    features: validRecords.map((record, index) => pointFeature(record, index === latestIndex))
  }
}

export function buildTrackGeoJson(records = []) {
  const validRecords = sortLocationsAscending(getValidLocationRecords(records))
  return {
    type: 'FeatureCollection',
    features: validRecords.length > 1
      ? [{
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: validRecords.map(record => [record.displayLongitude, record.displayLatitude])
          }
        }]
      : []
  }
}

export function formatApproximateAddress(record) {
  if (record?.approximateAddress) return `约在 ${record.approximateAddress}`
  if (record?.addressStatus === 'PENDING') return '地址解析中'
  if (record?.addressStatus === 'FAILED') return '地址暂不可用'
  return '暂无地址信息'
}
