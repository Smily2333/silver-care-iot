import maplibregl from 'maplibre-gl'
import { Protocol } from 'pmtiles'

let protocol = null

export function ensurePmtilesProtocol() {
  if (!protocol) {
    protocol = new Protocol()
    maplibregl.addProtocol('pmtiles', protocol.tile)
  }
  return protocol
}
