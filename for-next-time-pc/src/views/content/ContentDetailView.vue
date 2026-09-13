<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteContent, getContentDetail, recordCopy, setPinned, type ContentDetail } from '../../api/content'
import { ArrowLeft, Copy, Pencil, Pin, Trash2 } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const detail = ref<ContentDetail | null>(null)
const loading = ref(true)
const deleting = ref(false)
const copying = ref(false)
const pinning = ref(false)
const errorMessage = ref('')
const copyMessage = ref('')

/**
 * 加载路由指定的当前用户内容详情。
 */
async function loadDetail() {
  // 1. 校验路由参数，避免向接口传递无效内容主键。
  const id = Number(route.params.id)
  if (!Number.isInteger(id) || id < 1) {
    errorMessage.value = '内容不存在或无权操作'
    loading.value = false
    return
  }
  // 2. 请求详情并在失败时保留可读错误状态。
  try {
    detail.value = await getContentDetail(id)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载内容失败，请稍后重试'
  } finally {
    // 3. 请求结束后关闭加载状态。
    loading.value = false
  }
}

/**
 * 跳转到当前内容的编辑页。
 */
async function editContent() {
  // 1. 使用当前详情的内容主键进入共用表单编辑模式。
  await router.push({ name: 'content-edit', params: { id: route.params.id } })
}

/**
 * 先复制正文到剪贴板，成功后再记录复制统计。
 */
async function copyContent() {
  // 1. 复制正文失败时不请求后端统计接口。
  if (!detail.value) return
  copying.value = true
  copyMessage.value = ''
  try {
    await navigator.clipboard.writeText(detail.value.content)
    copyMessage.value = '就是这句。'
    // 2. 复制成功后才记录统计；统计失败不影响复制成功反馈。
    try {
      const stat = await recordCopy(detail.value.id)
      detail.value.copyCount = stat.copyCount
      detail.value.lastCopyTime = stat.lastCopyTime
    } catch {
      // 3. 接受少量统计误差，避免自动重试造成重复计数。
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '复制失败，请稍后重试'
  } finally {
    // 4. 无论成功或失败都恢复复制按钮状态。
    copying.value = false
  }
}

/**
 * 切换当前内容的明确置顶状态。
 */
async function togglePinned() {
  // 1. 读取当前状态并请求相反的明确置顶状态。
  if (!detail.value) return
  pinning.value = true
  try {
    const result = await setPinned(detail.value.id, !detail.value.pinned)
    detail.value.pinned = result.pinned
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '置顶设置失败，请稍后重试'
  } finally {
    // 2. 请求结束后恢复置顶按钮状态。
    pinning.value = false
  }
}

/**
 * 确认后逻辑删除当前内容并回到首页。
 */
async function removeContent() {
  // 1. 锁定删除按钮，避免重复发送删除请求。
  deleting.value = true
  try {
    // 2. 请求删除成功后替换为首页路由。
    await deleteContent(Number(route.params.id))
    await router.replace({ name: 'home' })
  } catch (error) {
    // 3. 在当前页展示可读的删除失败原因。
    errorMessage.value = error instanceof Error ? error.message : '删除失败，请稍后重试'
  } finally {
    // 4. 无论成功或失败都恢复删除按钮状态。
    deleting.value = false
  }
}

// 1. 页面首次进入时读取当前内容详情。
onMounted(loadDetail)
</script>

<template>
  <main class="detail-page">
    <section v-if="loading" class="state-card">正在翻找这句话……</section>
    <section v-else-if="errorMessage" class="state-card">
      <p>{{ errorMessage }}</p>
      <el-button type="primary" @click="router.replace({ name: 'home' })">回到首页</el-button>
    </section>
    <template v-else-if="detail">
      <header class="page-title-bar">
        <button type="button" aria-label="返回" @click="router.back()"><ArrowLeft :size="20" /></button>
        <h1>内容详情</h1>
        <span aria-hidden="true"></span>
      </header>
    <article class="detail-card">
      <p v-if="detail.pinned" class="pinned-mark"><Pin :size="14" fill="currentColor" />已置顶</p>
      <p class="content-text">{{ detail.content }}</p>
      <div class="tag-row"><span v-if="detail.categoryName" class="tag category-tag">{{ detail.categoryName }}</span><span v-for="tag in detail.tags" :key="tag.id" class="tag">#{{ tag.name }}</span></div>
      <div class="detail-actions">
        <el-button type="primary" :loading="copying" @click="copyContent"><Copy :size="16" />复制</el-button>
        <el-button :loading="pinning" @click="togglePinned"><Pin :size="16" />{{ detail.pinned ? '取消置顶' : '置顶' }}</el-button>
        <el-button @click="editContent"><Pencil :size="16" />编辑</el-button>
        <el-popconfirm title="确定删除这句话吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="removeContent"><template #reference><el-button class="delete-button" text type="danger" :loading="deleting"><Trash2 :size="16" />删除</el-button></template></el-popconfirm>
      </div>
      <p v-if="copyMessage" class="copy-message">{{ copyMessage }}</p>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <div class="info-grid">
        <div><span>分类</span><strong>{{ detail.categoryName ?? '未分类' }}</strong></div>
        <div><span>已用</span><strong>{{ detail.copyCount }} 次</strong></div>
        <div><span>最近使用</span><strong>{{ detail.lastCopyTime ?? '还没有复制过' }}</strong></div>
        <div><span>创建时间</span><strong>{{ detail.createTime }}</strong></div>
        <div><span>修改时间</span><strong>{{ detail.updateTime }}</strong></div>
      </div>
      <section v-if="detail.remark" class="remark-block"><p>备注</p><span>{{ detail.remark }}</span></section>
    </article>
    </template>
  </main>
</template>

<style scoped>
.detail-page { width: min(760px, calc(100% - 64px)); margin: 0 auto; padding: 32px 0 72px; }
.detail-card, .state-card { padding: 30px; border: 1px solid var(--color-border); border-radius: 22px; background: var(--color-surface); box-shadow: 0 10px 25px rgb(31 33 33 / 4%); }
.state-card { color: var(--color-text-secondary); text-align: center; }
.page-title-bar { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; margin-bottom: 18px; }
.page-title-bar button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); }
.page-title-bar h1 { margin: 0; color: var(--color-text-primary); font-size: 20px; text-align: center; }
.pinned-mark { display: flex; align-items: center; gap: 5px; margin: 0; color: var(--color-brand); font-size: 13px; font-weight: 700; }
.content-text { margin: 16px 0 22px; color: var(--color-text-primary); font-size: 25px; line-height: 1.85; white-space: pre-wrap; }
.tag-row { display: flex; flex-wrap: wrap; gap: 8px; }.tag { padding: 6px 10px; border-radius: 999px; background: #f1f2ef; color: #737878; font-size: 12px; }.category-tag { background: #fff1ec; color: #cf5a41; }
.detail-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 25px; }.detail-actions :deep(.el-button) { display: inline-flex; align-items: center; gap: 6px; min-height: 40px; border-radius: 12px; font-weight: 700; }.detail-actions .delete-button { margin-left: auto; color: var(--color-text-tertiary); }
.remark-block { margin-top: 16px; padding: 18px; border: 1px solid var(--color-border); border-radius: 16px; background: #fafaf8; }.remark-block p { margin: 0 0 8px; color: var(--color-text-tertiary); font-size: 12px; font-weight: 700; }.remark-block span { color: var(--color-text-secondary); line-height: 1.75; white-space: pre-wrap; }
.error-message, .copy-message { margin: 16px 0 0; font-size: 14px; }.error-message { color: var(--color-danger); }.copy-message { color: var(--color-brand); }
.info-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 24px; }.info-grid div { min-height: 76px; padding: 14px; border-radius: 15px; background: #f8f8f5; }.info-grid span, .info-grid strong { display: block; }.info-grid span { color: var(--color-text-tertiary); font-size: 11px; }.info-grid strong { margin-top: 6px; color: var(--color-text-secondary); font-size: 13px; font-weight: 500; line-height: 1.4; overflow-wrap: anywhere; }
@media (max-width: 600px) { .detail-page { width: min(100% - 32px, 760px); }.detail-card { padding: 22px; }.content-text { font-size: 21px; }.info-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
