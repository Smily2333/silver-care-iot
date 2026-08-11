<template>
  <el-collapse class="advanced-panel">
    <el-collapse-item title="高级指令（仅限了解设备协议的管理员）" name="command">
      <el-alert type="warning" :closable="false" show-icon title="手写命令只确认发送，不跟踪执行结果；错误命令可能改变设备配置。" />
      <div class="command-row">
        <el-input v-model="content" placeholder="输入协议内容，例如 CR" />
        <el-button type="danger" plain :loading="sending" @click="submit">确认并发送</el-button>
      </div>
      <div v-if="result" class="result">{{ result }}</div>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sendCommand } from '../../api/devices.js'

const props = defineProps({ deviceId: { type: [String, Number], required: true } })
const content = ref('')
const sending = ref(false)
const result = ref('')

async function submit() {
  const command = content.value.trim()
  if (!command) return
  try {
    await ElMessageBox.confirm(`确认向设备发送手写指令“${command}”？`, '高风险操作', {
      confirmButtonText: '确认发送', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  sending.value = true
  result.value = ''
  try {
    const response = await sendCommand(props.deviceId, command)
    result.value = `已写入设备连接：${response.data.packet}`
  } catch (error) {
    ElMessage.error(error.response?.data?.message ?? error.message)
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.advanced-panel { margin-top: 20px; }
.command-row { display: flex; gap: 10px; margin-top: 12px; max-width: 620px; }
.result { margin-top: 10px; color: #3b7a57; font-family: ui-monospace, monospace; word-break: break-all; }
</style>
