<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff } from 'lucide-vue-next'
import { useRequiredFormValidation } from '../../composables/useRequiredFormValidation'
import { showError } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '', confirmPassword: '' })
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const { validationMessage, clearRequiredValidation, handleValidationFailed } = useRequiredFormValidation()

/**
 * 提交注册表单，成功后直接进入首页。
 */
async function submit() {
  // 1. 清除空字段校验提示后，再校验两次密码是否一致。
  clearRequiredValidation()
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
    <header><div class="brand-heading"><p>FOR NEXT TIME</p></div><h1>创建账号</h1></header>
    <van-form :show-error-message="false" @submit="submit" @failed="handleValidationFailed">
      <p v-if="validationMessage" class="form-validation-message" role="alert">{{ validationMessage }}</p>
      <van-cell-group inset>
        <van-field v-model="form.username" name="username" label="用户名" autocomplete="username" :rules="[{ required: true, message: '请填写用户名' }]" />
        <van-field v-model="form.password" name="password" label="密码" :type="form.password && showPassword ? 'text' : 'password'" autocomplete="new-password" :rules="[{ required: true, message: '请填写密码' }]">
          <template v-if="form.password" #right-icon><button class="password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click.stop="showPassword = !showPassword"><EyeOff v-if="showPassword" :size="18" /><Eye v-else :size="18" /></button></template>
        </van-field>
        <van-field v-model="form.confirmPassword" name="confirmPassword" label="确认密码" :type="form.confirmPassword && showConfirmPassword ? 'text' : 'password'" autocomplete="new-password" :rules="[{ required: true, message: '请确认密码' }]">
          <template v-if="form.confirmPassword" #right-icon><button class="password-toggle" type="button" :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'" @click.stop="showConfirmPassword = !showConfirmPassword"><EyeOff v-if="showConfirmPassword" :size="18" /><Eye v-else :size="18" /></button></template>
        </van-field>
      </van-cell-group>
      <div class="submit-wrap"><van-button block round type="primary" native-type="submit" :loading="loading">注册</van-button></div>
    </van-form>
    <p class="switch-link">已有账号？<RouterLink to="/login">登录</RouterLink></p>
  </main>
</template>

<style scoped>
.auth-page { min-height: 100vh; padding: calc(80px + env(safe-area-inset-top)) 20px 40px; background: var(--color-page); } header { margin: 0 8px 38px; }.brand-heading { display: flex; align-items: center; } header p { margin: 0; color: var(--color-brand); font-size: 11px; font-weight: 700; letter-spacing: .18em; } h1 { margin: 18px 0 0; color: var(--color-text); font-size: 34px; letter-spacing: -.04em; } .switch-link { color: var(--color-muted); line-height: 1.7; }
:deep(.van-cell-group--inset) { margin: 0; overflow: hidden; border: 1px solid var(--color-border); border-radius: var(--radius-card); box-shadow: 0 10px 24px rgb(48 39 32 / 4%); }
:deep(.van-field) { display: block; min-height: 0; padding: 15px 16px 16px; }
:deep(.van-field::after) { display: none; }
:deep(.van-field__label) { width: auto; margin: 0 0 8px; color: var(--color-text); font-weight: 600; line-height: 1.4; }
:deep(.van-field__value) { overflow: visible; }
:deep(.van-field__body) { min-height: 46px; padding: 0 13px; border: 1px solid var(--color-border); border-radius: 14px; background: #fff; transition: border-color .2s ease, box-shadow .2s ease; }
:deep(.van-field:focus-within .van-field__body) { border-color: var(--color-brand); box-shadow: 0 0 0 4px rgb(255 111 78 / 12%); }
:deep(.van-field__control) { min-height: 44px; color: var(--color-text); font-size: 16px; }
.password-toggle { display: grid; width: 44px; height: 44px; padding: 0; place-items: center; border: 0; background: transparent; color: var(--color-muted); }
.form-validation-message { margin: 0 0 12px; padding: 10px 12px; border-radius: 12px; background: #fff1ed; color: #bb4b3d; font-size: 13px; line-height: 1.45; }
.submit-wrap { margin: 24px 0 0; }
.submit-wrap :deep(.van-button) { height: 50px; font-weight: 600; }
.switch-link { margin-top: 28px; text-align: center; }
.switch-link a { color: var(--color-brand); font-weight: 600; text-decoration: none; }
</style>
