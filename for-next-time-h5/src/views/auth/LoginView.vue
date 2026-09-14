<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showError } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

/**
 * 提交登录表单，成功后进入首页。
 */
async function submit() {
  // 1. 锁定提交按钮，防止重复请求。
  loading.value = true
  try {
    await authStore.loginByPassword(form)
    await router.replace({ name: 'home' })
  // 2. 将认证失败原因通过平台反馈层展示。
  } catch (error) {
    showError(error instanceof Error ? error.message : '登录失败，请稍后重试')
  // 3. 无论成功或失败都恢复按钮状态。
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <header><p>FOR NEXT TIME</p><h1>下次一定</h1><span>这次先留着，下次一定用得上</span></header>
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
.auth-page { min-height: 100vh; padding: calc(80px + env(safe-area-inset-top)) 20px 40px; background: var(--color-page); }
header { margin: 0 8px 38px; } header p { margin: 0 0 14px; color: var(--color-brand); font-size: 11px; font-weight: 700; letter-spacing: .18em; } h1 { margin: 0 0 12px; color: var(--color-text); font-size: 34px; letter-spacing: -.04em; } header span, .switch-link { color: var(--color-muted); line-height: 1.7; }
:deep(.van-cell-group--inset) { margin: 0; overflow: hidden; border: 1px solid var(--color-border); border-radius: var(--radius-card); box-shadow: 0 10px 24px rgb(48 39 32 / 4%); }
:deep(.van-field) { display: block; min-height: 0; padding: 15px 16px 16px; border-bottom: 1px solid #efefeb; }
:deep(.van-field:last-child) { border-bottom: 0; }
:deep(.van-field__label) { width: auto; margin: 0 0 8px; color: var(--color-text); font-weight: 600; line-height: 1.4; }
:deep(.van-field__value) { overflow: visible; }
:deep(.van-field__body) { min-height: 46px; padding: 0 13px; border: 1px solid var(--color-border); border-radius: 14px; background: #fff; transition: border-color .2s ease, box-shadow .2s ease; }
:deep(.van-field:focus-within .van-field__body) { border-color: var(--color-brand); box-shadow: 0 0 0 4px rgb(255 111 78 / 12%); }
:deep(.van-field__control) { min-height: 44px; color: var(--color-text); font-size: 16px; }
.submit-wrap { margin: 24px 0 0; }
.submit-wrap :deep(.van-button) { height: 50px; font-weight: 600; }
.switch-link { margin-top: 28px; text-align: center; }
.switch-link a { color: var(--color-brand); font-weight: 600; text-decoration: none; }
</style>
