<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showError } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '', confirmPassword: '' })
const loading = ref(false)

/**
 * 提交注册表单，成功后直接进入首页。
 */
async function submit() {
  // 1. 先在前端校验两次密码是否一致。
  if (form.password !== form.confirmPassword) {
    showError('两次输入的密码不一致')
    return
  }
  // 2. 通过后端注册并保存自动签发的令牌。
  loading.value = true
  try {
    await authStore.registerAccount(form)
    await router.replace({ name: 'home' })
  // 3. 将注册失败原因通过平台反馈层展示。
  } catch (error) {
    showError(error instanceof Error ? error.message : '注册失败，请稍后重试')
  // 4. 无论成功或失败都恢复按钮状态。
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <header><p>FOR NEXT TIME</p><h1>创建账号</h1><span>留住那些下次一定会用上的句子。</span></header>
    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.username" name="username" label="用户名" autocomplete="username" :rules="[{ required: true, message: '请填写用户名' }]" />
        <van-field v-model="form.password" name="password" label="密码" type="password" autocomplete="new-password" :rules="[{ required: true, message: '请填写密码' }]" />
        <van-field v-model="form.confirmPassword" name="confirmPassword" label="确认密码" type="password" autocomplete="new-password" :rules="[{ required: true, message: '请确认密码' }]" />
      </van-cell-group>
      <div class="submit-wrap"><van-button block round type="primary" native-type="submit" :loading="loading">注册</van-button></div>
    </van-form>
    <p class="switch-link">已有账号？<RouterLink to="/login">登录</RouterLink></p>
  </main>
</template>

<style scoped>
.auth-page { min-height: 100vh; padding: calc(72px + env(safe-area-inset-top)) 20px 32px; } header { margin: 0 4px 42px; } header p { margin: 0 0 12px; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .15em; } h1 { margin: 0 0 12px; font-size: 32px; } header span, .switch-link { color: var(--color-muted); } .submit-wrap { margin: 24px 16px 0; } .switch-link { margin-top: 28px; text-align: center; } .switch-link a { color: var(--color-brand); text-decoration: none; }
</style>
