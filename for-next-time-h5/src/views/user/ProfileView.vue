<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserProfile, type UserProfile } from '../../api/user'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile>()
const loading = ref(false)

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
    <p class="eyebrow">MY SPACE</p>
    <h1>我的</h1>
    <van-loading v-if="loading" class="loading" vertical>正在加载</van-loading>
    <template v-else-if="profile">
      <van-cell-group inset>
        <van-cell title="用户名" :value="profile.username" />
        <van-cell title="已留内容" :value="profile.contentCount + ' 条'" />
        <van-cell title="加入时间" :value="profile.createTime" />
      </van-cell-group>
      <van-cell-group inset class="about"><van-cell title="关于下次一定" label="这次先留着，下次一定用得上。" /></van-cell-group>
      <div class="actions"><van-button block round type="primary" @click="openPasswordForm">修改密码</van-button><van-button block plain round type="primary" @click="backHome">返回首页</van-button><van-button block plain round type="danger" @click="logout">退出登录</van-button></div>
    </template>
  </main>
</template>

<style scoped>
.profile-page { min-height: 100vh; padding: calc(72px + env(safe-area-inset-top)) 20px 32px; }.eyebrow { margin: 0 4px 12px; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .15em; }h1 { margin: 0 4px 32px; font-size: 32px; }.loading { margin-top: 80px; }.about { margin-top: 20px; }.actions { display: grid; gap: 12px; margin: 28px 16px 0; }
</style>
