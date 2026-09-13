<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordCopy, searchContents, type ContentListItem } from '../../api/content'
import { copyText } from '../../platform/clipboard'
import { showError, showSuccess } from '../../platform/feedback'

const route = useRoute()
const router = useRouter()
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const categoryCode = ref<string | null>(null)
const contents = ref<ContentListItem[]>([])
const loading = ref(false)
const copyingId = ref<number | null>(null)
const categories: Array<[string, string]> = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]
let searchTimer: ReturnType<typeof setTimeout> | undefined

/**
 * 立即请求当前关键词与分类组合的搜索结果。
 */
async function executeSearch() {
  // 1. 清理关键词；空关键词时清空结果而不请求接口。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    contents.value = []
    return
  }
  // 2. 调用统一搜索接口，并通过平台层反馈搜索失败原因。
  loading.value = true
  try {
    contents.value = await searchContents(normalizedKeyword, categoryCode.value)
  } catch (error) {
    showError(error instanceof Error ? error.message : '搜索失败，请稍后重试')
  } finally {
    // 3. 请求完成后恢复搜索状态。
    loading.value = false
  }
}

/**
 * 对输入变化进行 400ms 防抖，减少连续输入产生的请求。
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

// 1. 关键词和分类变化时统一走防抖搜索。
watch([keyword, categoryCode], scheduleSearch)
// 2. 带关键词进入页面时立即执行首次搜索。
onMounted(() => void executeSearch())
// 3. 页面离开时清理尚未执行的定时任务。
onBeforeUnmount(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <main class="search-page">
    <header class="top-bar"><button type="button" aria-label="返回" @click="router.back()">‹</button><h1>搜索</h1></header>
    <van-search v-model="keyword" placeholder="搜索正文、备注、标签或分类" shape="round" show-action @search="executeSearch"><template #action><button type="button" @click="executeSearch">搜索</button></template></van-search>
    <div class="category-scroller"><button :class="{ active: categoryCode === null }" type="button" @click="categoryCode = null">全部</button><button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="categoryCode = category[0]">{{ category[1] }}</button></div>
    <van-loading v-if="loading" class="loading" vertical>正在翻找……</van-loading><p v-else-if="keyword.trim() && !contents.length" class="empty-message">这次没翻到合适的。</p>
    <section class="content-list"><article v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item.id)"><p>{{ item.content }}</p><div class="card-footer"><div class="tag-row"><van-tag v-if="item.categoryName" plain type="warning">{{ item.categoryName }}</van-tag><van-tag v-for="tag in item.tags.slice(0, 3)" :key="tag.id" plain type="primary">#{{ tag.name }}</van-tag></div><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></div></article></section>
  </main>
</template>

<style scoped>
.search-page{min-height:100vh;padding:calc(10px + env(safe-area-inset-top)) 0 32px}.top-bar{display:flex;align-items:center;padding:0 16px 8px}.top-bar button{border:0;background:none;color:var(--color-brand);font-size:28px;line-height:1}.top-bar h1{margin:0 auto;font-size:18px;transform:translateX(-14px)}.top-bar+ :deep(.van-search__action button){border:0;background:none;color:var(--color-brand);font:inherit}.category-scroller{display:flex;gap:8px;overflow-x:auto;padding:4px 16px 16px}.category-scroller button{flex:0 0 auto;padding:7px 12px;border:1px solid var(--color-border);border-radius:999px;background:var(--color-card);color:var(--color-muted)}.category-scroller button.active{border-color:var(--color-brand);background:#fff1ec;color:var(--color-brand)}.loading{padding:64px 0}.empty-message{margin:48px 0;color:var(--color-muted);text-align:center}.content-list{padding:0 16px}.content-card{margin-bottom:12px;padding:16px;background:var(--color-card);border:1px solid var(--color-border);border-radius:var(--radius-card)}.content-card p{margin:0 0 14px;line-height:1.7;white-space:pre-wrap}.card-footer{display:flex;align-items:center;justify-content:space-between;gap:6px}.tag-row{display:flex;flex-wrap:wrap;gap:6px}
</style>
