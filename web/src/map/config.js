function numberFromEnv(value, fallback) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export const mapConfig = Object.freeze({
  mode: import.meta.env.VITE_MAP_MODE || 'raster',
  rasterTileUrl: import.meta.env.VITE_MAP_RASTER_TILE_URL ||
    'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  pmtilesUrl: import.meta.env.VITE_MAP_PMTILES_URL || '/maps/tianjin.pmtiles',
  glyphsUrl: import.meta.env.VITE_MAP_GLYPHS_URL || '/maps/assets/fonts/{fontstack}/{range}.pbf',
  spriteUrl: import.meta.env.VITE_MAP_SPRITE_URL || '/maps/assets/sprites/v4/light',
  attribution: import.meta.env.VITE_MAP_ATTRIBUTION ||
    '<a href="https://www.openstreetmap.org/copyright" target="_blank">\u00a9 OpenStreetMap contributors</a>',
  defaultCenter: [
    numberFromEnv(import.meta.env.VITE_MAP_DEFAULT_LONGITUDE, 117.7008),
    numberFromEnv(import.meta.env.VITE_MAP_DEFAULT_LATITUDE, 39.0321)
  ],
  defaultZoom: numberFromEnv(import.meta.env.VITE_MAP_DEFAULT_ZOOM, 13),
  maxZoom: numberFromEnv(import.meta.env.VITE_MAP_MAX_ZOOM, 18)
})

export function absoluteMapUrl(value) {
  if (typeof window === 'undefined') return value
  return new URL(value, window.location.origin).toString()
    .replaceAll('%7B', '{')
    .replaceAll('%7D', '}')
}
