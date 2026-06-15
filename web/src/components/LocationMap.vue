<template>
  <div ref="mapEl" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const props = defineProps({
  records: { type: Array, default: () => [] },
  height: { type: String, default: '400px' }
})

const mapEl = ref(null)
let map = null
let markerLayer = null
let polylineLayer = null

function initMap() {
  if (!window.TMap || !mapEl.value) return

  const center = props.records.length > 0
    ? new window.TMap.LatLng(Number(props.records[0].latitude), Number(props.records[0].longitude))
    : new window.TMap.LatLng(39.984120, 116.307484)

  map = new window.TMap.Map(mapEl.value, { zoom: 15, center })

  markerLayer = new window.TMap.MultiMarker({
    map,
    styles: {
      current: new window.TMap.MarkerStyle({ width: 24, height: 34, anchor: { x: 12, y: 34 } })
    },
    geometries: []
  })

  polylineLayer = new window.TMap.MultiPolyline({
    map,
    styles: {
      track: new window.TMap.PolylineStyle({
        color: '#3399FF', width: 4, borderWidth: 2, borderColor: '#FFFFFF', lineCap: 'round'
      })
    },
    geometries: []
  })

  renderRecords()
}

function renderRecords() {
  if (!map || !props.records.length) return

  const points = props.records
    .filter(r => r.latitude != null && r.longitude != null)
    .map(r => new window.TMap.LatLng(Number(r.latitude), Number(r.longitude)))

  if (!points.length) return

  markerLayer.setGeometries([{ id: 'current', styleId: 'current', position: points[0] }])
  // records are newest-first; reverse for chronological polyline
  polylineLayer.setGeometries([{ id: 'track', styleId: 'track', paths: [...points].reverse() }])
  map.setCenter(points[0])
}

onMounted(() => {
  if (window.TMap) {
    initMap()
  } else {
    const timer = setInterval(() => {
      if (window.TMap) { clearInterval(timer); initMap() }
    }, 200)
  }
})

watch(() => props.records, renderRecords, { deep: true })
</script>
