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
  <main class="home-shell">
    <section class="welcome-card">
      <p class="eyebrow">For Next Time</p>
      <h1>下次一定</h1>
      <p>这次先留着，下次一定用得上。</p>
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
