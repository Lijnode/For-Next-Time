<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '../../api/user'
import { showError, showSuccess } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)

/**
 * 提交密码修改请求，成功后清除本地登录状态。
 */
async function submit() {
  // 1. 锁定提交按钮，防止重复请求。
  loading.value = true
  try {
    // 2. 更新密码成功后主动退出，要求使用新密码重新登录。
    await changePassword(form)
    authStore.logout()
    showSuccess('密码已修改，请重新登录')
    await router.replace({ name: 'login' })
  // 3. 通过平台反馈层展示服务端可读错误。
  } catch (error) {
    showError(error instanceof Error ? error.message : '修改密码失败，请稍后重试')
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
    <p class="eyebrow">ACCOUNT SECURITY</p>
    <h1>修改密码</h1>
    <p class="hint">修改成功后需要使用新密码重新登录。</p>
    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.oldPassword" name="oldPassword" label="当前密码" type="password" autocomplete="current-password" :rules="[{ required: true, message: '请填写当前密码' }]" />
        <van-field v-model="form.newPassword" name="newPassword" label="新密码" type="password" autocomplete="new-password" :rules="[{ required: true, message: '请填写新密码' }]" />
        <van-field v-model="form.confirmPassword" name="confirmPassword" label="确认新密码" type="password" autocomplete="new-password" :rules="[{ required: true, message: '请确认新密码' }]" />
      </van-cell-group>
      <div class="actions"><van-button block round type="primary" native-type="submit" :loading="loading">确认修改</van-button><van-button block plain round type="primary" @click="cancel">取消</van-button></div>
    </van-form>
  </main>
</template>

<style scoped>
.password-page { min-height: 100vh; padding: calc(72px + env(safe-area-inset-top)) 20px 32px; }.eyebrow { margin: 0 4px 12px; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .15em; }h1 { margin: 0 4px 12px; font-size: 32px; }.hint { margin: 0 4px 32px; color: var(--color-muted); }.actions { display: grid; gap: 12px; margin: 28px 16px 0; }
</style>
