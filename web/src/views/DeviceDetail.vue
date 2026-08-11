<template>
  <div v-loading="deviceLoading">
    <el-page-header @back="$router.push('/devices')" style="margin-bottom:16px">
      <template #content>设备详情 — {{ device?.deviceNo }}</template>
    </el-page-header>

    <el-alert
      v-if="deviceError"
      :title="deviceError"
      type="error"
      show-icon
      :closable="false"
    />

    <el-tabs v-else v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="2" border style="margin-top:12px">
          <el-descriptions-item label="设备编号">{{ device?.deviceNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ device?.ownerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ device?.model ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="device?.status === 'ONLINE' ? 'success' : 'info'">
              {{ device?.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="电量">
            {{ device?.batteryLevel != null ? `${device.batteryLevel}%` : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="每日步数">
            {{ device?.stepCount != null ? `${device.stepCount} 步` : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="设备最后在线时间">
            {{ formatDateTime(device?.lastOnlineAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <DeviceQuickActions
          v-if="device?.id"
          :device-id="device.id"
          @refresh="refreshCurrentData"
          @action-complete="handleActionComplete"
        />
        <AdvancedCommandPanel v-if="device?.id" :device-id="device.id" />
      </el-tab-pane>

      <el-tab-pane label="健康记录" name="health" lazy>
        <el-alert
          v-if="healthError"
          :title="healthError"
          type="error"
          show-icon
          :closable="false"
          style="margin-top:12px"
        />
        <div v-if="healthSummary" class="health-summary">
          <div class="metric-card">
            <span>心率</span>
            <strong>{{ healthSummary.heartRate?.value ?? '—' }} <small>bpm</small></strong>
            <em>{{ metricHint(healthSummary.heartRate) }}</em>
          </div>
          <div class="metric-card">
            <span>血压</span>
            <strong>{{ pressureValue(healthSummary.bloodPressure) }} <small>mmHg</small></strong>
            <em>{{ metricHint(healthSummary.bloodPressure) }}</em>
          </div>
          <div class="metric-card">
            <span>体温</span>
            <strong>{{ healthSummary.temperature?.value ?? '—' }} <small>℃</small></strong>
            <em>{{ metricHint(healthSummary.temperature) }}</em>
          </div>
        </div>
        <el-table
          v-else
          v-loading="healthLoading"
          :data="healthRecords"
          stripe
          empty-text="暂无健康记录"
          style="width:100%;margin-top:12px"
        >
          <el-table-column label="时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.measuredAt) }}</template>
          </el-table-column>
          <el-table-column label="心率 (bpm)" width="120">
            <template #default="{ row }">{{ row.heartRate ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="收缩压" width="90">
            <template #default="{ row }">{{ row.systolicPressure ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="舒张压" width="90">
            <template #default="{ row }">{{ row.diastolicPressure ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="体温 (℃)" width="110">
            <template #default="{ row }">{{ row.bodyTemperature ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="{ row }">{{ row.sourceCommand || '—' }}</template>
          </el-table-column>
          <el-table-column label="测量状态" min-width="150">
            <template #default="{ row }">
              {{ recordStatus(row) }}
              <span v-if="row.invalidReason" class="invalid-reason">{{ row.invalidReason }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="位置轨迹" name="location" lazy>
        <DeviceLocationTab
          :records="locationRecords"
          :loading="locationLoading"
          :error="locationError"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getDevice, getHealthRecords, getHealthSummary, getLocationRecords } from '../api/devices.js'
import DeviceLocationTab from '../components/device/DeviceLocationTab.vue'
import DeviceQuickActions from '../components/device/DeviceQuickActions.vue'
import AdvancedCommandPanel from '../components/device/AdvancedCommandPanel.vue'
import { formatDateTime } from '../utils/format.js'

const route = useRoute()
const id = route.params.id

const device = ref(null)
const healthRecords = ref([])
const healthSummary = ref(null)
const locationRecords = ref([])
const activeTab = ref('info')

const deviceLoading = ref(false)
const healthLoading = ref(false)
const locationLoading = ref(false)
const deviceError = ref('')
const healthError = ref('')
const locationError = ref('')
const healthLoaded = ref(false)
const locationLoaded = ref(false)

function errorMessage(prefix, error) {
  return `${prefix}：${error.response?.data?.message ?? error.message}`
}

async function loadDevice() {
  deviceLoading.value = true
  deviceError.value = ''
  try {
    device.value = (await getDevice(id)).data
  } catch (error) {
    deviceError.value = errorMessage('设备信息加载失败', error)
  } finally {
    deviceLoading.value = false
  }
}

async function loadHealth() {
  if (healthLoaded.value || healthLoading.value) return
  healthLoading.value = true
  healthError.value = ''
  try {
    const [recordsResponse, summaryResponse] = await Promise.all([
      getHealthRecords(id), getHealthSummary(id)
    ])
    healthRecords.value = recordsResponse.data
    healthSummary.value = summaryResponse.data
    healthLoaded.value = true
  } catch (error) {
    healthError.value = errorMessage('健康记录加载失败', error)
  } finally {
    healthLoading.value = false
  }
}

async function loadLocations() {
  if (locationLoaded.value || locationLoading.value) return
  locationLoading.value = true
  locationError.value = ''
  try {
    locationRecords.value = (await getLocationRecords(id)).data
    locationLoaded.value = true
  } catch (error) {
    locationError.value = errorMessage('位置记录加载失败', error)
  } finally {
    locationLoading.value = false
  }
}

async function refreshCurrentData() {
  await loadDevice()
  healthLoaded.value = false
  locationLoaded.value = false
  if (activeTab.value === 'health') await loadHealth()
  if (activeTab.value === 'location') await loadLocations()
}

async function handleActionComplete(action) {
  if (action.type === 'LOCATE_NOW') {
    locationLoaded.value = false
    if (activeTab.value === 'location') await loadLocations()
  } else {
    healthLoaded.value = false
    if (activeTab.value === 'health') await loadHealth()
  }
}

function pressureValue(metric) {
  return metric ? `${metric.systolic ?? '—'}/${metric.diastolic ?? '—'}` : '—'
}

function metricHint(metric) {
  if (!metric) return '暂无有效数据'
  const freshness = metric.freshness === 'STALE' ? '数据已过期' : formatDateTime(metric.measuredAt)
  const status = metric.status && metric.status !== 'VALID' ? ` · ${statusText(metric.status)}` : ''
  return freshness + status
}

function statusText(status) {
  return ({ VALID: '有效', TOO_LOW: '超出显示范围（低）', TOO_HIGH: '超出显示范围（高）', INVALID: '无效' })[status] || '状态未知'
}

function recordStatus(row) {
  const statuses = [row.heartRateStatus, row.bloodPressureStatus, row.temperatureStatus].filter(Boolean)
  if (!statuses.length) return '历史数据（未分类）'
  return [...new Set(statuses.map(statusText))].join('、')
}

watch(activeTab, tab => {
  if (tab === 'health') loadHealth()
  if (tab === 'location') loadLocations()
})

onMounted(loadDevice)
</script>

<style scoped>
.health-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}
.metric-card { display: flex; flex-direction: column; gap: 5px; padding: 14px; border: 1px solid #e5e9f0; border-radius: 8px; background: #fafbfd; }
.metric-card span, .metric-card em { color: #7a8796; font-size: 13px; font-style: normal; }
.metric-card strong { color: #25364a; font-size: 22px; }
.metric-card small { font-size: 12px; font-weight: normal; }
.invalid-reason { display: block; color: #b26a00; font-size: 12px; }
@media (max-width: 760px) { .health-summary { grid-template-columns: 1fr; } }
</style>
