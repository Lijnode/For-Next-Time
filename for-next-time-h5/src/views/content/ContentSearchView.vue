<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Search } from 'lucide-vue-next'
import { recordCopy, searchContents, type ContentListItem } from '../../api/content'
import { copyText } from '../../platform/clipboard'
import { showError, showSuccess } from '../../platform/feedback'

const route = useRoute()
const router = useRouter()
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const contents = ref<ContentListItem[]>([])
const loading = ref(false)
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | undefined

/**
 * 立即请求当前关键词对应的搜索结果。
 */
async function executeSearch() {
  // 1. 清理关键词；空关键词时清空结果而不请求接口。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    contents.value = []
    hasMore.value = false
    return
  }
  // 2. 关键词变动后从第一页开始查询。
  pageNum.value = 1
  await loadSearchPage(false)
}

/**
 * 请求下一页搜索结果并追加到当前移动端列表。
 */
async function loadMore() {
  // 1. 没有下一页或正在加载时不重复请求。
  if (!hasMore.value || loadingMore.value) return
  // 2. 请求下一页，只有成功后才更新当前页码。
  await loadSearchPage(true, pageNum.value + 1)
}

/**
 * 加载当前关键词和页码对应的搜索结果。
 */
async function loadSearchPage(append: boolean, targetPage = pageNum.value) {
  // 1. 空关键词不请求接口，保持搜索结果为空。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) return
  // 2. 调用统一搜索接口，并通过平台层反馈搜索失败原因。
  loading.value = !append
  loadingMore.value = append
  try {
    const page = await searchContents(normalizedKeyword, targetPage)
    contents.value = append ? [...contents.value, ...page.list] : page.list
    pageNum.value = page.pageNum
    hasMore.value = page.pageNum < page.pages
  } catch (error) {
    showError(error instanceof Error ? error.message : '搜索失败，请稍后重试')
  } finally {
    // 3. 请求完成后恢复首屏和加载更多状态。
    loading.value = false
    loadingMore.value = false
  }
}

/**
 * 对关键词输入进行 400ms 防抖，减少连续输入产生的请求。
 */
function scheduleSearch() {
  // 1. 清除尚未执行的旧搜索任务。
  if (searchTimer) clearTimeout(searchTimer)
  // 2. 创建新的延迟搜索任务。
  searchTimer = setTimeout(() => void executeSearch(), 400)
}

/**
 * 打开指定内容的详情页。
 */
async function openDetail(id: number) {
  // 1. 将内容主键带入受保护的详情路由。
  await router.push({ name: 'content-detail', params: { id } })
}

/**
 * 通过平台剪贴板复制搜索结果正文并记录一次使用统计。
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
    // 6. 恢复当前结果卡片的复制按钮状态。
    copyingId.value = null
  }
}

// 1. 关键词变化时统一走防抖搜索。
watch(keyword, scheduleSearch)
// 2. 带关键词进入页面时立即执行首次搜索。
onMounted(() => void executeSearch())
// 3. 页面离开时清理尚未执行的定时任务。
onBeforeUnmount(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <main class="search-page">
    <header class="top-bar"><button type="button" aria-label="返回" @click="router.back()"><ArrowLeft :size="21" /></button><h1>找一句</h1><span aria-hidden="true"></span></header>
    <van-search v-model="keyword" class="search-input" placeholder="搜索正文、备注、标签或分类" shape="round" @search="executeSearch"><template #left-icon><Search :size="19" /></template><template #right-icon><button class="search-button" type="button" aria-label="搜索" @click="executeSearch"><Search :size="18" /></button></template></van-search>
    <van-loading v-if="loading" class="loading" vertical>正在翻找……</van-loading><p v-else-if="keyword.trim() && !contents.length" class="empty-message">这次没翻到合适的。</p>
    <section class="content-list"><article v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item.id)"><div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div><p class="content-text">{{ item.content }}</p><div class="tag-row"><span v-if="item.categoryName" class="tag category-tag">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="tag">#{{ tag.name }}</span></div><div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></div></article><van-button v-if="hasMore" class="load-more" block plain round type="primary" :loading="loadingMore" @click="loadMore">加载更多</van-button></section>
  </main>
</template>

<style scoped>
.search-page { min-height: 100vh; padding: calc(12px + env(safe-area-inset-top)) 0 32px; }.top-bar { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; padding: 0 16px 8px; }.top-bar button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); }.top-bar h1 { margin: 0; color: var(--color-text-primary); font-size: 18px; text-align: center; }.search-input { padding: 8px 8px 16px; background: transparent; }.search-input :deep(.van-search__content) { height: 52px; padding: 0 13px; border: 1px solid var(--color-border); border-radius: 17px; background: #fff; box-shadow: 0 5px 14px rgb(31 33 33 / 4%); }.search-input :deep(.van-field__body), .search-input :deep(.van-field__left-icon), .search-input :deep(.van-field__right-icon) { display: flex; align-items: center; }.search-input :deep(.van-field__body) { min-height: 50px; }.search-input :deep(.van-field__right-icon) { align-self: center; height: 34px; }.search-input :deep(.van-field__control) { line-height: normal; }.search-input :deep(.van-field__left-icon) { color: var(--color-muted); }.search-button { display: grid; width: 34px; height: 34px; place-items: center; border: 0; border-radius: 12px; background: var(--color-brand); color: #fff; }.loading { padding: 64px 0; }.empty-message { margin: 48px 0; color: var(--color-muted); text-align: center; }.content-list { padding: 0 16px; }.content-card { display: flex; min-height: 174px; flex-direction: column; margin-bottom: 12px; padding: 17px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }.card-top, .card-footer { display: flex; align-items: center; justify-content: space-between; color: var(--color-muted); font-size: 11px; }.pinned-mark { color: var(--color-brand); font-weight: 700; }.content-text { display: -webkit-box; flex: 1; overflow: hidden; margin: 14px 0; color: var(--color-text-primary); font-size: 16px; line-height: 1.72; white-space: pre-wrap; -webkit-box-orient: vertical; -webkit-line-clamp: 4; }.tag-row { display: flex; flex-wrap: wrap; gap: 6px; }.tag { padding: 5px 8px; border-radius: 999px; background: #f1f2ef; color: #737878; font-size: 11px; line-height: 1; }.category-tag { background: #fff1ec; color: #cf5a41; }.card-footer { margin-top: 13px; }.card-footer :deep(.van-button) { min-width: 68px; height: 40px; border-radius: 12px; font-weight: 700; }.load-more { margin-top: 20px; }
</style>
