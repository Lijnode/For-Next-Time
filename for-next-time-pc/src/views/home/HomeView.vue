<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowUpDown, CircleUserRound, Plus, Search } from 'lucide-vue-next'
import { getContentList, getRecentContents, recordCopy, searchContents, type ContentListItem, type ContentSort } from '../../api/content'
import { getUserProfile, type UserProfile } from '../../api/user'

const router = useRouter()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const userProfile = ref<UserProfile>()
const keyword = ref('')
const submittedKeyword = ref('')
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const contentLoading = ref(false)
const pageNum = ref(1)
const total = ref(0)
const pageSize = 10
const sortBy = ref<ContentSort>('time')
const recentOffset = ref(0)
const recentLoadingMore = ref(false)
const recentHasMore = ref(true)
const recentPageSize = 10
const recentContentOverflow = ref<Record<number, boolean>>({})
const categories: Array<[string, string]> = [['ABSTRACT', '抽象'], ['CRITIQUE', '锐评'], ['SWEET', '嘴甜'], ['RELAXED', '松弛'], ['SOBER', '清醒'], ['ROMANTIC', '浪漫'], ['HEALING', '治愈'], ['FRAGILE', '破碎']]
const isSearching = computed(() => Boolean(submittedKeyword.value))
const activeCategoryName = computed(() => categories.find(([code]) => code === categoryCode.value)?.[1])
const contentSectionTitle = computed(() => isSearching.value ? '搜索结果' : activeCategoryName.value ? activeCategoryName.value + '内容' : '全部内容')
const contentSectionSummary = computed(() => isSearching.value
  ? `“${submittedKeyword.value}”共翻到 ${total.value} 条`
  : activeCategoryName.value ? `“${activeCategoryName.value}”共留下 ${total.value} 条` : `共留下 ${total.value} 条内容`)

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
 * 在普通列表的时间与热度排序之间切换，并回到第一页重新加载。
 */
async function toggleSort() {
  // 1. 切换到另一种固定排序方式。
  sortBy.value = sortBy.value === 'time' ? 'hot' : 'time'
  // 2. 回到第一页并请求对应排序的内容。
  pageNum.value = 1
  await loadContentPage()
}

/**
 * 加载当前分类与页码的内容分页结果。
 */
async function loadContentPage() {
  // 1. 标记内容区请求中，避免排序按钮被重复触发。
  contentLoading.value = true
  try {
    // 2. 按首页当前模式请求普通列表或关键词搜索结果。
    const page = isSearching.value
      ? await searchContents(submittedKeyword.value, pageNum.value, categoryCode.value, pageSize)
      : await getContentList(pageNum.value, categoryCode.value, pageSize, sortBy.value)
    // 3. 用接口分页结果更新首页卡片区域。
    contents.value = page.list
    total.value = page.total
  } catch (error) {
    // 4. 请求失败时保留当前内容并提示用户。
    ElMessage.error(error instanceof Error ? error.message : '加载内容失败，请稍后重试')
  } finally {
    // 5. 无论请求结果如何都恢复可交互状态。
    contentLoading.value = false
  }
}

/**
 * 复制卡片正文成功后记录一次使用统计，并刷新最近使用排行榜。
 */
async function copyContent(item: ContentListItem) {
  // 1. 在剪贴板写入成功前不更新后端统计。
  copyingId.value = item.id
  try {
    await navigator.clipboard.writeText(item.content)
    // 2. 复制完成后显示品牌化成功反馈。
    ElMessage.success('复制成功')
    // 3. 统计接口失败不影响已经成功的复制操作。
    try {
      const stat = await recordCopy(item.id)
      // 4. 先同步当前卡片统计，再刷新右侧最近使用排行榜。
      item.copyCount = stat.copyCount
      item.lastCopyTime = stat.lastCopyTime
      await loadRecentContents()
    } catch {
      // 5. 接受少量统计误差，避免自动重试导致重复计数。
    }
  } catch (error) {
    // 6. 仅在浏览器复制失败时提示用户。
    ElMessage.error(error instanceof Error ? error.message : '复制失败，请稍后重试')
  } finally {
    // 7. 恢复当前卡片的复制按钮状态。
    copyingId.value = null
  }
}

/**
 * 将最近复制时间转换为首页快速取用区的简短相对时间。
 */
function formatRecentTime(value: string | null) {
  // 1. 未记录复制时间时使用自然的兜底文案。
  if (!value) return '刚刚用过'
  // 2. 解析后端约定的本地日期时间，异常时避免向用户展示原始值。
  const copiedAt = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(copiedAt.getTime())) return '最近用过'
  // 3. 按时间间隔生成便于快速浏览的相对时间。
  const elapsedMinutes = Math.max(0, Math.floor((Date.now() - copiedAt.getTime()) / 60000))
  if (elapsedMinutes < 1) return '刚刚用过'
  if (elapsedMinutes < 60) return elapsedMinutes + ' 分钟前用过'
  if (elapsedMinutes < 1440) return Math.floor(elapsedMinutes / 60) + ' 小时前用过'
  return Math.floor(elapsedMinutes / 1440) + ' 天前用过'
}

/**
 * 打开用户选中的内容详情。
 */
async function openDetail(id: number) {
  // 1. 跳转至受保护的详情路由。
  await router.push({ name: 'content-detail', params: { id } })
}

/**
 * 在首页原地加载当前关键词对应的搜索结果。
 */
async function searchOnHome() {
  // 1. 清理输入关键词；空输入时恢复普通内容列表。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    await clearSearch()
    return
  }
  // 2. 仅在用户确认搜索后提交关键词并从第一页查询。
  submittedKeyword.value = normalizedKeyword
  pageNum.value = 1
  await loadContentPage()
}

/**
 * 清空首页关键词并恢复当前分类下的普通内容列表。
 */
async function clearSearch() {
  // 1. 清空输入内容、已提交关键词并将分页回到第一页。
  keyword.value = ''
  submittedKeyword.value = ''
  pageNum.value = 1
  // 2. 重新加载当前分类下的普通列表。
  await loadContentPage()
}

/**
 * 加载最近使用内容，并在滚动分页时追加下一批数据。
 */
async function loadRecentContents(append = false) {
  // 1. 避免重复请求，且没有下一页时停止加载。
  if (recentLoadingMore.value || (append && !recentHasMore.value)) return

  // 2. 根据加载方式确定本次分页偏移量。
  const offset = append ? recentOffset.value : 0
  recentLoadingMore.value = true

  try {
    // 3. 请求固定十条最近使用内容，并同步分页状态。
    const items = await getRecentContents(recentPageSize, offset)
    recentContents.value = append ? [...recentContents.value, ...items] : items
    recentOffset.value = offset + items.length
    recentHasMore.value = items.length === recentPageSize
  } finally {
    // 4. 请求结束后恢复下一次滚动加载能力。
    recentLoadingMore.value = false
  }
}

/**
 * 在最近使用列表滚动接近底部时加载下一页。
 */
function handleRecentScroll(event: Event) {
  // 1. 仅在滚动到列表底部附近时继续请求数据。
  const list = event.currentTarget as HTMLElement
  if (list.scrollTop + list.clientHeight < list.scrollHeight - 24) return

  // 2. 追加下一批十条最近使用内容。
  void loadRecentContents(true)
}

/**
 * 仅为被两行摘要截断的最近使用正文启用完整内容提示。
 */
async function refreshRecentContentOverflow() {
  // 1. 等待列表 DOM 更新完成后读取实际文本高度。
  await nextTick()

  // 2. 根据每项真实溢出状态更新提示开关。
  const overflow: Record<number, boolean> = {}
  document.querySelectorAll<HTMLElement>('.recent-content[data-content-id]').forEach((element) => {
    const id = Number(element.dataset.contentId)
    overflow[id] = element.scrollHeight > element.clientHeight
  })
  recentContentOverflow.value = overflow
}

/**
 * 加载首页内容列表，失败时保留空状态。
 */
async function loadContents() {
  // 1. 并行请求当前用户内容列表和最近成功复制内容。
  const [page] = await Promise.all([getContentList(pageNum.value, categoryCode.value, pageSize, sortBy.value), loadRecentContents()])
  // 2. 写入首页两个独立展示区域。
  contents.value = page.list
  total.value = page.total
}

/**
 * 读取当前用户资料，用于首页右上角展示已上传的头像。
 */
async function loadUserProfile() {
  // 1. 请求当前用户资料，未上传头像时保留空地址。
  try {
    userProfile.value = await getUserProfile()
  } catch {
    // 2. 资料加载失败时保留默认图标，不影响首页内容使用。
  }
}

// 1. 最近使用列表变更后，根据真实截断状态控制正文提示。
watch(recentContents, () => {
  void refreshRecentContentOverflow()
}, { flush: 'post' })

// 1. 进入首页后并行加载内容和当前用户资料。
onMounted(() => {
  void loadContents()
  void loadUserProfile()
})
</script>

<template>
  <main class="home-shell">
    <header class="home-header">
      <RouterLink class="brand-lockup" to="/" aria-label="下次一定首页">
        <img src="/brand-mark.png?v=4" alt="">
        <span>下次一定</span>
      </RouterLink>
      <div class="home-actions">
        <RouterLink class="create-link" to="/contents/new"><Plus :size="17" />先留一句</RouterLink>
        <RouterLink class="profile-link" to="/profile" aria-label="我的">
          <img v-if="userProfile?.avatarUrl" :src="userProfile.avatarUrl" :alt="`${userProfile.username} 的头像`">
          <span v-else-if="userProfile" class="profile-initial">{{ userProfile.username.slice(0, 1) }}</span>
          <CircleUserRound v-else :size="21" />
        </RouterLink>
      </div>
    </header>
    <section class="hero">
      <h1>这次先留着，下次一定用得上</h1>
      <div class="search-entry">
        <el-input v-model="keyword" size="large" placeholder="找找现在用得上的" clearable :prefix-icon="Search" @clear="clearSearch" @keyup.enter="searchOnHome">
          <template #suffix>
            <button class="search-button" type="button" aria-label="搜索" @click="searchOnHome"><Search :size="19" /></button>
          </template>
        </el-input>
      </div>
      <div class="category-filter" aria-label="按分类浏览">
        <button :class="{ active: categoryCode === null }" type="button" @click="selectCategory(null)">全部</button>
        <button v-for="category in categories" :key="category[0]" :class="{ active: categoryCode === category[0] }" type="button" @click="selectCategory(category[0])">{{ category[1] }}</button>
      </div>
    </section>
    <div :class="['home-content-layout', { 'search-layout': isSearching }]">
      <section class="all-content-section">
        <div class="section-heading"><div><h2>{{ contentSectionTitle }}</h2><p>{{ contentSectionSummary }}</p></div><el-button v-if="isSearching" text type="primary" @click="clearSearch">返回全部</el-button><el-button v-else class="sort-button" text :loading="contentLoading" :aria-label="sortBy === 'time' ? '切换为按热度' : '切换为按时间'" @click="toggleSort"><span class="sort-button-content"><ArrowUpDown :size="15" /><span>{{ sortBy === 'time' ? '按时间' : '按热度' }}</span></span></el-button></div>
        <div class="content-grid">
          <article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)">
            <div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div>
            <p class="content-text">{{ item.content }}</p>
            <div v-if="item.categoryName || item.tags.length" class="tag-row"><span v-if="item.categoryName" :class="['card-chip', 'category-chip', item.categoryCode ? 'category-' + item.categoryCode.toLowerCase() : '']">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="card-chip tag-chip">#{{ tag.name }}</span></div>
            <div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><el-button class="copy-button" text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div>
          </article>
        </div>
<!--        <el-button v-if="!contents.length" class="empty-create" type="primary" @click="createContent">先留一句</el-button>-->
        <el-pagination v-if="total > 0" class="pagination" background layout="prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="changePage" />
      </section>
      <aside v-if="!isSearching" class="recent-panel">
        <div class="section-heading"><h2>最近使用</h2></div>
        <div v-if="recentContents.length" class="recent-list" aria-label="最近使用内容" @scroll.passive="handleRecentScroll">
          <article v-for="(item, index) in recentContents" :key="item.id" class="recent-quick-item" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)">
            <span class="recent-rank">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="recent-item-body">
              <el-tooltip :content="item.content" effect="light" placement="top" :show-after="200" popper-class="recent-content-tooltip" :disabled="!recentContentOverflow[item.id]"><p class="recent-content" :data-content-id="item.id">{{ item.content }}</p></el-tooltip>
              <div class="recent-item-footer"><span>{{ formatRecentTime(item.lastCopyTime) }} · {{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '刚刚用过' }}</span><div class="recent-item-actions"><el-button class="copy-button" text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div></div>
            </div>
          </article>
          <p v-if="recentLoadingMore" class="recent-loading">加载中…</p>
        </div>
        <p v-else class="recent-empty">最近还没用上这里的内容</p>
      </aside>
    </div>
  </main>
</template>

<style scoped>
.home-shell {
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  padding: 24px 0 72px;
}

.home-header,
.home-actions {
  display: flex;
  align-items: center;
}

.home-header {
  justify-content: space-between;
}

.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--color-text-primary);
  font-size: 20px;
  font-weight: 750;
  letter-spacing: .03em;
  text-decoration: none;
}

.brand-lockup img {
  display: block;
  width: 48px;
  height: 48px;
}

.home-actions {
  gap: 18px;
}

.create-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 11px 16px;
  border-radius: 14px;
  background: var(--color-brand);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  box-shadow: 0 7px 15px rgb(255 111 78 / 18%);
}

.profile-link {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid #f0dfd9;
  border-radius: 50%;
  background: #fff1ec;
  color: var(--color-brand);
  overflow: hidden;
  text-decoration: none;
}

.profile-link img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-initial {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  background: var(--color-brand);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.hero {
  padding: 28px 0 14px;
  text-align: center;
}

h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: clamp(26px, 2.4vw, 32px);
  letter-spacing: .02em;
}

.search-entry {
  width: min(760px, 100%);
  margin: 18px auto 15px;
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

.search-entry :deep(.el-input__suffix-inner) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-entry :deep(.el-input__clear) {
  margin: 0;
}

.search-button {
  display: grid;
  flex: 0 0 auto;
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

.home-content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  align-items: start;
  gap: 40px;
  margin-top: 38px;
}

.section-heading h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 19px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-heading :deep(.el-button) {
  margin-top: 1px;
  flex-shrink: 0;
  font-weight: 700;
}

.section-heading p {
  margin: 7px 0 0;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 17px;
}

.search-layout {
  grid-template-columns: minmax(0, 1fr);
}

.search-layout .content-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.recent-panel {
  padding: 20px;
  border: 1px solid #f0dfd9;
  border-radius: 20px;
  background: #fff8f5;
  box-shadow: 0 8px 20px rgb(31 33 33 / 3%);
}

.recent-list {
  margin-top: 14px;
  height: 390px;
  padding-right: 4px;
  overflow-y: auto;
  scrollbar-width: none;
}

.sort-button {
  color: var(--color-text-secondary);
}

.sort-button-content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.recent-list::-webkit-scrollbar {
  display: none;
}

.recent-loading {
  margin: 10px 0 2px;
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
}

.recent-quick-item {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr);
  gap: 10px;
  padding: 15px 0;
  border-bottom: 1px solid #efdfda;
  cursor: pointer;
  transition: color .18s ease;
}

.recent-quick-item + .recent-quick-item {
  margin-top: 0;
}

.recent-quick-item:hover,
.recent-quick-item:focus-visible {
  color: var(--color-brand);
  outline: none;
}

.recent-rank {
  padding-top: 2px;
  color: #b6bcba;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -.02em;
  line-height: 1;
}

.recent-quick-item:first-child .recent-rank {
  color: #e9927d;
}

.recent-item-body {
  min-width: 0;
}

.recent-item-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.recent-item-footer > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-content {
  display: -webkit-box;
  overflow: hidden;
  margin: 0 0 8px;
  color: var(--color-text-primary);
  font-size: 15px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

:global(.el-popper.recent-content-tooltip) {
  max-width: min(420px, calc(100vw - 32px));
  max-height: min(320px, calc(100vh - 48px));
  padding: 12px 14px;
  overflow: auto;
  border: 1px solid #f0dfd9;
  border-radius: 12px;
  background: #fffaf7;
  box-shadow: 0 10px 28px rgb(72 48 40 / 14%);
  color: var(--color-text-primary);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
  scrollbar-width: none;
  white-space: pre-wrap;
}

:global(.el-popper.recent-content-tooltip::-webkit-scrollbar) {
  display: none;
}

:global(.el-popper.recent-content-tooltip .el-popper__arrow::before) {
  border-color: #f0dfd9;
  background: #fffaf7;
}

.recent-item-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
  white-space: nowrap;
}

.recent-item-footer :deep(.copy-button) {
  padding: 5px 9px;
  border-radius: 10px;
  font-weight: 700;
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
  overflow-wrap: anywhere;
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
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.category-chip {
  border: 1px solid transparent;
}

.tag-chip {
  border: 1px solid #e1e5e2;
  background: #fff;
  color: #6f7575;
  font-weight: 500;
}

.category-abstract {
  background: #fff8dc;
  color: #d9a514;
}

.category-critique {
  background: #fff0f0;
  color: #e5484d;
}

.category-sweet {
  background: #fff0f7;
  color: #e46aa3;
}

.category-relaxed {
  background: #eef8f2;
  color: #4faf7b;
}

.category-healing {
  background: #fff3e8;
  color: #ed8b4a;
}

.category-sober {
  background: #eef4ff;
  color: #4c78d0;
}

.category-romantic {
  background: #f4efff;
  color: #8a63d2;
}

.category-fragile {
  background: #f1f0f3;
  color: #514c59;
}

.card-footer {
  margin-top: 15px;
}

.card-footer :deep(.copy-button) {
  padding: 5px 9px;
  border-radius: 10px;
  font-weight: 700;
}

:deep(.copy-button:hover:not(.is-disabled)) {
  background: #fff1ec;
  color: #f55f3c;
}

.recent-empty {
  margin: 17px 0 0;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.empty-create {
  margin-top: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next),
.pagination :deep(.el-pager li) {
  min-width: 34px;
  height: 34px;
  margin: 0 3px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: #fff;
  color: var(--color-text-secondary);
  font-weight: 600;
}

.pagination :deep(.btn-prev:hover),
.pagination :deep(.btn-next:hover),
.pagination :deep(.el-pager li:hover) {
  border-color: #ffb29f;
  color: var(--color-brand);
}

.pagination :deep(.el-pager li.is-active) {
  border-color: var(--color-brand);
  background: var(--color-brand);
  color: #fff;
  box-shadow: 0 5px 12px rgb(255 111 78 / 20%);
}

@media (max-width: 1180px) {
  .home-content-layout {
    grid-template-columns: minmax(0, 1fr) 250px;
    gap: 28px;
  }

  .recent-panel {
    padding: 18px;
  }
}

@media (max-width: 900px) {
  .home-content-layout {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .recent-panel {
    padding: 20px;
  }

  .search-layout .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recent-panel .section-heading {
    padding-top: 0;
  }

  .recent-list {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
  }

  .recent-quick-item,
  .recent-quick-item + .recent-quick-item {
    padding: 15px 0;
  }
}

@media (max-width: 680px) {
  .home-shell {
    width: min(100% - 32px, 1240px);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .search-layout .content-grid {
    grid-template-columns: 1fr;
  }

  .recent-list {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}
</style>
