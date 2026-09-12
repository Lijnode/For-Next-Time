<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function logout() {
  authStore.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <main class="home-page">
    <p class="eyebrow">FOR NEXT TIME</p>
    <h1>下次一定</h1>
    <p class="description">先留下，找得到，用得上。</p>
    <van-button plain type="primary" @click="logout">退出登录</van-button>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 96px 24px;
  background: var(--color-page);
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

h1 {
  margin: 0;
  font-size: 32px;
  letter-spacing: 0.04em;
}

.description {
  margin-top: 16px;
  color: var(--color-muted);
}
</style>
