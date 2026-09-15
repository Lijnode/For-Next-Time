<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadRequestOptions } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronRight, House, Info, KeyRound, LogOut, SquarePen } from 'lucide-vue-next'
import { getUserProfile, uploadAvatar, type UserProfile } from '../../api/user'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile>()
const loading = ref(false)
const uploadingAvatar = ref(false)
const aboutDialogVisible = ref(false)

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
 * 在 Element Plus 提交前校验头像格式和大小。
 */
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  // 1. 检查文件是否符合后端允许的 JPEG、PNG 与 2MB 限制。
  const valid = ['image/jpeg', 'image/png'].includes(file.type) && file.size <= 2 * 1024 * 1024
  // 2. 阻止不合格文件进入上传流程并展示一致的错误提示。
  if (!valid) {
    ElMessage.error('头像仅支持 JPG 或 PNG，且不能超过 2MB')
  }
  return valid
}

/**
 * 通过 Element Plus 的自定义请求钩子上传头像并刷新页面资料。
 *
 * @param options Element Plus 提供的本次上传文件与回调信息
 */
async function uploadAvatarRequest(options: UploadRequestOptions) {
  // 1. 锁定头像入口，避免用户在前一次上传未完成时重复提交。
  uploadingAvatar.value = true
  try {
    // 2. 复用 API 层上传方法，并使用后端返回的资料更新页面。
    profile.value = await uploadAvatar(options.file)
    ElMessage.success('头像已更新')
  } catch (error) {
    // 3. 将后端的可读错误交给 Element Plus 的失败状态处理。
    const message = error instanceof Error ? error.message : '头像上传失败，请稍后重试'
    ElMessage.error(message)
    throw new Error(message)
  } finally {
    // 4. 上传完成后恢复头像入口的交互能力。
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
 * 打开产品介绍弹窗，展示与移动端一致的应用说明。
 */
function openAboutDialog() {
  // 1. 仅切换本页弹窗状态，不离开当前个人页。
  aboutDialogVisible.value = true
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
  // 1. 清理 Pinia 与浏览器本地存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护的资料页面。
  await router.replace({ name: 'login' })
}

// 1. 页面进入时加载当前账户资料。
onMounted(loadProfile)
</script>

<template>
  <main class="profile-page">
    <section class="profile-card" v-loading="loading">
      <template v-if="profile">
        <div class="profile-head">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :disabled="uploadingAvatar"
            :before-upload="beforeAvatarUpload"
            :http-request="uploadAvatarRequest"
          >
            <span class="profile-mark" :class="{ 'is-uploading': uploadingAvatar }">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="`${profile.username} 的头像`">
              <span v-else>{{ profile.username.slice(0, 1) }}</span>
              <span class="avatar-edit" aria-hidden="true"><SquarePen :size="14" /></span>
            </span>
          </el-upload>
          <div>
            <h1>{{ profile.username }}</h1>
            <p>已经留下 {{ profile.contentCount }} 条内容</p>
          </div>
        </div>
        <div class="menu">
          <div class="menu-item"><span>内容总数</span><strong>{{ profile.contentCount }}</strong></div>
          <button class="menu-item" type="button" @click="openPasswordForm"><span>修改密码</span><KeyRound :size="17" /></button>
          <div class="menu-item"><span>加入时间</span><strong>{{ profile.createTime }}</strong></div>
        </div>
        <section class="about-menu" aria-label="产品信息">
          <button class="menu-item about-row" type="button" @click="openAboutDialog"><span><Info :size="17" />关于下次一定</span><ChevronRight :size="17" /></button>
        </section>
        <div class="actions">
          <el-button @click="backHome"><span class="action-content"><House :size="16" /><span>返回首页</span></span></el-button>
          <el-button class="logout-button" text @click="logout"><span class="action-content"><LogOut :size="16" /><span>退出登录</span></span></el-button>
        </div>
      </template>
    </section>
    <el-dialog v-model="aboutDialogVisible" width="440px" align-center class="about-dialog">
      <section class="about-dialog-content">
        <div class="brand-mark" aria-hidden="true">次</div>
        <h2>下次一定</h2>
        <p class="english-name">FOR NEXT TIME</p>
        <p class="slogan">这次先留着，下次一定用得上</p>
        <p class="intro">一个用于收藏、搜索和快速复用句子与内容素材的私人小工具</p>
        <p class="version">V1.0</p>
      </section>
      <template #footer><el-button type="primary" @click="aboutDialogVisible = false">知道了</el-button></template>
    </el-dialog>
  </main>
</template>

<style scoped>
.profile-page { width: min(760px, calc(100% - 64px)); margin: 0 auto; padding: 42px 0 72px; }
.profile-card { padding: 32px; border: 1px solid var(--color-border); border-radius: 22px; background: var(--color-surface); box-shadow: 0 10px 25px rgb(31 33 33 / 4%); }
.profile-head { display: flex; align-items: center; gap: 16px; }
.avatar-uploader :deep(.el-upload) { display: block; border-radius: 20px; cursor: pointer; outline: none; }
.avatar-uploader :deep(.el-upload:focus-visible) { outline: 2px solid var(--color-brand); outline-offset: 3px; }
.profile-mark { position: relative; display: grid; width: 62px; height: 62px; place-items: center; overflow: hidden; border-radius: 20px; background: var(--color-brand); color: #fff; font-size: 22px; font-weight: 800; box-shadow: 0 7px 15px rgb(255 111 78 / 18%); transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease; }
.avatar-uploader:hover .profile-mark:not(.is-uploading) { transform: translateY(-1px); box-shadow: 0 10px 19px rgb(255 111 78 / 27%); }
.profile-mark.is-uploading { cursor: wait; opacity: 0.75; }
.profile-mark img { width: 100%; height: 100%; object-fit: cover; }
.avatar-edit { position: absolute; right: 4px; bottom: 4px; z-index: 1; display: grid; width: 18px; height: 18px; place-items: center; color: #24324A; font-size: 14px; }
.avatar-uploader:hover .avatar-edit { color: #151D2B; }
.profile-head h1 { margin: 0; color: var(--color-text-primary); font-size: 21px; }
.profile-head p { margin: 6px 0 0; color: var(--color-text-tertiary); font-size: 13px; }
.menu { overflow: hidden; margin-top: 26px; border: 1px solid var(--color-border); border-radius: 18px; }
.menu-item { display: flex; width: 100%; align-items: center; justify-content: space-between; min-height: 55px; padding: 0 18px; border: 0; border-bottom: 1px solid #efefeb; background: #fff; color: var(--color-text-secondary); font: inherit; font-size: 14px; text-align: left; }
.menu-item:last-child { border-bottom: 0; }
.menu-item strong { color: var(--color-text-primary); font-weight: 700; }
.menu-item button { cursor: pointer; }
.about-menu { overflow: hidden; margin-top: 16px; border: 1px solid var(--color-border); border-radius: 18px; }
.about-row { cursor: pointer; color: var(--color-text-primary); }
.about-row > span { display: inline-flex; align-items: center; gap: 9px; }
.about-row > svg { color: var(--color-text-tertiary); }
.actions { display: flex; align-items: center; gap: 10px; margin-top: 22px; }
.actions :deep(.el-button) { min-height: 40px; border-radius: 12px; }
.action-content { display: inline-flex; align-items: center; gap: 9px; }
.logout-button { margin-left: auto; color: var(--color-text-tertiary); }
.about-dialog-content { padding: 20px 16px 8px; text-align: center; }
.brand-mark { display: grid; width: 68px; height: 68px; margin: 0 auto 17px; place-items: center; border-radius: 23px; background: var(--color-brand); color: #fff; font-size: 27px; font-weight: 800; box-shadow: 0 9px 20px rgb(255 111 78 / 22%); }
.about-dialog-content h2 { margin: 0; color: var(--color-text-primary); font-size: 24px; }.english-name { margin: 7px 0 30px; color: var(--color-brand); font-size: 11px; font-weight: 750; letter-spacing: .16em; }.slogan { margin: 0; color: var(--color-text-primary); font-size: 16px; font-weight: 600; }.intro { max-width: 270px; margin: 16px auto 0; color: var(--color-text-tertiary); font-size: 13px; line-height: 1.75; }.version { display: inline-block; margin: 30px 0 0; padding: 5px 10px; border-radius: 999px; background: #fff0eb; color: var(--color-brand); font-size: 12px; font-weight: 700; }
:global(.about-dialog) { border-radius: 20px; }
:global(.about-dialog .el-dialog__header) { margin-right: 0; padding: 20px 24px 0; }
:global(.about-dialog .el-dialog__title) { color: var(--color-text-primary); font-weight: 700; }
:global(.about-dialog .el-dialog__body) { padding: 0 24px; }
:global(.about-dialog .el-dialog__footer) { padding: 8px 24px 22px; text-align: center; }
:global(.about-dialog .el-dialog__footer .el-button) { min-width: 106px; height: 40px; border-radius: 12px; font-weight: 700; }
</style>
