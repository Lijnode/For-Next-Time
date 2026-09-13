<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContentList, getRecentContents, recordCopy, type ContentListItem } from '../../api/content'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const contents = ref<ContentListItem[]>([])
const recentContents = ref<ContentListItem[]>([])
const keyword = ref('')
const categoryCode = ref<string | null>(null)
const copyingId = ref<number | null>(null)
const pageNum = ref(1)
const total = ref(0)
const categories: Array<[string, string]> = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]

/**
 * 清除登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与本地存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护页面。
  await router.replace({ name: 'login' })
}

/**
 * 进入新增内容页面。
 */
async function createContent() {
  // 1. 进入受保护的内容编辑页。
  await router.push({ name: 'content-create' })
}

/**
 * 进入当前登录用户的个人资料页。
 */
async function openProfile() {
  // 1. 进入受保护的个人资料路由。
  await router.push({ name: 'profile' })
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
    <section class="welcome-card">
      <p class="eyebrow">For Next Time</p>
      <h1>下次一定</h1>
      <p>这次先留着，下次一定用得上。</p>
      <div class="search-entry"><el-input v-model="keyword" placeholder="找找现在用得上的。" clearable @keyup.enter="openSearch"><template #append><el-button @click="openSearch">搜索</el-button></template></el-input></div>
      <el-button type="primary" @click="createContent">先留一句</el-button>
      <el-button text type="primary" @click="openProfile">我的</el-button>
      <el-button text type="primary" @click="logout">退出登录</el-button>
    </section>
    <section v-if="recentContents.length" class="recent-section"><h2>最近使用</h2><div class="recent-list"><button v-for="item in recentContents" :key="item.id" type="button" @click="openDetail(item.id)">{{ item.content }}</button></div></section>
    <section class="all-content-section"><div class="section-heading"><h2>全部内容</h2><div class="category-filter"><el-button :type="categoryCode === null ? 'primary' : 'default'" size="small" round @click="selectCategory(null)">全部</el-button><el-button v-for="category in categories" :key="category[0]" :type="categoryCode === category[0] ? 'primary' : 'default'" size="small" round @click="selectCategory(category[0])">{{ category[1] }}</el-button></div></div><div class="content-grid"><article v-for="item in contents" :key="item.id" class="content-card" tabindex="0" @click="openDetail(item.id)" @keydown.enter="openDetail(item.id)"><div class="card-top"><el-tag v-if="item.pinned" size="small" type="warning" effect="plain">置顶</el-tag><span>{{ item.lastCopyTime ? '最近用过' : item.updateTime }}</span></div><p>{{ item.content }}</p><div class="card-footer"><div class="tag-row"><el-tag v-if="item.categoryName" size="small" type="warning" effect="plain">{{ item.categoryName }}</el-tag><el-tag v-for="tag in item.tags.slice(0, 3)" :key="tag.id" size="small" effect="plain">#{{ tag.name }}</el-tag></div><el-button text type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</el-button></div></article></div><p v-if="!contents.length">这里还空着，先留下一句。</p><el-pagination v-if="total > 20" class="pagination" background layout="prev, pager, next" :current-page="pageNum" :page-size="20" :total="total" @current-change="changePage" /></section>
  </main>
</template>

<style scoped>
.home-shell {
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  padding: 80px 0;
}

.welcome-card {
  padding: 48px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: 0 4px 18px rgb(20 24 24 / 4%);
}

.eyebrow {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
}
.search-entry { width: min(560px, 100%); margin: 28px 0 18px; }
.recent-section,.all-content-section{margin-top:32px}.recent-section h2,.all-content-section h2{font-size:18px}.recent-list{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.recent-list button{overflow:hidden;padding:14px;border:1px solid var(--color-border);border-radius:var(--radius-md);background:var(--color-surface);color:var(--color-text-secondary);text-align:left;text-overflow:ellipsis;white-space:nowrap;cursor:pointer}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}.category-filter{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:4px}

h1 {
  margin: var(--space-2) 0;
  font-size: 32px;
}

p {
  color: var(--color-text-secondary);
}
.content-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-top:20px}.content-card{padding:20px;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-lg);cursor:pointer}.content-card:hover,.content-card:focus-visible{border-color:var(--color-brand);outline:none}.content-card p{min-height:76px;white-space:pre-wrap}.card-top,.card-footer,.tag-row{display:flex;align-items:center;gap:8px}.card-top{justify-content:space-between;color:var(--color-text-tertiary);font-size:12px}.card-footer{justify-content:space-between}.tag-row{flex-wrap:wrap}.pagination{justify-content:center;margin-top:28px}
</style>
