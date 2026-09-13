<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getContentList, getRecentContents, recordCopy, type ContentListItem } from '../../api/content'
import { copyText } from '../../platform/clipboard'
import { showError, showSuccess } from '../../platform/feedback'

const router = useRouter()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const keyword = ref('')
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const categories: Array<[string, string]> = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]

/**
 * 进入新增内容页面。
 */
async function createContent() {
  // 1. 进入受保护的内容编辑页。
  await router.push({ name: 'content-create' })
}

/**
 * 切换首页分类，并按当前用户范围重新加载内容。
 */
async function selectCategory(value: string | null) {
  // 1. 保存用户轻触选择的固定分类编码。
  categoryCode.value = value
  // 2. 切换分类后回到第一页，避免页码超出筛选结果范围。
  pageNum.value = 1
  await loadContentPage(false)
}

/**
 * 请求下一页内容并追加到当前移动端列表。
 */
async function loadMore() {
  // 1. 没有下一页或正在加载时不重复请求。
  if (!hasMore.value || loadingMore.value) return
  // 2. 请求下一页，只有成功后才更新当前页码。
  await loadContentPage(true, pageNum.value + 1)
}

/**
 * 加载当前分类与页码的内容分页结果。
 */
async function loadContentPage(append: boolean, targetPage = pageNum.value) {
  // 1. 请求当前用户范围内的指定内容页。
  loadingMore.value = true
  try {
    const page = await getContentList(targetPage, categoryCode.value)
    contents.value = append ? [...contents.value, ...page.list] : page.list
    pageNum.value = page.pageNum
    hasMore.value = page.pageNum < page.pages
  } catch (error) {
    // 2. 请求失败时保留当前内容并提示用户。
    showError(error instanceof Error ? error.message : '加载内容失败，请稍后重试')
  } finally {
    // 3. 请求结束后恢复加载更多按钮状态。
    loadingMore.value = false
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
  const [page, recent] = await Promise.all([getContentList(pageNum.value, categoryCode.value), getRecentContents()])
  // 2. 写入首页两个独立展示区域。
  contents.value = page.list
  recentContents.value = recent
  hasMore.value = page.pageNum < page.pages
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
    <section class="recent-section"><h2>最近使用</h2><div v-if="recentContents.length"><article v-for="item in recentContents" :key="item.id" class="recent-item" @click="openDetail(item.id)"><p>{{ item.content }}</p><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></article></div><p v-else class="recent-empty">最近还没用上这里的内容。</p></section>
    <section class="all-content-section"><h2>全部内容</h2><div class="category-scroller"><button :class="{ active: categoryCode === null }" type="button" @click="selectCategory(null)">全部</button><button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="selectCategory(category[0])">{{ category[1] }}</button></div><div class="content-list"><article v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item.id)"><div class="card-top"><van-tag v-if="item.pinned" plain type="warning">置顶</van-tag><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div><p>{{ item.content }}</p><div class="card-footer"><div class="tag-row"><van-tag v-if="item.categoryName" plain type="warning">{{ item.categoryName }}</van-tag><van-tag v-for="tag in item.tags.slice(0, 3)" :key="tag.id" plain type="primary">#{{ tag.name }}</van-tag></div><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></div></article></div><p v-if="!contents.length" class="description">这里还空着，先留下一句。</p><van-button v-if="hasMore" class="load-more" block plain round type="primary" :loading="loadingMore" @click="loadMore">加载更多</van-button></section>
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
.recent-section,.all-content-section{margin-top:28px}.recent-section h2,.all-content-section h2{font-size:17px}.recent-item{display:flex;align-items:center;gap:8px;width:100%;margin:8px 0;padding:11px 13px;border:1px solid var(--color-border);border-radius:var(--radius-card);background:var(--color-card)}.recent-item p{flex:1;overflow:hidden;margin:0;color:var(--color-muted);text-align:left;text-overflow:ellipsis;white-space:nowrap}.recent-empty{margin:8px 0 0;color:var(--color-muted);font-size:14px}.category-scroller{display:flex;gap:8px;overflow-x:auto;padding:4px 0 12px}.category-scroller button{flex:0 0 auto;padding:7px 12px;border:1px solid var(--color-border);border-radius:999px;background:var(--color-card);color:var(--color-muted)}.category-scroller button.active{border-color:var(--color-brand);background:#fff1ec;color:var(--color-brand)}
.content-list{margin-top:4px}.content-card{margin-bottom:12px;padding:16px;background:var(--color-card);border:1px solid var(--color-border);border-radius:var(--radius-card);cursor:pointer}.content-card p{min-height:48px;white-space:pre-wrap}.card-top,.card-footer,.tag-row{display:flex;align-items:center;gap:6px}.card-top{justify-content:space-between;color:var(--color-muted);font-size:12px}.card-footer{justify-content:space-between}.tag-row{flex-wrap:wrap}.load-more{margin-top:20px}
</style>
