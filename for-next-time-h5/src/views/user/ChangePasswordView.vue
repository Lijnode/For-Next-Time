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
    <header><button type="button" aria-label="返回" @click="cancel">‹</button><h1>修改密码</h1><span aria-hidden="true"></span></header>
    <section class="password-card"><p class="hint">修改成功后，需要使用新密码重新登录。</p><van-form @submit="submit">
      <div class="editor-field"><label for="old-password">原密码</label><van-field id="old-password" v-model="form.oldPassword" name="oldPassword" type="password" autocomplete="current-password" :rules="[{ required: true, message: '请填写当前密码' }]" /></div>
      <div class="editor-field"><label for="new-password">新密码</label><van-field id="new-password" v-model="form.newPassword" name="newPassword" type="password" autocomplete="new-password" placeholder="至少 8 位" :rules="[{ required: true, message: '请填写新密码' }]" /></div>
      <div class="editor-field"><label for="confirm-password">确认新密码</label><van-field id="confirm-password" v-model="form.confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" :rules="[{ required: true, message: '请确认新密码' }]" /></div>
      <div class="actions"><van-button block type="primary" native-type="submit" :loading="loading">确认修改</van-button><van-button block plain @click="cancel">取消</van-button></div>
    </van-form>
    </section>
  </main>
</template>

<style scoped>
.password-page { min-height: 100vh; padding: calc(12px + env(safe-area-inset-top)) 16px 32px; }.password-page header { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; margin-bottom: 16px; }.password-page header button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); font-size: 26px; line-height: 1; }.password-page h1 { margin: 0; color: var(--color-text-primary); font-size: 18px; text-align: center; }.password-card { padding: 20px 17px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }.hint { margin: 0 0 22px; color: var(--color-muted); font-size: 13px; }.editor-field { margin-bottom: 19px; }.editor-field label { display: block; margin-bottom: 8px; color: var(--color-text-primary); font-size: 13px; font-weight: 700; }.editor-field :deep(.van-cell) { min-height: 48px; padding: 0 13px; border: 1px solid var(--color-border); border-radius: 14px; background: #fbfbf9; }.editor-field :deep(.van-field__body) { min-height: 46px; }.editor-field :deep(.van-field__control) { min-height: 44px; color: var(--color-text-primary); font-size: 15px; }.editor-field :deep(.van-field__control::placeholder) { color: var(--color-muted); }.actions { display: grid; gap: 10px; margin-top: 25px; }.actions :deep(.van-button) { height: 46px; border-radius: 14px; font-weight: 700; }
</style>
