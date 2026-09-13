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
    <section class="password-card">
      <p class="eyebrow">ACCOUNT SECURITY</p>
      <h1>修改密码</h1>
      <p class="hint">修改成功后需要使用新密码重新登录。</p>
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
.password-page { width: min(640px, calc(100% - 64px)); margin: 0 auto; padding: 96px 0; }
.password-card { padding: 44px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); box-shadow: 0 16px 36px rgb(32 35 34 / 7%); }
.eyebrow { margin: 0; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
h1 { margin: 12px 0 8px; font-size: 30px; }.hint { margin: 0 0 28px; color: var(--color-text-secondary); }.error-message { color: var(--color-danger); }.actions { display: flex; gap: 12px; margin-top: 28px; }
</style>
