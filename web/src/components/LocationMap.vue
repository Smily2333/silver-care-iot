<template>
  <div class="location-map-wrap">
    <div ref="mapEl" class="location-map" :style="{ height }"></div>
    <div v-if="!validRecords.length" class="map-empty">暂无有效位置数据</div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  records: { type: Array, default: () => [] },
  height: { type: String, default: '400px' }
})

const DEFAULT_CENTER = [39.984120, 116.307484]
const TILE_URL = import.meta.env.VITE_MAP_TILE_URL || 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
const TILE_ATTRIBUTION = import.meta.env.VITE_MAP_TILE_ATTRIBUTION || '&copy; OpenStreetMap contributors'

const mapEl = ref(null)
let map = null
let currentMarker = null
let trackLine = null

const validRecords = computed(() =>
  props.records
    .map(record => ({
      ...record,
      displayLatitude: Number(record.latitude),
      displayLongitude: Number(record.longitude)
    }))
    .filter(record => Number.isFinite(record.displayLatitude) && Number.isFinite(record.displayLongitude))
)

function initMap() {
  if (!mapEl.value || map) return

  map = L.map(mapEl.value, {
    center: DEFAULT_CENTER,
    zoom: 15,
    zoomControl: true,
    attributionControl: true
  })

  L.tileLayer(TILE_URL, {
    maxZoom: 19,
    attribution: TILE_ATTRIBUTION
  }).addTo(map)

  renderRecords()
  nextTick(() => map?.invalidateSize())
}

function renderRecords() {
  if (!map) return

  if (currentMarker) {
    currentMarker.remove()
    currentMarker = null
  }
  if (trackLine) {
    trackLine.remove()
    trackLine = null
  }

  const points = validRecords.value.map(record => [record.displayLatitude, record.displayLongitude])
  if (!points.length) {
    map.setView(DEFAULT_CENTER, 15)
    return
  }

  currentMarker = L.circleMarker(points[0], {
    radius: 8,
    color: '#ffffff',
    weight: 3,
    fillColor: '#1677ff',
    fillOpacity: 1
  }).addTo(map)

  const trackPoints = [...points].reverse()
  trackLine = L.polyline(trackPoints, {
    color: '#1677ff',
    weight: 4,
    opacity: 0.85
  }).addTo(map)

  if (trackPoints.length > 1) {
    map.fitBounds(trackLine.getBounds(), { padding: [24, 24], maxZoom: 17 })
  } else {
    map.setView(points[0], 16)
  }
}

onMounted(initMap)

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})

watch(validRecords, renderRecords, { deep: true })
</script>

<style scoped>
.location-map-wrap {
  position: relative;
  width: 100%;
}

.location-map {
  width: 100%;
  min-height: 320px;
  border-radius: 6px;
  overflow: hidden;
  background: #eef2f6;
}

.map-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a9bb0;
  font-size: 14px;
  pointer-events: none;
  background: rgba(255,255,255,0.65);
}
</style>
