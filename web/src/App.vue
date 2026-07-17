<template>
  <router-view v-if="$route.meta.public" />
  <el-container v-else style="height: 100vh">
    <el-aside width="200px" style="background:#304156">
      <div style="color:#fff;padding:20px;font-size:16px;font-weight:bold">
        Silver Care
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/devices">设备管理</el-menu-item>
        <el-menu-item index="/packets">原始报文</el-menu-item>
      </el-menu>
      <el-button class="logout-button" text @click="logout">退出登录</el-button>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearAdminCredentials } from './api/axios.js'

const router = useRouter()

function logout() {
  clearAdminCredentials()
  router.replace('/login')
}
</script>

<style scoped>
.logout-button {
  position: absolute;
  bottom: 18px;
  left: 18px;
  color: #bfcbd9;
}
</style>
