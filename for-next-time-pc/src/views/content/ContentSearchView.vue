<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchContents, type ContentListItem } from '../../api/content'

const route = useRoute()
const router = useRouter()
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const categoryCode = ref<string | null>(null)
const contents = ref<ContentListItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
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
    return
  }
  // 2. 调用统一搜索接口并记录可读错误状态。
  loading.value = true
  errorMessage.value = ''
  try {
    contents.value = await searchContents(normalizedKeyword, categoryCode.value)
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
    <section class="content-grid"><article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)"><p>{{ item.content }}</p><div class="tag-row"><el-tag v-if="item.categoryName" size="small" type="warning" effect="plain">{{ item.categoryName }}</el-tag><el-tag v-for="tag in item.tags" :key="tag.id" size="small" effect="plain">#{{ tag.name }}</el-tag></div></article></section>
  </main>
</template>

<style scoped>
.search-page{width:min(1040px,calc(100% - 64px));margin:0 auto;padding:64px 0}.search-header{display:flex;align-items:flex-start;justify-content:space-between;gap:24px}.eyebrow{margin:0;color:var(--color-brand);font-size:12px;font-weight:700;letter-spacing:.14em}h1{margin:10px 0 28px;font-size:28px}.search-input{max-width:680px}.category-filter{display:flex;flex-wrap:wrap;gap:8px;margin:20px 0 24px}.state-message{margin:28px 0;color:var(--color-text-secondary);text-align:center}.error-message{color:var(--color-danger)}.content-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.content-card{padding:20px;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);cursor:pointer}.content-card:hover,.content-card:focus-visible{border-color:var(--color-brand);outline:none}.content-card p{margin:0 0 16px;line-height:1.7;white-space:pre-wrap}.tag-row{display:flex;flex-wrap:wrap;gap:6px}
</style>
