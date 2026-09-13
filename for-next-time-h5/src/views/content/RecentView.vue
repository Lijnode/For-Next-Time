<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Clock3 } from 'lucide-vue-next'
import { getRecentContents, recordCopy, type ContentListItem } from '../../api/content'
import { copyText } from '../../platform/clipboard'
import { showError, showSuccess } from '../../platform/feedback'

const router = useRouter()
const recentContents = ref<ContentListItem[]>([])
const loading = ref(true)
const copyingId = ref<number | null>(null)

/**
 * 加载当前用户最近成功复制过的内容。
 */
async function loadRecentContents() {
  // 1. 进入独立历史页后再请求最近使用数据，避免首页首屏冗余加载。
  loading.value = true
  try {
    recentContents.value = await getRecentContents()
  } catch (error) {
    // 2. 请求失败时保留空列表并给出可理解的反馈。
    showError(error instanceof Error ? error.message : '加载最近使用失败，请稍后重试')
  } finally {
    // 3. 无论请求结果如何都结束页面加载状态。
    loading.value = false
  }
}

/**
 * 格式化最近成功复制的时间，优先呈现相对时间。
 */
function formatRecentTime(value: string | null) {
  // 1. 没有成功复制时间时显示兜底文案。
  if (!value) return '刚刚留下'
  // 2. 兼容后端 yyyy-MM-dd HH:mm:ss 格式并计算与当前时间的差值。
  const date = new Date(value.replace(' ', 'T'))
  const diff = Date.now() - date.getTime()
  if (Number.isNaN(date.getTime()) || diff < 60_000) return '刚刚用过'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前用过`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前用过`
  return value.slice(0, 10)
}

/**
 * 复制历史内容并在成功后同步后端使用统计。
 */
async function copyContent(item: ContentListItem) {
  // 1. 先完成平台剪贴板复制，失败时不请求后端统计接口。
  copyingId.value = item.id
  try {
    await copyText(item.content)
    // 2. 告知用户复制成功，再异步同步本次使用统计。
    showSuccess('就是这句')
    try {
      const stat = await recordCopy(item.id)
      item.copyCount = stat.copyCount
      item.lastCopyTime = stat.lastCopyTime
    } catch {
      // 3. 统计同步失败不影响已完成的复制，避免重试造成重复计数。
    }
  } catch (error) {
    // 4. 仅在剪贴板操作失败时提示用户。
    showError(error instanceof Error ? error.message : '复制失败，请稍后重试')
  } finally {
    // 5. 结束当前条目的复制加载状态。
    copyingId.value = null
  }
}

/**
 * 打开历史列表中用户选择的内容详情。
 */
async function openDetail(id: number) {
  // 1. 进入当前登录用户可访问的内容详情页面。
  await router.push({ name: 'content-detail', params: { id } })
}

// 1. 页面进入时按需加载最近使用数据。
onMounted(loadRecentContents)
</script>

<template>
  <main class="recent-page">
    <header class="recent-header"><h1>最近使用</h1></header>

    <section class="recent-content" aria-label="最近使用内容">
      <van-loading v-if="loading" class="loading-state" vertical>正在翻找最近用过的内容</van-loading>
      <template v-else>
        <p v-if="!recentContents.length" class="empty-state">最近还没用上这里的内容。</p>
        <div v-else class="recent-list">
          <article v-for="item in recentContents" :key="item.id" class="recent-item" @click="openDetail(item.id)">
            <div class="item-heading"><span class="item-time"><Clock3 :size="14" />{{ formatRecentTime(item.lastCopyTime) }}</span><span v-if="item.pinned" class="pinned-mark">已置顶</span></div>
            <p class="item-content">{{ item.content }}</p>
            <footer class="item-footer"><span>已用 {{ item.copyCount }} 次</span><van-button plain size="small" type="primary" :loading="copyingId === item.id" @click.stop="copyContent(item)">复制</van-button></footer>
          </article>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.recent-page { min-height: 100vh; background: var(--color-page); }
.recent-header { padding: 18px 16px 14px; border-bottom: 1px solid var(--color-border); background: #fffaf7; }
h1 { margin: 0; color: var(--color-text-primary); font-size: 20px; letter-spacing: .02em; }
.recent-content { padding: 20px 16px 30px; }.loading-state { padding-top: 88px; color: var(--color-muted); }.empty-state { margin: 48px 0; color: var(--color-muted); font-size: 14px; text-align: center; }
.recent-list { border-top: 1px solid var(--color-border); }.recent-item { padding: 17px 0 16px; border-bottom: 1px solid var(--color-border); cursor: pointer; }.item-heading, .item-footer { display: flex; align-items: center; }.item-time, .item-footer { color: var(--color-muted); font-size: 11px; }.item-time { display: inline-flex; gap: 4px; align-items: center; }.pinned-mark { margin-left: auto; color: var(--color-brand); font-size: 11px; font-weight: 700; }.item-content { display: -webkit-box; overflow: hidden; margin: 11px 0; color: var(--color-text-primary); font-size: 16px; line-height: 1.65; white-space: pre-wrap; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.item-footer { justify-content: space-between; margin-top: 13px; }.item-footer :deep(.van-button) { min-width: 68px; height: 38px; border-radius: 12px; font-weight: 700; }
</style>
