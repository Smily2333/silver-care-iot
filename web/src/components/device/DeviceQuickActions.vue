<template>
  <section class="quick-actions">
    <div class="section-heading">
      <h4>快捷操作</h4>
      <span>只有人工确认过的通信能力才会启用</span>
    </div>
    <el-alert
      class="wear-warning"
      type="warning"
      :closable="false"
      show-icon
      title="当前固件无法可靠识别佩戴状态；未佩戴时体温仍可能返回正常区间数值，请结合实际佩戴情况判断。"
    />
    <div class="button-row">
      <el-tooltip v-for="item in buttons" :key="item.type" :content="disabledReason(item.type)" :disabled="canRun(item.type)">
        <span>
          <el-button
            :type="item.primary ? 'primary' : 'default'"
            :disabled="!canRun(item.type) || polling"
            :loading="creatingType === item.type"
            @click="run(item.type)"
          >{{ item.label }}</el-button>
        </span>
      </el-tooltip>
      <el-button :loading="refreshing" @click="refresh">刷新数据</el-button>
    </div>
    <DeviceActionProgress :action="currentAction" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createDeviceAction, getActionCapabilities, getDeviceAction } from '../../api/deviceActions.js'
import DeviceActionProgress from './DeviceActionProgress.vue'

const props = defineProps({ deviceId: { type: [String, Number], required: true } })
const emit = defineEmits(['refresh', 'action-complete'])
const buttons = [
  { type: 'LOCATE_NOW', label: '立即定位', primary: true },
  { type: 'MEASURE_HEART_RATE', label: '测量心率' },
  { type: 'MEASURE_TEMPERATURE', label: '测量体温' },
  { type: 'MEASURE_HEALTH', label: '立即健康检测' }
]
const capabilities = ref([])
const currentAction = ref(null)
const creatingType = ref('')
const polling = ref(false)
const refreshing = ref(false)
let timer = null

const capabilityMap = computed(() => Object.fromEntries(capabilities.value.map(item => [item.type, item])))
const canRun = type => capabilityMap.value[type]?.enabled === true
const disabledReason = type => capabilityMap.value[type]?.reason || '能力检查中'

async function loadCapabilities() {
  try {
    capabilities.value = (await getActionCapabilities(props.deviceId)).data
  } catch (error) {
    capabilities.value = []
    ElMessage.error('快捷能力加载失败：' + (error.response?.data?.message ?? error.message))
  }
}

async function run(type) {
  creatingType.value = type
  try {
    currentAction.value = (await createDeviceAction(props.deviceId, type)).data
    poll()
  } catch (error) {
    ElMessage.error(error.response?.data?.message ?? error.message)
  } finally {
    creatingType.value = ''
  }
}

function terminal(status) {
  return ['COMPLETED', 'PARTIAL_SUCCESS', 'SEND_FAILED', 'ACK_TIMEOUT', 'RESULT_TIMEOUT', 'CANCELLED'].includes(status)
}

async function poll() {
  clearTimeout(timer)
  if (!currentAction.value?.id || terminal(currentAction.value.status)) {
    polling.value = false
    if (currentAction.value && terminal(currentAction.value.status)) emit('action-complete', currentAction.value)
    return
  }
  polling.value = true
  timer = setTimeout(async () => {
    try {
      currentAction.value = (await getDeviceAction(props.deviceId, currentAction.value.id)).data
      poll()
    } catch (error) {
      polling.value = false
      ElMessage.error('操作进度查询失败：' + (error.response?.data?.message ?? error.message))
    }
  }, 2000)
}

async function refresh() {
  refreshing.value = true
  try {
    emit('refresh')
    await loadCapabilities()
  } finally {
    refreshing.value = false
  }
}

watch(() => props.deviceId, loadCapabilities)
onMounted(loadCapabilities)
onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.quick-actions { margin-top: 22px; padding: 16px; border: 1px solid #e5e9f0; border-radius: 8px; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 12px; }
.section-heading h4 { margin: 0; }
.section-heading span { color: #8a96a5; font-size: 12px; }
.wear-warning { margin-bottom: 12px; }
.button-row { display: flex; flex-wrap: wrap; gap: 10px; }
</style>
