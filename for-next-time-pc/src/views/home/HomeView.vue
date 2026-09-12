<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

/**
 * 清除登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与本地存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护页面。
  await router.replace({ name: 'login' })
}

/**
 * 进入新增内容页面。
 */
async function createContent() {
  // 1. 进入受保护的内容编辑页。
  await router.push({ name: 'content-create' })
}
</script>

<template>
  <main class="home-shell">
    <section class="welcome-card">
      <p class="eyebrow">For Next Time</p>
      <h1>下次一定</h1>
      <p>这次先留着，下次一定用得上。</p>
      <el-button type="primary" @click="createContent">先留一句</el-button>
      <el-button text type="primary" @click="logout">退出登录</el-button>
    </section>
  </main>
</template>

<style scoped>
.home-shell {
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  padding: 80px 0;
}

.welcome-card {
  padding: 48px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 18px rgb(20 24 24 / 4%);
}

.eyebrow {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

h1 {
  margin: var(--space-2) 0;
  font-size: 32px;
}

p {
  color: var(--color-text-secondary);
}
</style>
