<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserProfile, type UserProfile } from '../../api/user'

const router = useRouter()
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

// 1. 页面进入时加载当前账户资料。
onMounted(loadProfile)
</script>

<template>
  <main class="profile-page">
    <section class="profile-card" v-loading="loading">
      <p class="eyebrow">MY SPACE</p>
      <h1>我的</h1>
      <template v-if="profile">
        <dl>
          <div><dt>用户名</dt><dd>{{ profile.username }}</dd></div>
          <div><dt>已留内容</dt><dd>{{ profile.contentCount }} 条</dd></div>
          <div><dt>加入时间</dt><dd>{{ profile.createTime }}</dd></div>
        </dl>
        <div class="actions">
          <el-button type="primary" @click="openPasswordForm">修改密码</el-button>
          <el-button @click="backHome">返回首页</el-button>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.profile-page { width: min(760px, calc(100% - 64px)); margin: 0 auto; padding: 96px 0; }
.profile-card { padding: 48px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); box-shadow: 0 16px 36px rgb(32 35 34 / 7%); }
.eyebrow { margin: 0; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
h1 { margin: 12px 0 32px; font-size: 30px; }
dl { margin: 0; }
dl div { display: flex; justify-content: space-between; padding: 18px 0; border-top: 1px solid var(--color-border); }
dt { color: var(--color-text-secondary); } dd { margin: 0; font-weight: 600; }
.actions { display: flex; gap: 12px; margin-top: 32px; }
</style>
