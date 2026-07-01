<template>
  <div>
    <h2 style="margin-bottom:16px">设备列表</h2>
    <el-table :data="devices" v-loading="loading" stripe style="width:100%">
      <el-table-column prop="deviceNo" label="设备编号" width="160" />
      <el-table-column label="姓名" width="120">
        <template #default="{ row }">
          {{ row.ownerName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="model" label="型号" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'">
            {{ row.status === 'ONLINE' ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="电量" width="100">
        <template #default="{ row }">
          {{ row.batteryLevel != null ? row.batteryLevel + '%' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="步数" width="100">
        <template #default="{ row }">{{ row.stepCount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="最后心跳" min-width="180">
        <template #default="{ row }">
          {{ row.lastHeartbeatAt ? new Date(row.lastHeartbeatAt).toLocaleString('zh-CN') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="$router.push(`/devices/${row.id}`)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top:16px"
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage + 1"
      @current-change="onPageChange"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listDevices } from '../api/devices.js'

const devices = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(0)
const pageSize = 20

async function load(page = 0) {
  loading.value = true
  try {
    const res = await listDevices(page, pageSize)
    devices.value = res.data.content
    total.value = res.data.totalElements
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  load(page - 1)
}

onMounted(() => load())
</script>
