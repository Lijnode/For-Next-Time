<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { House, KeyRound, LogOut } from 'lucide-vue-next'
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
    <van-loading v-if="loading" class="loading" vertical>正在加载</van-loading>
    <template v-else-if="profile">
      <section class="profile-card"><div class="profile-head"><span class="profile-mark">{{ profile.username.slice(0, 1) }}</span><div><h1>{{ profile.username }}</h1><p>已经留下 {{ profile.contentCount }} 条内容</p></div></div><div class="menu"><div class="menu-item"><span>内容总数</span><strong>{{ profile.contentCount }}</strong></div><button class="menu-item" type="button" @click="openPasswordForm"><span>修改密码</span><KeyRound :size="17" /></button><div class="menu-item"><span>加入时间</span><strong>{{ profile.createTime }}</strong></div></div><section class="about"><p>关于下次一定</p><span>这次先留着，下次一定用得上。</span></section><div class="actions"><van-button plain @click="backHome"><House :size="16" />返回首页</van-button><button class="logout-button" type="button" @click="logout"><LogOut :size="16" />退出登录</button></div></section>
    </template>
  </main>
</template>

<style scoped>
.profile-page { min-height: 100vh; padding: calc(24px + env(safe-area-inset-top)) 16px 32px; }.loading { margin-top: 80px; }.profile-card { padding: 20px 17px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }.profile-head { display: flex; align-items: center; gap: 13px; }.profile-mark { display: grid; width: 54px; height: 54px; place-items: center; border-radius: 18px; background: var(--color-brand); color: #fff; font-size: 20px; font-weight: 800; box-shadow: 0 6px 14px rgb(255 111 78 / 20%); }.profile-head h1 { margin: 0; color: var(--color-text-primary); font-size: 19px; }.profile-head p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }.menu { overflow: hidden; margin-top: 21px; border: 1px solid var(--color-border); border-radius: 16px; }.menu-item { display: flex; width: 100%; min-height: 52px; align-items: center; justify-content: space-between; padding: 0 15px; border: 0; border-bottom: 1px solid #efefeb; background: #fff; color: var(--color-text-secondary); font: inherit; font-size: 13px; text-align: left; }.menu-item:last-child { border-bottom: 0; }.menu-item strong { color: var(--color-text-primary); font-weight: 700; }.about { margin-top: 12px; padding: 15px; border: 1px solid var(--color-border); border-radius: 15px; background: #fafaf8; }.about p { margin: 0 0 7px; color: var(--color-muted); font-size: 11px; font-weight: 700; }.about span { color: var(--color-text-secondary); font-size: 13px; }.actions { display: flex; align-items: center; gap: 10px; margin-top: 19px; }.actions :deep(.van-button) { display: inline-flex; align-items: center; gap: 5px; height: 40px; border-radius: 12px; font-weight: 700; }.logout-button { display: inline-flex; align-items: center; gap: 5px; margin-left: auto; padding: 8px 0; border: 0; background: transparent; color: var(--color-muted); font: inherit; font-size: 12px; }
</style>
