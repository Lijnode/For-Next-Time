<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { getContentList, getRecentContents, recordCopy, type ContentListItem } from '../../api/content'

const router = useRouter()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const keyword = ref('')
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const total = ref(0)
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
  // 1. 保存用户选择的固定分类编码。
  categoryCode.value = value
  // 2. 切换分类后回到第一页，避免页码超出筛选结果范围。
  pageNum.value = 1
  await loadContentPage()
}

/**
 * 切换桌面端分页器页码，并保持当前分类筛选条件。
 */
async function changePage(value: number) {
  // 1. 写入用户选择的新页码。
  pageNum.value = value
  // 2. 使用当前分类重新请求对应分页数据。
  await loadContentPage()
}

/**
 * 加载当前分类与页码的内容分页结果。
 */
async function loadContentPage() {
  // 1. 请求当前用户范围内的指定内容页。
  try {
    const page = await getContentList(pageNum.value, categoryCode.value)
    contents.value = page.list
    total.value = page.total
  } catch (error) {
    // 2. 请求失败时保留当前内容并提示用户。
    ElMessage.error(error instanceof Error ? error.message : '加载内容失败，请稍后重试')
  }
}

/**
 * 复制卡片正文成功后记录一次使用统计。
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
    // 6. 恢复当前卡片的复制按钮状态。
    copyingId.value = null
  }
}

/**
 * 打开用户选中的内容详情。
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
  total.value = page.total
  recentContents.value = recent
}

// 1. 进入首页后加载内容列表。
onMounted(loadContents)
</script>

<template>
  <main class="home-shell">
    <section class="hero">
      <p class="eyebrow">FOR NEXT TIME</p>
      <h1>这次先留着，下次一定用得上。</h1>
      <p class="hero-description">以前留下的那些句子，需要的时候，都应该找得回来。</p>
      <div class="search-entry">
        <el-input v-model="keyword" size="large" placeholder="找找现在用得上的。" clearable :prefix-icon="Search" @keyup.enter="openSearch">
          <template #suffix>
            <button class="search-button" type="button" aria-label="搜索" @click="openSearch"><Search :size="19" /></button>
          </template>
        </el-input>
      </div>
      <div class="category-filter" aria-label="按分类浏览">
        <button :class="{ active: categoryCode === null }" type="button" @click="selectCategory(null)">全部</button>
        <button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="selectCategory(category[0])">{{ category[1] }}</button>
      </div>
    </section>
    <section class="content-section recent-section">
      <div class="section-heading">
        <div><h2>最近使用</h2><p>最近真正拿出去用过的内容</p></div>
      </div>
      <div v-if="recentContents.length" class="content-grid">
        <article v-for="item in recentContents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)">
          <div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime || '刚刚留下' }}</span></div>
          <p class="content-text">{{ item.content }}</p>
          <div class="tag-row"><span v-if="item.categoryName" class="card-chip category-chip">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="card-chip">#{{ tag.name }}</span></div>
          <div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div>
        </article>
      </div>
      <p v-else class="recent-empty">最近还没用上这里的内容。</p>
    </section>
    <section class="content-section all-content-section">
      <div class="section-heading"><div><h2>全部内容</h2><p>共留下 {{ total }} 条，慢慢翻，总能翻到。</p></div></div>
      <div class="content-grid">
        <article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)">
          <div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div>
          <p class="content-text">{{ item.content }}</p>
          <div class="tag-row"><span v-if="item.categoryName" class="card-chip category-chip">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="card-chip">#{{ tag.name }}</span></div>
          <div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div>
        </article>
      </div>
      <p v-if="!contents.length" class="empty-state">这里还空着，先留下一句。</p>
      <el-button v-if="!contents.length" class="empty-create" type="primary" @click="createContent">先留一句</el-button>
      <el-pagination v-if="total > 20" class="pagination" background layout="prev, pager, next" :current-page="pageNum" :page-size="20" :total="total" @current-change="changePage" />
    </section>
  </main>
</template>

<style scoped>
.home-shell {
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  padding: 34px 0 72px;
}

.hero {
  padding: 31px 0 18px;
  text-align: center;
}

.eyebrow {
  margin: 0;
  color: var(--color-brand);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .2em;
}

h1 {
  margin: 13px 0 0;
  color: var(--color-text-primary);
  font-size: clamp(29px, 3vw, 36px);
  letter-spacing: .02em;
}

.hero-description {
  margin: 13px 0 0;
  color: var(--color-text-secondary);
  font-size: 15px;
}

.search-entry {
  width: min(760px, 100%);
  margin: 24px auto 15px;
}

.search-entry :deep(.el-input__wrapper) {
  min-height: 54px;
  padding: 0 8px 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 5px 16px rgb(31 33 33 / 4%);
}

.search-entry :deep(.el-input__wrapper.is-focus) {
  border-color: #ffb29f;
  box-shadow: 0 0 0 3px rgb(255 111 78 / 10%);
}

.search-button {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 0;
  border-radius: 13px;
  background: var(--color-brand);
  color: #fff;
}

.category-filter {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 9px;
}

.category-filter button {
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.category-filter button:hover,
.category-filter button.active {
  border-color: #1f2121;
  background: #1f2121;
  color: #fff;
}

.content-section {
  margin-top: 38px;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 19px;
}

.section-heading p {
  margin: 7px 0 0;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 17px;
}

.content-card {
  display: flex;
  min-height: 196px;
  flex-direction: column;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: 0 7px 18px rgb(31 33 33 / 3%);
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
}

.content-card:hover,
.content-card:focus-visible {
  border-color: #ffc1b1;
  outline: none;
  box-shadow: 0 12px 25px rgb(31 33 33 / 7%);
  transform: translateY(-3px);
}

.card-top,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.pinned-mark {
  color: var(--color-brand);
  font-weight: 700;
}

.content-text {
  display: -webkit-box;
  flex: 1;
  overflow: hidden;
  margin: 17px 0;
  color: var(--color-text-primary);
  font-size: 16px;
  line-height: 1.75;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.card-chip {
  padding: 5px 9px;
  border-radius: 999px;
  background: #f1f2ef;
  color: #737878;
  font-size: 12px;
  line-height: 1;
}

.category-chip {
  background: #fff1ec;
  color: #cf5a41;
}

.card-footer {
  margin-top: 15px;
}

.card-footer :deep(.el-button) {
  padding: 5px 0;
  font-weight: 700;
}

.recent-empty,
.empty-state {
  margin: 17px 0 0;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.empty-create {
  margin-top: 14px;
}

.pagination {
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .home-shell {
    width: min(100% - 32px, 1240px);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
