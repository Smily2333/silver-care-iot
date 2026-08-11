import { describe, expect, it } from 'vitest'
import { absoluteMapUrl } from './config.js'

describe('map config', () => {
  it('keeps MapLibre glyph placeholders in absolute URLs', () => {
    const url = absoluteMapUrl('/maps/assets/fonts/{fontstack}/{range}.pbf')
    expect(url).toContain('/maps/assets/fonts/{fontstack}/{range}.pbf')
  })
})
