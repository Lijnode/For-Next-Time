<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '../../api/user'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const errorMessage = ref('')
const loading = ref(false)

/**
 * 提交密码修改请求，成功后清除本地登录状态。
 */
async function submit() {
  // 1. 清理旧错误并锁定重复提交。
  errorMessage.value = ''
  loading.value = true
  try {
    // 2. 更新密码成功后主动退出，要求使用新密码重新登录。
    await changePassword(form)
    authStore.logout()
    await router.replace({ name: 'login' })
  // 3. 仅在当前页面展示服务端返回的可读错误。
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '修改密码失败，请稍后重试'
  // 4. 无论请求结果都恢复按钮状态。
  } finally {
    loading.value = false
  }
}

/**
 * 放弃当前修改并返回个人资料页。
 */
async function cancel() {
  // 1. 返回当前用户资料页，不保存表单内容。
  await router.push({ name: 'profile' })
}
</script>

<template>
  <main class="password-page">
    <header class="page-title-bar"><button type="button" aria-label="返回" @click="cancel">‹</button><h1>修改密码</h1><span aria-hidden="true"></span></header>
    <section class="password-card">
      <p class="hint">修改成功后，需要使用新密码重新登录。</p>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="当前密码"><el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password minlength="8" maxlength="64" autocomplete="new-password" /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="form.confirmPassword" type="password" show-password minlength="8" maxlength="64" autocomplete="new-password" /></el-form-item>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <div class="actions"><el-button type="primary" native-type="submit" :loading="loading">确认修改</el-button><el-button @click="cancel">取消</el-button></div>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.password-page { width: min(640px, calc(100% - 64px)); margin: 0 auto; padding: 32px 0 72px; }.page-title-bar { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; margin-bottom: 18px; }.page-title-bar button { width: 38px; height: 38px; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); font-size: 25px; line-height: 1; }.page-title-bar h1 { margin: 0; color: var(--color-text-primary); font-size: 20px; text-align: center; }.password-card { padding: 30px; border: 1px solid var(--color-border); border-radius: 22px; background: var(--color-surface); box-shadow: 0 10px 25px rgb(31 33 33 / 4%); }.hint { margin: 0 0 25px; color: var(--color-text-secondary); font-size: 14px; }.password-card :deep(.el-form-item__label) { color: var(--color-text-primary); font-size: 13px; font-weight: 700; }.password-card :deep(.el-input__wrapper) { min-height: 48px; border: 1px solid var(--color-border); border-radius: 14px; background: #fbfbf9; box-shadow: none; }.password-card :deep(.el-input__wrapper.is-focus) { border-color: #ffb29f; box-shadow: 0 0 0 3px rgb(255 111 78 / 10%); }.error-message { color: var(--color-danger); }.actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 28px; }.actions :deep(.el-button) { min-width: 88px; min-height: 42px; border-radius: 12px; font-weight: 700; }
</style>
