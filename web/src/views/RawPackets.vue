<template>
  <div>
    <h2 style="margin-bottom:16px">原始报文</h2>
    <el-table :data="packets" v-loading="loading" stripe style="width:100%">
      <el-table-column label="时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.receivedAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column prop="deviceNo" label="设备编号" width="140" />
      <el-table-column label="方向" width="90">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'UPLINK' ? '' : 'warning'" size="small">
            {{ row.direction === 'UPLINK' ? '上行' : '下行' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="command" label="命令" width="120" />
      <el-table-column label="解析状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.parseStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
            {{ row.parseStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <code style="font-size:12px">{{ row.content ?? row.rawPacket }}</code>
        </template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span style="color:#f56c6c">{{ row.errorMessage }}</span>
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
import { listPackets } from '../api/packets.js'

const packets = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(0)
const pageSize = 20

async function load(page = 0) {
  loading.value = true
  try {
    const res = await listPackets(page, pageSize)
    packets.value = res.data.content
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
