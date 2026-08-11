<template>
  <div>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      style="margin-top:12px"
    />
    <template v-else>
      <LocationSummary
        :latest="latestValid"
        :valid-count="validRecords.length"
        :invalid-count="invalidCount"
      />
      <el-alert
        v-if="invalidCount"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom:12px"
        title="无效GPS记录仍保留在列表中，但不会显示在地图或参与轨迹连线。"
      />
      <LocationMap
        :records="records"
        :selected-record-id="selectedRecordId"
        :loading="loading"
        height="440px"
        @select-record="selectRecord"
      />
      <div class="table-heading">
        <strong>最近 {{ records.length }} 条位置记录</strong>
        <span>地图使用 WGS84 坐标</span>
      </div>
      <LocationRecordTable
        :records="records"
        :selected-record-id="selectedRecordId"
        @select-record="selectRecord"
      />
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import LocationMap from '../LocationMap.vue'
import LocationRecordTable from './LocationRecordTable.vue'
import LocationSummary from './LocationSummary.vue'
import { getLatestValidLocation, getValidLocationRecords } from '../../utils/location.js'

const props = defineProps({
  records: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

const selectedRecordId = ref(null)
const validRecords = computed(() => getValidLocationRecords(props.records))
const invalidCount = computed(() => props.records.length - validRecords.value.length)
const latestValid = computed(() => getLatestValidLocation(props.records))

function selectRecord(record) {
  selectedRecordId.value = record?.id ?? null
}

watch(() => props.records, () => {
  if (!props.records.some(record => String(record.id) === String(selectedRecordId.value))) {
    selectedRecordId.value = latestValid.value?.id ?? null
  }
}, { immediate: true, deep: true })
</script>

<style scoped>
.table-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 8px;
  color: #25364a;
}

.table-heading span {
  color: #8a96a5;
  font-size: 12px;
}
</style>
