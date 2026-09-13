<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const errorMessage = ref('')
const loading = ref(false)

/**
 * 提交登录表单，成功后进入首页。
 */
async function submit() {
  // 1. 清除上一次的错误并进入提交状态。
  errorMessage.value = ''
  loading.value = true
  // 2. 请求登录并在成功后替换当前路由。
  try {
    await authStore.loginByPassword(form)
    await router.replace({ name: 'home' })
  // 3. 将可读错误反馈到当前表单。
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  // 4. 无论成功或失败都恢复按钮状态。
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <p class="eyebrow">FOR NEXT TIME</p>
      <h1>下次一定</h1>
      <p class="subtitle">这次先留着，下次一定用得上。</p>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" maxlength="50" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">登录</el-button>
      </el-form>
      <p class="switch-link">还没有账号？<RouterLink to="/register">注册</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.auth-page { display: grid; min-height: 100vh; place-items: center; padding: 48px; background: var(--color-bg); }
.auth-card { width: min(440px, 100%); padding: 48px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); box-shadow: 0 20px 48px rgb(32 35 34 / 7%); }
.eyebrow { margin: 0; color: var(--color-brand); font-size: 11px; font-weight: 700; letter-spacing: .18em; }
h1 { margin: 14px 0 10px; color: var(--color-text-primary); font-size: 34px; font-weight: 700; letter-spacing: -.03em; }
.subtitle { margin: 0 0 34px; color: var(--color-text-secondary); line-height: 1.7; }
.auth-card :deep(.el-form-item) { display: block; margin-bottom: 22px; }.auth-card :deep(.el-form-item__label) { display: block; height: auto; padding: 0 0 8px; color: var(--color-text-primary); font-weight: 600; line-height: 1.4; }.auth-card :deep(.el-form-item__content) { display: block; margin-left: 0 !important; line-height: 1; }.auth-card :deep(.el-input) { width: 100%; }.auth-card :deep(.el-input__wrapper) { min-height: 46px; padding: 1px 14px; border-radius: 14px; box-shadow: 0 0 0 1px var(--color-border) inset; transition: box-shadow .2s ease; }.auth-card :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--color-brand) inset, 0 0 0 4px rgb(255 111 78 / 12%); }
.submit-button { width: 100%; min-height: 46px; margin-top: 4px; border-radius: 14px; font-weight: 600; }
.error-message { margin: -6px 0 14px; color: var(--color-danger); font-size: 13px; }
.switch-link { margin: 28px 0 0; color: var(--color-text-secondary); text-align: center; }.switch-link a { color: var(--color-brand); font-weight: 600; text-decoration: none; }
</style>
