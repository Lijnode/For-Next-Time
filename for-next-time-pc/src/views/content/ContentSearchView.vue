<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search } from 'lucide-vue-next'
import { recordCopy, searchContents, type ContentListItem } from '../../api/content'

const route = useRoute()
const router = useRouter()
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const categoryCode = ref<string | null>(null)
const contents = ref<ContentListItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const total = ref(0)
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
    errorMessage.value = ''
    total.value = 0
    return
  }
  // 2. 关键词或分类变动后从第一页开始查询。
  pageNum.value = 1
  await loadSearchPage()
}

/**
 * 切换桌面端搜索分页器页码，并保持当前查询条件。
 */
async function changePage(value: number) {
  // 1. 写入用户选择的新页码。
  pageNum.value = value
  // 2. 使用当前关键词和分类加载对应搜索结果。
  await loadSearchPage()
}

/**
 * 加载当前关键词、分类和页码对应的搜索结果。
 */
async function loadSearchPage() {
  // 1. 空关键词不请求接口，保持搜索结果为空。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) return
  // 2. 调用统一搜索接口并记录可读错误状态。
  loading.value = true
  errorMessage.value = ''
  try {
    const page = await searchContents(normalizedKeyword, pageNum.value, categoryCode.value)
    contents.value = page.list
    total.value = page.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '搜索失败，请稍后重试'
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
 * 复制搜索结果正文成功后记录一次使用统计。
 */
async function copyContent(item: ContentListItem) {
  // 1. 在剪贴板写入成功前不更新后端统计。
  copyingId.value = item.id
  try {
    await navigator.clipboard.writeText(item.content)
    // 2. 复制完成后显示品牌化成功反馈。
    ElMessage.success('就是这句。')
    // 3. 统计接口失败不影响已经成功的复制操作。
    try {
      const stat = await recordCopy(item.id)
      item.copyCount = stat.copyCount
      item.lastCopyTime = stat.lastCopyTime
    } catch {
      // 4. 接受少量统计误差，避免自动重试导致重复计数。
    }
  } catch (error) {
    // 5. 仅在浏览器复制失败时提示用户。
    ElMessage.error(error instanceof Error ? error.message : '复制失败，请稍后重试')
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
    <header class="page-title-bar"><button type="button" aria-label="返回" @click="router.back()"><ArrowLeft :size="20" /></button><div><p>FOR NEXT TIME</p><h1>找找现在用得上的。</h1></div><span aria-hidden="true"></span></header>
    <el-input v-model="keyword" class="search-input" size="large" placeholder="搜索正文、备注、标签或分类" clearable :prefix-icon="Search" @keyup.enter="executeSearch"><template #suffix><button class="search-button" type="button" aria-label="搜索" @click="executeSearch"><Search :size="19" /></button></template></el-input>
    <div class="category-filter" aria-label="按分类筛选"><button :class="{ active: categoryCode === null }" type="button" @click="categoryCode = null">全部</button><button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="categoryCode = category[0]">{{ category[1] }}</button></div>
    <p v-if="loading" class="state-message">正在翻找……</p><p v-else-if="errorMessage" class="state-message error-message">{{ errorMessage }}</p><p v-else-if="keyword.trim() && !contents.length" class="state-message">这次没翻到合适的。</p>
    <section class="content-grid"><article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)"><div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div><p class="content-text">{{ item.content }}</p><div class="tag-row"><span v-if="item.categoryName" class="tag category-tag">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="tag">#{{ tag.name }}</span></div><div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div></article></section><el-pagination v-if="total > 20" class="pagination" background layout="prev, pager, next" :current-page="pageNum" :page-size="20" :total="total" @current-change="changePage" />
  </main>
</template>

<style scoped>
.search-page { width: min(1040px, calc(100% - 64px)); margin: 0 auto; padding: 32px 0 72px; }.page-title-bar { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; margin-bottom: 20px; }.page-title-bar button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); }.page-title-bar div { text-align: center; }.page-title-bar p { margin: 0; color: var(--color-brand); font-size: 10px; font-weight: 800; letter-spacing: .17em; }.page-title-bar h1 { margin: 5px 0 0; color: var(--color-text-primary); font-size: 24px; }
.search-input { max-width: 760px; margin: 0 auto; }.search-input :deep(.el-input__wrapper) { min-height: 54px; padding: 0 8px 0 16px; border: 1px solid var(--color-border); border-radius: 18px; background: #fff; box-shadow: 0 5px 16px rgb(31 33 33 / 4%); }.search-input :deep(.el-input__wrapper.is-focus) { border-color: #ffb29f; box-shadow: 0 0 0 3px rgb(255 111 78 / 10%); }.search-button { display: grid; width: 38px; height: 38px; place-items: center; border: 0; border-radius: 13px; background: var(--color-brand); color: #fff; }
.category-filter { display: flex; flex-wrap: wrap; justify-content: center; gap: 9px; margin: 16px 0 30px; }.category-filter button { padding: 8px 14px; border: 1px solid var(--color-border); border-radius: 999px; background: #fff; color: var(--color-text-secondary); font-size: 13px; }.category-filter button.active, .category-filter button:hover { border-color: #1f2121; background: #1f2121; color: #fff; }.state-message { margin: 48px 0; color: var(--color-text-secondary); text-align: center; }.error-message { color: var(--color-danger); }
.content-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }.content-card { display: flex; min-height: 196px; flex-direction: column; padding: 20px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-surface); box-shadow: 0 7px 18px rgb(31 33 33 / 3%); cursor: pointer; transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease; }.content-card:hover, .content-card:focus-visible { border-color: #ffc1b1; outline: none; box-shadow: 0 12px 25px rgb(31 33 33 / 7%); transform: translateY(-3px); }.card-top, .card-footer { display: flex; align-items: center; justify-content: space-between; color: var(--color-text-tertiary); font-size: 12px; }.pinned-mark { color: var(--color-brand); font-weight: 700; }.content-text { display: -webkit-box; flex: 1; overflow: hidden; margin: 17px 0; color: var(--color-text-primary); font-size: 16px; line-height: 1.75; white-space: pre-wrap; -webkit-box-orient: vertical; -webkit-line-clamp: 4; }.tag-row { display: flex; flex-wrap: wrap; gap: 7px; }.tag { padding: 5px 9px; border-radius: 999px; background: #f1f2ef; color: #737878; font-size: 12px; line-height: 1; }.category-tag { background: #fff1ec; color: #cf5a41; }.card-footer { margin-top: 15px; }.card-footer :deep(.el-button) { padding: 5px 0; font-weight: 700; }.pagination { justify-content: center; margin-top: 30px; }
@media (max-width: 900px) { .content-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 600px) { .search-page { width: min(100% - 32px, 1040px); }.content-grid { grid-template-columns: 1fr; } }
</style>
