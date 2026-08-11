<template>
  <div class="location-map-wrap" :style="{ height }">
    <div ref="mapEl" class="location-map"></div>
    <div v-if="loading" class="map-overlay">地图加载中…</div>
    <div v-else-if="mapError" class="map-overlay map-error">
      <span>地图加载失败，请检查地图资源是否已部署。</span>
      <el-button size="small" type="primary" @click="retryMap">重新加载</el-button>
    </div>
    <div v-else-if="mapLoaded && !validRecords.length" class="map-overlay map-empty">
      暂无有效定位数据
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { mapConfig } from '../map/config.js'
import { ensurePmtilesProtocol } from '../map/pmtilesProtocol.js'
import { createMapStyle } from '../map/style.js'
import {
  buildPointGeoJson,
  buildTrackGeoJson,
  getValidLocationRecords,
  sortLocationsAscending
} from '../utils/location.js'
import { formatDateTime } from '../utils/format.js'

const TRACK_SOURCE = 'device-track'
const POINT_SOURCE = 'device-points'

const props = defineProps({
  records: { type: Array, default: () => [] },
  selectedRecordId: { type: [String, Number], default: null },
  height: { type: String, default: '420px' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['select-record', 'map-error'])
const mapEl = ref(null)
const mapError = ref('')
const mapLoaded = ref(false)
const validRecords = computed(() => getValidLocationRecords(props.records))

let map = null
let popup = null
let resizeObserver = null

function addDataLayers() {
  map.addSource(TRACK_SOURCE, { type: 'geojson', data: buildTrackGeoJson(props.records) })
  map.addLayer({
    id: 'device-track-line',
    type: 'line',
    source: TRACK_SOURCE,
    paint: {
      'line-color': '#1677ff',
      'line-width': 4,
      'line-opacity': 0.82
    }
  })

  map.addSource(POINT_SOURCE, { type: 'geojson', data: buildPointGeoJson(props.records) })
  map.addLayer({
    id: 'device-history-points',
    type: 'circle',
    source: POINT_SOURCE,
    paint: {
      'circle-radius': 5,
      'circle-color': '#409eff',
      'circle-stroke-color': '#ffffff',
      'circle-stroke-width': 2
    }
  })
  map.addLayer({
    id: 'device-latest-point',
    type: 'circle',
    source: POINT_SOURCE,
    filter: ['==', ['get', 'isLatest'], true],
    paint: {
      'circle-radius': 9,
      'circle-color': '#1677ff',
      'circle-stroke-color': '#ffffff',
      'circle-stroke-width': 3
    }
  })
  map.addLayer({
    id: 'device-selected-point',
    type: 'circle',
    source: POINT_SOURCE,
    filter: ['==', ['get', 'recordId'], String(props.selectedRecordId ?? '')],
    paint: {
      'circle-radius': 12,
      'circle-color': 'rgba(0,0,0,0)',
      'circle-stroke-color': '#ff7a00',
      'circle-stroke-width': 3
    }
  })

  map.on('click', 'device-history-points', handlePointClick)
  map.on('mouseenter', 'device-history-points', setPointerCursor)
  map.on('mouseleave', 'device-history-points', clearPointerCursor)
}

function setPointerCursor() {
  if (map) map.getCanvas().style.cursor = 'pointer'
}

function clearPointerCursor() {
  if (map) map.getCanvas().style.cursor = ''
}

function popupContent(properties) {
  const container = document.createElement('div')
  container.className = 'location-popup'
  const address = document.createElement('strong')
  address.textContent = properties.approximateAddress
    ? `约在 ${properties.approximateAddress}`
    : '暂无地址信息'
  const time = document.createElement('div')
  time.textContent = formatDateTime(properties.locatedAt)
  const details = document.createElement('div')
  const speed = properties.speed === '' ? '—' : `${properties.speed} 节`
  const battery = properties.batteryLevel === '' ? '—' : `${properties.batteryLevel}%`
  details.textContent = `速度：${speed}　电量：${battery}`
  container.append(address, time, details)
  return container
}

function handlePointClick(event) {
  const feature = event.features?.[0]
  if (!feature) return
  const recordId = feature.properties?.recordId
  const record = validRecords.value.find(item => String(item.id ?? '') === String(recordId))
  if (record) emit('select-record', record)

  popup?.remove()
  popup = new maplibregl.Popup({ offset: 14, closeButton: true })
    .setLngLat(feature.geometry.coordinates)
    .setDOMContent(popupContent(feature.properties || {}))
    .addTo(map)
}

function fitToRecords() {
  if (!map || !mapLoaded.value) return
  const sorted = sortLocationsAscending(validRecords.value)
  if (!sorted.length) {
    map.jumpTo({ center: mapConfig.defaultCenter, zoom: mapConfig.defaultZoom })
    return
  }
  if (sorted.length === 1) {
    map.easeTo({
      center: [sorted[0].displayLongitude, sorted[0].displayLatitude],
      zoom: 16,
      duration: 0
    })
    return
  }
  const bounds = new maplibregl.LngLatBounds()
  sorted.forEach(record => bounds.extend([record.displayLongitude, record.displayLatitude]))
  map.fitBounds(bounds, { padding: 36, maxZoom: 17, duration: 0 })
}

function updateData() {
  if (!map || !mapLoaded.value) return
  map.getSource(TRACK_SOURCE)?.setData(buildTrackGeoJson(props.records))
  map.getSource(POINT_SOURCE)?.setData(buildPointGeoJson(props.records))
  fitToRecords()
}

function updateSelection() {
  if (!map || !mapLoaded.value || !map.getLayer('device-selected-point')) return
  map.setFilter('device-selected-point', [
    '==', ['get', 'recordId'], String(props.selectedRecordId ?? '')
  ])
  const selected = validRecords.value.find(
    record => String(record.id ?? '') === String(props.selectedRecordId ?? '')
  )
  if (selected) {
    map.easeTo({ center: [selected.displayLongitude, selected.displayLatitude], duration: 300 })
  }
}

function initMap() {
  if (!mapEl.value || map) return
  mapError.value = ''
  mapLoaded.value = false
  try {
    ensurePmtilesProtocol()
    map = new maplibregl.Map({
      container: mapEl.value,
      style: createMapStyle(),
      center: mapConfig.defaultCenter,
      zoom: mapConfig.defaultZoom,
      maxZoom: mapConfig.maxZoom,
      localIdeographFontFamily: 'Noto Sans CJK SC, Microsoft YaHei, sans-serif',
      attributionControl: false
    })
    map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-left')
    map.addControl(new maplibregl.AttributionControl({ compact: true }), 'bottom-right')
    map.on('load', () => {
      addDataLayers()
      mapLoaded.value = true
      mapError.value = ''
      fitToRecords()
    })
    map.on('error', event => {
      const message = event?.error?.message || '地图资源加载失败'
      mapError.value = message
      emit('map-error', message)
    })
    resizeObserver = new ResizeObserver(() => map?.resize())
    resizeObserver.observe(mapEl.value)
    nextTick(() => map?.resize())
  } catch (error) {
    mapError.value = error.message || '地图初始化失败'
    emit('map-error', mapError.value)
  }
}

function destroyMap() {
  popup?.remove()
  popup = null
  resizeObserver?.disconnect()
  resizeObserver = null
  map?.remove()
  map = null
  mapLoaded.value = false
}

function retryMap() {
  destroyMap()
  nextTick(initMap)
}

onMounted(initMap)
onBeforeUnmount(destroyMap)

watch(() => props.records, updateData, { deep: true })
watch(() => props.selectedRecordId, updateSelection)
</script>

<style scoped>
.location-map-wrap {
  position: relative;
  width: 100%;
  min-height: 320px;
  overflow: hidden;
  border-radius: 8px;
  background: #eef2f6;
}

.location-map {
  width: 100%;
  height: 100%;
}

.map-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #60758a;
  background: rgba(247, 249, 252, 0.82);
}

.map-error {
  flex-direction: column;
  color: #b42318;
}

.map-empty {
  pointer-events: none;
}

:deep(.maplibregl-ctrl-attrib) {
  font-size: 11px;
}

:deep(.location-popup) {
  min-width: 190px;
  line-height: 1.7;
}

:deep(.location-popup strong) {
  display: block;
  margin-bottom: 2px;
}
</style>
