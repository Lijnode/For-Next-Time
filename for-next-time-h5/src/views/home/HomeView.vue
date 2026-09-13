<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getContentList, getRecentContents, recordCopy, type ContentListItem } from '../../api/content'
import { copyText } from '../../platform/clipboard'
import { showError, showSuccess } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const keyword = ref('')
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const categories: Array<[string, string]> = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]

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
 * 切换首页分类，并按当前用户范围重新加载内容。
 */
async function selectCategory(value: string | null) {
  // 1. 保存用户轻触选择的固定分类编码。
  categoryCode.value = value
  // 2. 只刷新列表，最近使用区域保持独立。
  try {
    contents.value = await getContentList(categoryCode.value)
  } catch (error) {
    // 3. 分类切换失败时保留当前内容并提示用户。
    showError(error instanceof Error ? error.message : '加载内容失败，请稍后重试')
  }
}

/**
 * 通过平台剪贴板复制卡片正文并记录一次使用统计。
 */
async function copyContent(item: ContentListItem) {
  // 1. 在剪贴板写入成功前不更新后端统计。
  copyingId.value = item.id
  try {
    await copyText(item.content)
    // 2. 复制完成后显示品牌化成功反馈。
    showSuccess('就是这句。')
    // 3. 统计接口失败不影响已经成功的复制操作。
    try {
      const stat = await recordCopy(item.id)
      item.copyCount = stat.copyCount
      item.lastCopyTime = stat.lastCopyTime
    } catch {
      // 4. 接受少量统计误差，避免自动重试导致重复计数。
    }
  } catch (error) {
    // 5. 仅在平台复制失败时提示用户。
    showError(error instanceof Error ? error.message : '复制失败，请稍后重试')
  } finally {
    // 6. 恢复当前卡片的复制按钮状态。
    copyingId.value = null
  }
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
  const [list, recent] = await Promise.all([getContentList(categoryCode.value), getRecentContents()])
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
    <section class="all-content-section"><h2>全部内容</h2><div class="category-scroller"><button :class="{ active: categoryCode === null }" type="button" @click="selectCategory(null)">全部</button><button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="selectCategory(category[0])">{{ category[1] }}</button></div><div class="content-list"><article v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item.id)"><div class="card-top"><van-tag v-if="item.pinned" plain type="warning">置顶</van-tag><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div><p>{{ item.content }}</p><div class="card-footer"><div class="tag-row"><van-tag v-if="item.categoryName" plain type="warning">{{ item.categoryName }}</van-tag><van-tag v-for="tag in item.tags.slice(0, 3)" :key="tag.id" plain type="primary">#{{ tag.name }}</van-tag></div><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></div></article></div><p v-if="!contents.length" class="description">这里还空着，先留下一句。</p></section>
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
.recent-section,.all-content-section{margin-top:28px}.recent-section h2,.all-content-section h2{font-size:17px}.recent-item{display:block;width:100%;overflow:hidden;margin:8px 0;padding:13px;border:1px solid var(--color-border);border-radius:var(--radius-card);background:var(--color-card);color:var(--color-muted);text-align:left;text-overflow:ellipsis;white-space:nowrap}.category-scroller{display:flex;gap:8px;overflow-x:auto;padding:4px 0 12px}.category-scroller button{flex:0 0 auto;padding:7px 12px;border:1px solid var(--color-border);border-radius:999px;background:var(--color-card);color:var(--color-muted)}.category-scroller button.active{border-color:var(--color-brand);background:#fff1ec;color:var(--color-brand)}
.content-list{margin-top:4px}.content-card{margin-bottom:12px;padding:16px;background:var(--color-card);border:1px solid var(--color-border);border-radius:var(--radius-card);cursor:pointer}.content-card p{min-height:48px;white-space:pre-wrap}.card-top,.card-footer,.tag-row{display:flex;align-items:center;gap:6px}.card-top{justify-content:space-between;color:var(--color-muted);font-size:12px}.card-footer{justify-content:space-between}.tag-row{flex-wrap:wrap}
</style>
