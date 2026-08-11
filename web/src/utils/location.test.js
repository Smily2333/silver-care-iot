import { describe, expect, it } from 'vitest'
import {
  buildPointGeoJson,
  buildTrackGeoJson,
  getLatestValidLocation,
  getValidLocationRecords,
  isValidWgs84Coordinate
} from './location.js'

const records = [
  { id: 3, latitude: '39.0300', longitude: '117.7000', gpsValid: true, locatedAt: '2026-08-11T10:03:00Z' },
  { id: 1, latitude: '39.0100', longitude: '117.6800', gpsValid: true, locatedAt: '2026-08-11T10:01:00Z' },
  { id: 2, latitude: '39.0200', longitude: '117.6900', gpsValid: false, locatedAt: '2026-08-11T10:02:00Z' }
]

describe('location helpers', () => {
  it('only accepts valid WGS84 coordinates', () => {
    expect(isValidWgs84Coordinate(39, 117)).toBe(true)
    expect(isValidWgs84Coordinate(91, 117)).toBe(false)
    expect(isValidWgs84Coordinate(39, Number.NaN)).toBe(false)
  })

  it('excludes GPS-invalid records from the map', () => {
    expect(getValidLocationRecords(records).map(record => record.id)).toEqual([3, 1])
  })

  it('builds tracks chronologically using longitude-latitude order', () => {
    const track = buildTrackGeoJson(records)
    expect(track.features[0].geometry.coordinates).toEqual([
      [117.68, 39.01],
      [117.7, 39.03]
    ])
  })

  it('marks the newest valid point as latest', () => {
    const points = buildPointGeoJson(records)
    expect(points.features.find(feature => feature.properties.isLatest).properties.recordId).toBe('3')
    expect(getLatestValidLocation(records).id).toBe(3)
  })
})
