<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'
import { getContentList, recordCopy, type ContentListItem } from '../../api/content'
import { copyText } from '../../platform/clipboard'
import { showError, showSuccess } from '../../platform/feedback'

const router = useRouter()
const contents = ref<ContentListItem[]>([])
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const categories: Array<[string, string]> = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]

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
  // 1. 没有下一页时结束 Vant List 的本次加载状态。
  if (!hasMore.value) {
    loadingMore.value = false
    return
  }
  // 2. 由 Vant List 在接近底部时触发下一页请求。
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
    showSuccess('就是这句')
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
 * 从首页进入独立的内容搜索页。
 */
async function openSearchPage() {
  // 1. 打开不含底部导航的搜索任务页。
  await router.push({ name: 'content-search' })
}

/**
 * 加载首页内容列表，失败时保留空状态。
 */
async function loadContents() {
  // 1. 请求首页首屏需要的当前用户内容列表。
  const page = await getContentList(pageNum.value, categoryCode.value)
  // 2. 写入首页内容列表与分页状态。
  contents.value = page.list
  hasMore.value = page.pageNum < page.pages
}

// 1. 进入首页后加载内容列表。
onMounted(loadContents)
</script>

<template>
  <main class="home-page">
    <header class="home-head">
      <div class="brand-row">
        <div class="brand-area"><span class="brand-mark" aria-hidden="true">次</span><div><h1>下次一定</h1><p>先留下，找得到，用得上。</p></div></div>
        <button class="search-entry" type="button" aria-label="搜索内容" @click="openSearchPage"><Search :size="21" /></button>
      </div>
      <div class="category-scroller" aria-label="按分类浏览">
        <button :class="{ active: categoryCode === null }" type="button" @click="selectCategory(null)">全部</button>
        <button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="selectCategory(category[0])">{{ category[1] }}</button>
      </div>
    </header>
    <div class="home-content">
      <section class="content-section all-content-section">
        <van-list v-model:loading="loadingMore" :finished="!hasMore" finished-text="" :immediate-check="false" :offset="120" class="content-list" @load="loadMore">
          <article v-for="item in contents" :key="item.id" class="content-card" @click="openDetail(item.id)">
            <div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div>
            <p class="content-text">{{ item.content }}</p>
            <div class="tag-row"><span v-if="item.categoryName" :class="['card-chip', 'category-chip', item.categoryCode ? 'category-' + item.categoryCode.toLowerCase() : '']">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="card-chip">#{{ tag.name }}</span></div>
            <div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></div>
          </article>
        </van-list>
        <p v-if="!contents.length" class="empty-state">这里还空着，先留下一句。</p>
      </section>
    </div>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--color-page);
}

.home-head {
  padding: 20px 16px 12px;
  background: linear-gradient(180deg, #fff7f3 0%, var(--color-page) 100%);
}

.brand-row,
.brand-area,
.card-top,
.card-footer {
  display: flex;
  align-items: center;
}

.brand-row,
.card-top,
.card-footer {
  justify-content: space-between;
}
.brand-area { gap: 11px; }
.brand-mark { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 14px; background: var(--color-brand); color: #fff; font-size: 18px; font-weight: 800; box-shadow: 0 5px 12px rgb(255 111 78 / 22%); }
h1 { margin: 0; color: var(--color-text-primary); font-size: 22px; letter-spacing: .04em; }.brand-area p { margin: 4px 0 0; color: var(--color-muted); font-size: 12px; }
.search-entry { display: grid; width: 44px; height: 44px; padding: 0; place-items: center; border: 1px solid #f0dfd9; border-radius: 15px; background: #fff1ec; color: var(--color-brand); }

.category-scroller {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin: 16px -16px -2px;
  padding: 0 16px 4px;
  scrollbar-width: none;
}

.category-scroller::-webkit-scrollbar {
  display: none;
}

.category-scroller button {
  flex: 0 0 auto;
  padding: 7px 12px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: #fff;
  color: var(--color-muted);
  font-size: 12px;
}

.category-scroller button.active {
  border-color: #1f2121;
  background: #1f2121;
  color: #fff;
}

.home-content {
  padding: 0 16px 24px;
}

.content-section {
  margin-top: 25px;
}

.content-list {
  margin-top: 0;
}

.content-card {
  display: flex;
  min-height: 174px;
  flex-direction: column;
  margin-bottom: 12px;
  padding: 17px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-card);
  box-shadow: 0 6px 16px rgb(31 33 33 / 3%);
  cursor: pointer;
}

.card-top,
.card-footer {
  color: var(--color-muted);
  font-size: 11px;
}

.pinned-mark {
  color: var(--color-brand);
  font-weight: 700;
}

.content-text {
  display: -webkit-box;
  flex: 1;
  overflow: hidden;
  margin: 14px 0;
  color: var(--color-text-primary);
  font-size: 16px;
  line-height: 1.72;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.card-chip {
  padding: 5px 8px;
  border-radius: 999px;
  background: #f1f2ef;
  color: #737878;
  font-size: 11px;
  line-height: 1;
}


.card-footer {
  margin-top: 13px;
}

.card-footer :deep(.van-button) {
  min-width: 68px;
  height: 40px;
  border-radius: 12px;
  font-weight: 700;
}

.empty-state {
  margin: 13px 0 0;
  color: var(--color-muted);
  font-size: 13px;
}

</style>
