<template>
  <el-alert v-if="action" :type="alertType" :closable="false" show-icon class="action-progress">
    <template #title>{{ title }}</template>
    <div class="progress-meta">
      <span>{{ statusText }}</span>
      <span v-if="action.failureReason">{{ action.failureReason }}</span>
      <span v-if="action.ackMissing">结果已收到，但未匹配到设备 ACK</span>
    </div>
  </el-alert>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ action: { type: Object, default: null } })

const typeNames = {
  LOCATE_NOW: '立即定位',
  MEASURE_HEART_RATE: '测量心率',
  MEASURE_TEMPERATURE: '测量体温',
  MEASURE_HEALTH: '健康检测'
}

const statusNames = {
  PENDING: '正在创建操作', SENT: '命令已发送，等待设备确认',
  ACKNOWLEDGED: '设备已确认，等待新数据', COMPLETED: '操作已完成',
  PARTIAL_SUCCESS: '设备返回了异常或无效测量值', SEND_FAILED: '发送失败',
  ACK_TIMEOUT: '设备确认超时', RESULT_TIMEOUT: '结果等待超时', CANCELLED: '操作已取消'
}

const title = computed(() => typeNames[props.action?.type] || '设备操作')
const statusText = computed(() => statusNames[props.action?.status] || props.action?.status || '')
const alertType = computed(() => {
  if (props.action?.status === 'COMPLETED') return 'success'
  if (['PARTIAL_SUCCESS', 'ACK_TIMEOUT', 'RESULT_TIMEOUT'].includes(props.action?.status)) return 'warning'
  if (['SEND_FAILED', 'CANCELLED'].includes(props.action?.status)) return 'error'
  return 'info'
})
</script>

<style scoped>
.action-progress { margin-top: 12px; }
.progress-meta { display: flex; flex-direction: column; gap: 3px; font-size: 13px; }
</style>
