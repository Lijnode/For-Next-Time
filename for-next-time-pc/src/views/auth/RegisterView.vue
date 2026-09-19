<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRequiredFormValidation } from '../../composables/useRequiredFormValidation'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '', confirmPassword: '' })
const { validationMessage, validateRequired } = useRequiredFormValidation()
const loading = ref(false)

/**
 * 提交注册表单，注册成功后直接进入首页。
 */
async function submit() {
  // 1. 将缺少的必填项显示在表单顶部，不打断用户继续输入。
  if (!validateRequired([
    { value: form.username, message: '请填写用户名' },
    { value: form.password, message: '请填写密码' },
    { value: form.confirmPassword, message: '请确认密码' },
  ])) return
  // 2. 对已输入的信息使用悬浮提示反馈业务错误。
  if (form.password !== form.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  // 3. 通过后端注册并保存自动签发的令牌。
  loading.value = true
  try {
    await authStore.registerAccount(form)
    await router.replace({ name: 'home' })
  // 4. 将账号或服务端错误作为悬浮提示展示。
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败，请稍后重试')
  // 5. 无论成功或失败都恢复按钮状态。
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <div class="brand-heading"><p class="eyebrow">FOR NEXT TIME</p></div>
      <h1>创建账号</h1>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <p v-if="validationMessage" class="form-validation-message" role="alert">{{ validationMessage }}</p>
        <el-form-item label="用户名"><el-input v-model="form.username" maxlength="50" autocomplete="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">注册</el-button>
      </el-form>
      <p class="switch-link">已有账号？<RouterLink to="/login">登录</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.auth-page { display: grid; box-sizing: border-box; height: 100dvh; place-items: center; overflow: hidden; padding: 32px 48px; background: var(--color-bg); }
.auth-card { width: min(440px, 100%); padding: 36px 48px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); box-shadow: 0 20px 48px rgb(32 35 34 / 7%); }
.brand-heading { display: flex; align-items: center; }.eyebrow { margin: 0; color: var(--color-brand); font-size: 11px; font-weight: 700; letter-spacing: .18em; }
h1 { margin: 16px 0 30px; color: var(--color-text-primary); font-size: 34px; font-weight: 700; letter-spacing: -.03em; }
.auth-card :deep(.el-form-item) { display: block; margin-bottom: 16px; }.auth-card :deep(.el-form-item__label) { display: block; height: auto; padding: 0 0 8px; color: var(--color-text-primary); font-weight: 600; line-height: 1.4; }.auth-card :deep(.el-form-item__content) { display: block; margin-left: 0 !important; line-height: 1; }.auth-card :deep(.el-input) { width: 100%; }.auth-card :deep(.el-input__wrapper) { min-height: 46px; padding: 1px 14px; border-radius: 14px; box-shadow: 0 0 0 1px var(--color-border) inset; transition: box-shadow .2s ease; }.auth-card :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--color-brand) inset, 0 0 0 4px rgb(255 111 78 / 12%); }
.submit-button { width: 100%; min-height: 46px; margin-top: 4px; border-radius: 14px; font-weight: 600; }
.form-validation-message { margin: 0 0 16px; padding: 10px 12px; border-radius: 12px; background: #fff1ed; color: var(--color-danger); font-size: 13px; line-height: 1.45; }
.switch-link { margin: 22px 0 0; color: var(--color-text-secondary); text-align: center; }.switch-link a { color: var(--color-brand); font-weight: 600; text-decoration: none; }
</style>
