<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { House, KeyRound, LogOut } from 'lucide-vue-next'
import type { UploaderAfterRead } from 'vant'
import { getUserProfile, uploadAvatar, type UserProfile } from '../../api/user'
import { showError, showSuccess } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile>()
const loading = ref(false)
const uploadingAvatar = ref(false)

/**
 * 加载当前登录用户的个人资料。
 */
async function loadProfile() {
  // 1. 进入加载状态并请求当前用户资料。
  loading.value = true
  try {
    profile.value = await getUserProfile()
  // 2. 请求结束后恢复页面交互状态。
  } finally {
    loading.value = false
  }
}

/**
 * 校验并上传 Vant 上传组件读取到的头像文件。
 *
 * @param item Vant 返回的已读取文件对象
 */
const handleAvatarRead: UploaderAfterRead = async (item) => {
  // 1. 读取单文件上传结果，并忽略取消选择或不完整文件。
  const file = Array.isArray(item) ? item[0]?.file : item.file
  if (!file) {
    return
  }
  // 2. 在请求前提示不符合服务端约束的文件。
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) {
    showError('头像仅支持 JPG 或 PNG，且不能超过 2MB')
    return
  }
  // 3. 上传文件并用服务端返回的最新资料更新页面。
  uploadingAvatar.value = true
  try {
    profile.value = await uploadAvatar(file)
    showSuccess('头像已更新')
  } catch (error) {
    showError(error instanceof Error ? error.message : '头像上传失败，请稍后重试')
  } finally {
    // 4. 恢复上传入口，允许用户再次选择头像。
    uploadingAvatar.value = false
  }
}

/**
 * 进入修改当前账户密码的页面。
 */
async function openPasswordForm() {
  // 1. 跳转至同样受登录保护的密码修改路由。
  await router.push({ name: 'password-change' })
}

/**
 * 返回内容首页继续浏览已保存的内容。
 */
async function backHome() {
  // 1. 返回当前用户的内容首页。
  await router.push({ name: 'home' })
}

/**
 * 清除当前账户登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与平台存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护的资料页面。
  await router.replace({ name: 'login' })
}

// 1. 页面进入时加载当前账户资料。
onMounted(loadProfile)
</script>

<template>
  <main class="profile-page">
    <van-loading v-if="loading" class="loading" vertical>正在加载</van-loading>
    <template v-else-if="profile">
      <section class="profile-card">
        <div class="profile-head">
          <van-uploader class="avatar-uploader" accept="image/jpeg,image/png" :max-count="1" :preview-image="false" :disabled="uploadingAvatar" :after-read="handleAvatarRead">
            <button class="profile-mark" type="button" :aria-label="uploadingAvatar ? '正在上传头像' : '更换头像'">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="`${profile.username} 的头像`">
              <span v-else>{{ profile.username.slice(0, 1) }}</span>
            </button>
          </van-uploader>
          <div><h1>{{ profile.username }}</h1><p>已经留下 {{ profile.contentCount }} 条内容</p></div>
        </div>
        <div class="menu">
          <div class="menu-item"><span>内容总数</span><strong>{{ profile.contentCount }}</strong></div>
          <button class="menu-item" type="button" @click="openPasswordForm"><span>修改密码</span><KeyRound :size="17" /></button>
          <div class="menu-item"><span>加入时间</span><strong>{{ profile.createTime }}</strong></div>
        </div>
        <section class="about"><p>关于下次一定</p><span>这次先留着，下次一定用得上。</span></section>
        <div class="actions"><van-button plain @click="backHome"><House :size="16" />返回首页</van-button><button class="logout-button" type="button" @click="logout"><LogOut :size="16" />退出登录</button></div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.profile-page { min-height: 100vh; padding: calc(24px + env(safe-area-inset-top)) 16px 32px; }
.loading { margin-top: 80px; }
.profile-card { padding: 20px 17px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }
.profile-head { display: flex; align-items: center; gap: 13px; }
.avatar-uploader { display: block; flex: 0 0 54px; width: 54px; height: 54px; }.avatar-uploader :deep(.van-uploader__wrapper) { display: block; width: 54px; height: 54px; }.avatar-uploader :deep(.van-uploader__input) { width: 54px; height: 54px; }
.profile-mark { position: relative; display: grid; box-sizing: border-box; width: 54px; height: 54px; padding: 0; place-items: center; overflow: hidden; border: 0; border-radius: 18px; background: var(--color-brand); color: #fff; font: inherit; font-size: 20px; font-weight: 800; box-shadow: 0 6px 14px rgb(255 111 78 / 20%); }
.profile-mark:disabled { opacity: 0.75; }
.profile-mark img { position: absolute; inset: 0; display: block; width: 100% !important; height: 100% !important; max-width: none; object-fit: cover; }
.profile-head h1 { margin: 0; color: var(--color-text-primary); font-size: 19px; }
.profile-head p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }
.menu { overflow: hidden; margin-top: 21px; border: 1px solid var(--color-border); border-radius: 16px; }
.menu-item { display: flex; width: 100%; min-height: 52px; align-items: center; justify-content: space-between; padding: 0 15px; border: 0; border-bottom: 1px solid #efefeb; background: #fff; color: var(--color-text-secondary); font: inherit; font-size: 13px; text-align: left; }
.menu-item:last-child { border-bottom: 0; }
.menu-item strong { color: var(--color-text-primary); font-weight: 700; }
.about { margin-top: 12px; padding: 15px; border: 1px solid var(--color-border); border-radius: 15px; background: #fafaf8; }
.about p { margin: 0 0 7px; color: var(--color-muted); font-size: 11px; font-weight: 700; }
.about span { color: var(--color-text-secondary); font-size: 13px; }
.actions { display: flex; align-items: center; gap: 10px; margin-top: 19px; }
.actions :deep(.van-button) { display: inline-flex; align-items: center; gap: 7px; height: 40px; border-radius: 12px; font-weight: 700; }
.logout-button { display: inline-flex; align-items: center; gap: 7px; margin-left: auto; padding: 8px 0; border: 0; background: transparent; color: var(--color-muted); font: inherit; font-size: 12px; }
</style>
