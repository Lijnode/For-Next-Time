<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
    <header class="search-header"><div><p class="eyebrow">FOR NEXT TIME</p><h1>找找现在用得上的。</h1></div><el-button text @click="router.back()">返回</el-button></header>
    <el-input v-model="keyword" class="search-input" placeholder="搜索正文、备注、标签或分类" clearable @keyup.enter="executeSearch"><template #append><el-button @click="executeSearch">搜索</el-button></template></el-input>
    <div class="category-filter"><el-button :type="categoryCode === null ? 'primary' : 'default'" round @click="categoryCode = null">全部</el-button><el-button v-for="category in categories" :key="category[0]" :type="categoryCode === category[0] ? 'primary' : 'default'" round @click="categoryCode = category[0]">{{ category[1] }}</el-button></div>
    <p v-if="loading" class="state-message">正在翻找……</p><p v-else-if="errorMessage" class="state-message error-message">{{ errorMessage }}</p><p v-else-if="keyword.trim() && !contents.length" class="state-message">这次没翻到合适的。</p>
    <section class="content-grid"><article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)"><p>{{ item.content }}</p><div class="card-footer"><div class="tag-row"><el-tag v-if="item.categoryName" size="small" type="warning" effect="plain">{{ item.categoryName }}</el-tag><el-tag v-for="tag in item.tags.slice(0, 3)" :key="tag.id" size="small" effect="plain">#{{ tag.name }}</el-tag></div><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div></article></section><el-pagination v-if="total > 20" class="pagination" background layout="prev, pager, next" :current-page="pageNum" :page-size="20" :total="total" @current-change="changePage" />
  </main>
</template>

<style scoped>
.search-page{width:min(1040px,calc(100% - 64px));margin:0 auto;padding:64px 0}.search-header{display:flex;align-items:flex-start;justify-content:space-between;gap:24px}.eyebrow{margin:0;color:var(--color-brand);font-size:12px;font-weight:700;letter-spacing:.14em}h1{margin:10px 0 28px;font-size:28px}.search-input{max-width:680px}.category-filter{display:flex;flex-wrap:wrap;gap:8px;margin:20px 0 24px}.state-message{margin:28px 0;color:var(--color-text-secondary);text-align:center}.error-message{color:var(--color-danger)}.content-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.content-card{padding:20px;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);cursor:pointer}.content-card:hover,.content-card:focus-visible{border-color:var(--color-brand);outline:none}.content-card p{margin:0 0 16px;line-height:1.7;white-space:pre-wrap}.card-footer{display:flex;align-items:center;justify-content:space-between;gap:8px}.tag-row{display:flex;flex-wrap:wrap;gap:6px}.pagination{justify-content:center;margin-top:28px}
</style>
