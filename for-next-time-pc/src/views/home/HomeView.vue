<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getContentList, type ContentListItem } from '../../api/content'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const contents = ref<ContentListItem[]>([])

/**
 * 清除登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与本地存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护页面。
  await router.replace({ name: 'login' })
}

/**
 * 进入新增内容页面。
 */
async function createContent() {
  // 1. 进入受保护的内容编辑页。
  await router.push({ name: 'content-create' })
}

/**
 * 打开用户选中的内容详情。
 */
async function openDetail(id: number) {
  // 1. 跳转至受保护的详情路由。
  await router.push({ name: 'content-detail', params: { id } })
}

/**
 * 加载首页内容列表，失败时保留空状态。
 */
async function loadContents() {
  // 1. 请求当前用户的默认分页内容。
  contents.value = await getContentList()
}

// 1. 进入首页后加载内容列表。
onMounted(loadContents)
</script>

<template>
  <main class="home-shell">
    <section class="welcome-card">
      <p class="eyebrow">For Next Time</p>
      <h1>下次一定</h1>
      <p>这次先留着，下次一定用得上。</p>
      <el-button type="primary" @click="createContent">先留一句</el-button>
      <el-button text type="primary" @click="logout">退出登录</el-button>
    </section>
    <section class="content-grid"><article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)"><p>{{ item.content }}</p><el-tag v-for="tag in item.tags" :key="tag.id" size="small">#{{ tag.name }}</el-tag></article><p v-if="!contents.length">这里还空着，先留下一句。</p></section>
  </main>
</template>

<style scoped>
.home-shell {
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  padding: 80px 0;
}

.welcome-card {
  padding: 48px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 18px rgb(20 24 24 / 4%);
}

.eyebrow {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

h1 {
  margin: var(--space-2) 0;
  font-size: 32px;
}

p {
  color: var(--color-text-secondary);
}
.content-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-top:24px}.content-card{padding:20px;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);cursor:pointer}.content-card:hover,.content-card:focus-visible{border-color:var(--color-brand);outline:none}.content-card p{white-space:pre-wrap}
</style>
