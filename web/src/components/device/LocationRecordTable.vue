<template>
  <el-table
    :data="records"
    stripe
    highlight-current-row
    row-key="id"
    :current-row-key="selectedRecordId"
    empty-text="暂无位置记录"
    style="width:100%"
    @row-click="row => emit('select-record', row)"
  >
    <el-table-column label="时间" min-width="170">
      <template #default="{ row }">{{ formatDateTime(row.locatedAt) }}</template>
    </el-table-column>
    <el-table-column label="大概位置" min-width="250">
      <template #default="{ row }">
        <span v-if="row.gpsValid">{{ formatApproximateAddress(row) }}</span>
        <span v-else class="muted">定位无效，未解析地址</span>
      </template>
    </el-table-column>
    <el-table-column label="GPS状态" width="110">
      <template #default="{ row }">
        <el-tag v-if="row.gpsValid" type="success" size="small">有效</el-tag>
        <el-tooltip
          v-else
          content="GPS定位无效，可能位于室内、被建筑遮挡或信号较弱；该记录不参与轨迹绘制。"
          placement="top"
        >
          <el-tag type="warning" size="small">无效</el-tag>
        </el-tooltip>
      </template>
    </el-table-column>
    <el-table-column label="速度" width="90">
      <template #default="{ row }">{{ row.speed != null ? `${row.speed} 节` : '—' }}</template>
    </el-table-column>
    <el-table-column label="电量" width="80">
      <template #default="{ row }">
        {{ row.batteryLevel != null ? `${row.batteryLevel}%` : '—' }}
      </template>
    </el-table-column>
    <el-table-column label="经纬度" min-width="210">
      <template #default="{ row }">
        <span v-if="hasCoordinates(row)" class="coordinates">
          {{ formatCoordinate(row.latitude) }}, {{ formatCoordinate(row.longitude) }}
        </span>
        <span v-else>—</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { formatDateTime } from '../../utils/format.js'
import { formatApproximateAddress } from '../../utils/location.js'

defineProps({
  records: { type: Array, default: () => [] },
  selectedRecordId: { type: [String, Number], default: null }
})

const emit = defineEmits(['select-record'])

function hasCoordinates(row) {
  return Number.isFinite(Number(row?.latitude)) && Number.isFinite(Number(row?.longitude))
}

function formatCoordinate(value) {
  return Number(value).toFixed(6)
}
</script>

<style scoped>
.muted,
.coordinates {
  color: #7a8796;
}

.coordinates {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
</style>
