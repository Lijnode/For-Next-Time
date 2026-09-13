<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleUserRound, Plus, Search } from 'lucide-vue-next'
import { getContentList, getRecentContents, recordCopy, searchContents, type ContentListItem } from '../../api/content'

const router = useRouter()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const keyword = ref('')
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const total = ref(0)
const categories: Array<[string, string]> = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]
const isSearching = computed(() => Boolean(keyword.value.trim()))

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
  // 1. 按首页当前模式请求普通列表或关键词搜索结果。
  try {
    const page = isSearching.value
      ? await searchContents(keyword.value.trim(), pageNum.value, categoryCode.value)
      : await getContentList(pageNum.value, categoryCode.value)
    // 2. 用接口分页结果更新首页卡片区域。
    contents.value = page.list
    total.value = page.total
  } catch (error) {
    // 3. 请求失败时保留当前内容并提示用户。
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
  // 1. 清理空白关键词，避免发起无效搜索。
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) return
  // 2. 从第一页加载当前关键词和分类的结果。
  pageNum.value = 1
  await loadContentPage()
}

/**
 * 清空首页关键词并恢复当前分类下的普通内容列表。
 */
async function clearSearch() {
  // 1. 清空输入内容并将分页回到第一页。
  keyword.value = ''
  pageNum.value = 1
  // 2. 重新加载当前分类下的普通列表。
  await loadContentPage()
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
    <div class="home-actions">
      <RouterLink class="create-link" to="/contents/new"><Plus :size="17" />先留一句</RouterLink>
      <RouterLink class="profile-link" to="/profile" aria-label="我的"><CircleUserRound :size="21" /></RouterLink>
    </div>
    <section class="hero">
      <h1>这次先留着，下次一定用得上。</h1>
      <div class="search-entry">
        <el-input v-model="keyword" size="large" placeholder="找找现在用得上的。" clearable :prefix-icon="Search" @clear="clearSearch" @keyup.enter="searchOnHome">
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
    <div class="home-content-layout">
      <section class="all-content-section">
        <div class="section-heading"><div><h2>{{ isSearching ? '搜索结果' : '全部内容' }}</h2><p v-if="isSearching">“{{ keyword.trim() }}”共翻到 {{ total }} 条。</p><p v-else>共留下 {{ total }} 条，慢慢翻，总能翻到。</p></div><el-button v-if="isSearching" text type="primary" @click="clearSearch">返回全部</el-button></div>
        <div class="content-grid">
          <article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)">
            <div class="card-top"><span v-if="item.pinned" class="pinned-mark">已置顶</span><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div>
            <p class="content-text">{{ item.content }}</p>
            <div v-if="item.categoryName || item.tags.length" class="tag-row"><span v-if="item.categoryName" :class="['card-chip', 'category-chip', item.categoryCode ? 'category-' + item.categoryCode.toLowerCase() : '']">{{ item.categoryName }}</span><span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="card-chip tag-chip">#{{ tag.name }}</span></div>
            <div class="card-footer"><span>{{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '还没用过' }}</span><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div>
          </article>
        </div>
        <p v-if="!contents.length" class="empty-state">这里还空着，先留下一句。</p>
        <el-button v-if="!contents.length" class="empty-create" type="primary" @click="createContent">先留一句</el-button>
        <el-pagination v-if="total > 20" class="pagination" background layout="prev, pager, next" :current-page="pageNum" :page-size="20" :total="total" @current-change="changePage" />
      </section>
      <aside v-if="!isSearching" class="recent-panel">
        <div class="section-heading"><h2>最近使用</h2><p>最近真正拿出去用过的内容</p></div>
        <div v-if="recentContents.length" class="recent-list" aria-label="最近使用内容">
          <article v-for="(item, index) in recentContents.slice(0, 3)" :key="item.id" class="recent-quick-item" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)">
            <span class="recent-rank">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="recent-item-body">
              <p class="recent-content">{{ item.content }}</p>
              <div class="recent-item-footer"><span>{{ formatRecentTime(item.lastCopyTime) }} · {{ item.copyCount ? '已用 ' + item.copyCount + ' 次' : '刚刚用过' }}</span><div class="recent-item-actions"><span v-if="item.pinned" class="pinned-mark">已置顶</span><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div></div>
            </div>
          </article>
        </div>
        <p v-else class="recent-empty">最近还没用上这里的内容。</p>
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

.home-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
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
  border-radius: 14px;
  background: #fff1ec;
  color: var(--color-brand);
  text-decoration: none;
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

.recent-panel {
  padding-left: 22px;
  border-left: 1px solid var(--color-border);
}

.recent-list {
  margin-top: 17px;
}

.recent-quick-item {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr);
  gap: 10px;
  padding: 15px 0;
  border-bottom: 1px solid var(--color-border);
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.recent-content {
  display: -webkit-box;
  overflow: hidden;
  margin: 0 0 8px;
  color: var(--color-text-primary);
  font-size: 15px;
  line-height: 1.5;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.recent-item-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recent-item-footer :deep(.el-button) {
  padding: 4px 0;
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

.category-funny {
  background: #fff4d8;
  color: #9c6700;
}

.category-abstract {
  background: #f1edff;
  color: #715cac;
}

.category-emotion {
  background: #fff0f3;
  color: #b85f74;
}

.category-literary {
  background: #edf1ff;
  color: #556eae;
}

.category-healing {
  background: #edf8f0;
  color: #4d8662;
}

.category-sober {
  background: #edf5f8;
  color: #4f7e91;
}

.category-sarcasm {
  background: #f5effa;
  color: #865d9a;
}

.category-other {
  background: #f1f2ef;
  color: #666d6d;
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
  .home-content-layout {
    grid-template-columns: minmax(0, 1fr) 250px;
    gap: 28px;
  }

  .recent-panel {
    padding-left: 18px;
  }
}

@media (max-width: 900px) {
  .home-content-layout {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .recent-panel {
    padding: 0;
    border-top: 1px solid var(--color-border);
    border-left: 0;
  }

  .recent-panel .section-heading {
    padding-top: 28px;
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

  .recent-list {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}
</style>
