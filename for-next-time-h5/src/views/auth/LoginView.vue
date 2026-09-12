<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showError } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function submit() {
  loading.value = true
  try {
    await authStore.loginByPassword(form)
    await router.replace({ name: 'home' })
  } catch (error) {
    showError(error instanceof Error ? error.message : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <header><p>FOR NEXT TIME</p><h1>下次一定</h1><span>这次先留着，下次一定用得上。</span></header>
    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.username" name="username" label="用户名" autocomplete="username" :rules="[{ required: true, message: '请填写用户名' }]" />
        <van-field v-model="form.password" name="password" label="密码" type="password" autocomplete="current-password" :rules="[{ required: true, message: '请填写密码' }]" />
      </van-cell-group>
      <div class="submit-wrap"><van-button block round type="primary" native-type="submit" :loading="loading">登录</van-button></div>
    </van-form>
    <p class="switch-link">还没有账号？<RouterLink to="/register">注册</RouterLink></p>
  </main>
</template>

<style scoped>
.auth-page { min-height: 100vh; padding: calc(72px + env(safe-area-inset-top)) 20px 32px; }
header { margin: 0 4px 42px; } header p { margin: 0 0 12px; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .15em; } h1 { margin: 0 0 12px; font-size: 32px; } header span, .switch-link { color: var(--color-muted); } .submit-wrap { margin: 24px 16px 0; } .switch-link { margin-top: 28px; text-align: center; } .switch-link a { color: var(--color-brand); text-decoration: none; }
</style>
