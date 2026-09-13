<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getContentList, getRecentContents, type ContentListItem } from '../../api/content'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const keyword = ref('')

/**
 * 清除登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与平台存储中的认证信息。
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
 * 进入当前登录用户的个人资料页。
 */
async function openProfile() {
  // 1. 进入受保护的个人资料路由。
  await router.push({ name: 'profile' })
}

/**
 * 打开用户轻触的内容详情。
 */
async function openDetail(id: number) {
  // 1. 跳转至受保护的详情路由。
  await router.push({ name: 'content-detail', params: { id } })
}

/**
 * 使用首页关键词进入内容搜索页。
 */
async function openSearch() {
  // 1. 清理空白关键词，避免跳转后发起无效搜索。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) return
  // 2. 将关键词带入受保护的搜索路由。
  await router.push({ name: 'content-search', query: { keyword: normalizedKeyword } })
}

/**
 * 加载首页内容列表，失败时保留空状态。
 */
async function loadContents() {
  // 1. 并行请求当前用户内容列表和最近成功复制内容。
  const [list, recent] = await Promise.all([getContentList(), getRecentContents()])
  // 2. 写入首页两个独立展示区域。
  contents.value = list
  recentContents.value = recent
}

// 1. 进入首页后加载内容列表。
onMounted(loadContents)
</script>

<template>
  <main class="home-page">
    <p class="eyebrow">FOR NEXT TIME</p>
    <h1>下次一定</h1>
    <p class="description">先留下，找得到，用得上。</p>
    <van-search v-model="keyword" placeholder="找找现在用得上的。" shape="round" @search="openSearch" />
    <van-button type="primary" block @click="createContent">先留一句</van-button>
    <van-button plain type="primary" @click="openProfile">我的</van-button>
    <van-button plain type="primary" @click="logout">退出登录</van-button>
    <section v-if="recentContents.length" class="recent-section"><h2>最近使用</h2><button v-for="item in recentContents" :key="item.id" type="button" class="recent-item" @click="openDetail(item.id)">{{ item.content }}</button></section>
    <section class="content-list"><article v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item.id)"><p>{{ item.content }}</p><van-tag v-for="tag in item.tags" :key="tag.id" type="primary">#{{ tag.name }}</van-tag></article><p v-if="!contents.length" class="description">这里还空着，先留下一句。</p></section>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 96px 24px;
  background: var(--color-page);
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

h1 {
  margin: 0;
  font-size: 32px;
  letter-spacing: 0.04em;
}

.description {
  margin-top: 16px;
  color: var(--color-muted);
}
.recent-section{margin-top:28px}.recent-section h2{font-size:17px}.recent-item{display:block;width:100%;overflow:hidden;margin:8px 0;padding:13px;border:1px solid var(--color-border);border-radius:var(--radius-card);background:var(--color-card);color:var(--color-muted);text-align:left;text-overflow:ellipsis;white-space:nowrap}
.content-list{margin-top:24px}.content-card{margin-bottom:12px;padding:16px;background:var(--color-card);border:1px solid var(--color-border);border-radius:var(--radius-card);cursor:pointer}.content-card p{white-space:pre-wrap}.content-card :deep(.van-tag){margin-right:6px}
</style>
