<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="brand">Silver Care</div>
      <h1>管理端登录</h1>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input
            v-model="username"
            autocomplete="username"
            placeholder="管理员账号"
            size="large"
            autofocus
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="管理员密码"
            size="large"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" />
        <el-button
          class="login-button"
          type="primary"
          size="large"
          native-type="submit"
          :loading="loading"
          :disabled="!username.trim() || !password"
        >
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listDevices } from '../api/devices.js'
import { clearAdminCredentials, setAdminCredentials } from '../api/axios.js'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  if (!username.value.trim() || !password.value || loading.value) return
  loading.value = true
  errorMessage.value = ''
  setAdminCredentials(username.value.trim(), password.value)
  try {
    await listDevices(0, 1)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/devices'
    await router.replace(redirect.startsWith('/') ? redirect : '/devices')
  } catch (error) {
    clearAdminCredentials()
    errorMessage.value = error.response?.status === 401 ? '账号或密码错误' : '暂时无法连接服务器'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f3f6f9;
}

.login-panel {
  width: min(100%, 380px);
  padding: 32px;
  border: 1px solid #dfe6ee;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(34, 51, 68, 0.08);
}

.brand {
  color: #1677ff;
  font-size: 14px;
  font-weight: 700;
}

h1 {
  margin: 8px 0 24px;
  color: #1f2d3d;
  font-size: 24px;
}

.login-button {
  width: 100%;
  margin-top: 18px;
}
</style>
