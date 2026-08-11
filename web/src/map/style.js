import { layers, namedFlavor } from '@protomaps/basemaps'
import { absoluteMapUrl, mapConfig } from './config.js'

export function createMapStyle() {
  if (mapConfig.mode !== 'pmtiles') {
    return {
      version: 8,
      sources: {
        openstreetmap: {
          type: 'raster',
          tiles: [mapConfig.rasterTileUrl],
          tileSize: 256,
          attribution: mapConfig.attribution
        }
      },
      layers: [{ id: 'openstreetmap', type: 'raster', source: 'openstreetmap' }]
    }
  }
  return {
    version: 8,
    glyphs: absoluteMapUrl(mapConfig.glyphsUrl),
    sprite: absoluteMapUrl(mapConfig.spriteUrl),
    sources: {
      protomaps: {
        type: 'vector',
        url: `pmtiles://${absoluteMapUrl(mapConfig.pmtilesUrl)}`,
        attribution: mapConfig.attribution
      }
    },
    layers: layers('protomaps', namedFlavor('light'), { lang: 'zh' })
  }
}
