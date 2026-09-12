<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '', confirmPassword: '' })
const errorMessage = ref('')
const loading = ref(false)

/**
 * 提交注册表单，注册成功后直接进入首页。
 */
async function submit() {
  // 1. 清除旧错误并在前端先校验两次密码。
  errorMessage.value = ''
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  // 2. 通过后端注册并保存自动签发的令牌。
  loading.value = true
  try {
    await authStore.registerAccount(form)
    await router.replace({ name: 'home' })
  // 3. 将可读错误反馈到当前表单。
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败，请稍后重试'
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
      <h1>创建账号</h1>
      <p class="subtitle">留住那些下次一定会用上的句子。</p>
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item label="用户名"><el-input v-model="form.username" maxlength="50" autocomplete="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">注册</el-button>
      </el-form>
      <p class="switch-link">已有账号？<RouterLink to="/login">登录</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.auth-page { display: grid; min-height: 100vh; place-items: center; padding: 48px; }
.auth-card { width: 420px; padding: 44px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); box-shadow: 0 16px 36px rgb(32 35 34 / 7%); }
.eyebrow { margin: 0; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
h1 { margin: 12px 0 8px; font-size: 30px; }
.subtitle { margin: 0 0 28px; color: var(--color-text-secondary); }
.submit-button { width: 100%; margin-top: 4px; }
.error-message { margin: -4px 0 12px; color: var(--color-danger); font-size: 13px; }
.switch-link { margin: 24px 0 0; color: var(--color-text-secondary); text-align: center; }
.switch-link a { color: var(--color-brand); text-decoration: none; }
</style>
