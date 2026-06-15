<template>
  <div v-loading="loading">
    <el-page-header @back="$router.push('/devices')" style="margin-bottom:16px">
      <template #content>设备详情 — {{ device?.deviceNo }}</template>
    </el-page-header>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="2" border style="margin-top:12px">
          <el-descriptions-item label="设备编号">{{ device?.deviceNo }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ device?.model ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="device?.status === 'ONLINE' ? 'success' : 'info'">
              {{ device?.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="电量">
            {{ device?.batteryLevel != null ? device.batteryLevel + '%' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="步数">{{ device?.stepCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后心跳">
            {{ device?.lastHeartbeatAt ? new Date(device.lastHeartbeatAt).toLocaleString('zh-CN') : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top:24px">
          <h4 style="margin-bottom:8px">发送指令</h4>
          <el-input
            v-model="commandContent"
            placeholder="输入指令内容，例如：CR"
            style="width:300px;margin-right:8px"
          />
          <el-button type="primary" @click="sendCmd" :loading="sending">发送</el-button>
          <div v-if="cmdResult" style="margin-top:8px;color:#67c23a;font-family:monospace">
            {{ cmdResult }}
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="健康记录" name="health" lazy>
        <el-table :data="healthRecords" stripe style="width:100%;margin-top:12px">
          <el-table-column label="时间" min-width="180">
            <template #default="{ row }">
              {{ new Date(row.measuredAt).toLocaleString('zh-CN') }}
            </template>
          </el-table-column>
          <el-table-column label="心率(bpm)" width="110">
            <template #default="{ row }">{{ row.heartRate ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="收缩压" width="90">
            <template #default="{ row }">{{ row.systolicPressure ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="舒张压" width="90">
            <template #default="{ row }">{{ row.diastolicPressure ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="体温(℃)" width="100">
            <template #default="{ row }">{{ row.bodyTemperature ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="{ row }">{{ row.sourceCommand }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="位置轨迹" name="location" lazy>
        <LocationMap :records="locationRecords" height="420px" style="margin-top:12px" />
        <el-table :data="locationRecords" stripe style="width:100%;margin-top:12px">
          <el-table-column label="时间" min-width="180">
            <template #default="{ row }">
              {{ new Date(row.locatedAt).toLocaleString('zh-CN') }}
            </template>
          </el-table-column>
          <el-table-column label="纬度" width="120">
            <template #default="{ row }">{{ row.latitude ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="经度" width="120">
            <template #default="{ row }">{{ row.longitude ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="GPS有效" width="90">
            <template #default="{ row }">
              <el-tag :type="row.gpsValid ? 'success' : 'warning'" size="small">
                {{ row.gpsValid ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="速度(节)" width="90">
            <template #default="{ row }">{{ row.speed ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="电量" width="80">
            <template #default="{ row }">
              {{ row.batteryLevel != null ? row.batteryLevel + '%' : '-' }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDevice, getHealthRecords, getLocationRecords, sendCommand } from '../api/devices.js'
import LocationMap from '../components/LocationMap.vue'

const route = useRoute()
const id = route.params.id

const loading = ref(false)
const device = ref(null)
const healthRecords = ref([])
const locationRecords = ref([])
const activeTab = ref('info')
const commandContent = ref('')
const cmdResult = ref('')
const sending = ref(false)

async function load() {
  loading.value = true
  try {
    const [devRes, healthRes, locRes] = await Promise.all([
      getDevice(id),
      getHealthRecords(id),
      getLocationRecords(id)
    ])
    device.value = devRes.data
    healthRecords.value = healthRes.data
    locationRecords.value = locRes.data
  } catch (e) {
    ElMessage.error('加载失败：' + (e.response?.data?.message ?? e.message))
  } finally {
    loading.value = false
  }
}

async function sendCmd() {
  if (!commandContent.value.trim()) return
  sending.value = true
  cmdResult.value = ''
  try {
    const res = await sendCommand(id, commandContent.value.trim())
    cmdResult.value = '已发送：' + res.data.packet
  } catch (e) {
    ElMessage.error('发送失败：' + (e.response?.data?.message ?? e.message))
  } finally {
    sending.value = false
  }
}

onMounted(load)
</script>
