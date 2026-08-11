<template>
  <div class="summary-grid">
    <div class="summary-item summary-address">
      <span class="label">最新大概位置</span>
      <strong>{{ latest ? formatApproximateAddress(latest) : '暂无有效定位' }}</strong>
    </div>
    <div class="summary-item">
      <span class="label">最新定位时间</span>
      <strong>{{ latest ? formatDateTime(latest.locatedAt) : '—' }}</strong>
    </div>
    <div class="summary-item">
      <span class="label">有效定位</span>
      <strong>{{ validCount }} 条</strong>
    </div>
    <div class="summary-item">
      <span class="label">无效定位</span>
      <strong>{{ invalidCount }} 条</strong>
    </div>
  </div>
</template>

<script setup>
import { formatDateTime } from '../../utils/format.js'
import { formatApproximateAddress } from '../../utils/location.js'

defineProps({
  latest: { type: Object, default: null },
  validCount: { type: Number, default: 0 },
  invalidCount: { type: Number, default: 0 }
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: minmax(260px, 2fr) repeat(3, minmax(130px, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.summary-item {
  display: flex;
  min-height: 66px;
  flex-direction: column;
  justify-content: center;
  padding: 12px 14px;
  border: 1px solid #e5e9f0;
  border-radius: 8px;
  background: #fafbfd;
}

.summary-item .label {
  margin-bottom: 6px;
  color: #7a8796;
  font-size: 13px;
}

.summary-item strong {
  color: #25364a;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
