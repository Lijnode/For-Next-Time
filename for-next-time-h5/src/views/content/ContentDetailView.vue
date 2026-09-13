<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { ArrowLeft, Copy, Pencil, Pin, Trash2 } from 'lucide-vue-next'
import { deleteContent, getContentDetail, recordCopy, setPinned, type ContentDetail } from '../../api/content'
import { showError, showSuccess } from '../../platform/feedback'
import { copyText } from '../../platform/clipboard'

const route = useRoute()
const router = useRouter()
const detail = ref<ContentDetail | null>(null)
const loading = ref(true)
const deleting = ref(false)
const copying = ref(false)
const pinning = ref(false)
const errorMessage = ref('')

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
  // 2. 请求详情并通过平台反馈层展示失败原因。
  try {
    detail.value = await getContentDetail(id)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载内容失败，请稍后重试'
    showError(errorMessage.value)
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
 * 通过平台剪贴板复制正文，成功后再记录复制统计。
 */
async function copyContent() {
  // 1. 平台剪贴板复制失败时不请求后端统计接口。
  if (!detail.value) return
  copying.value = true
  try {
    await copyText(detail.value.content)
    showSuccess('就是这句')
    // 2. 复制成功后才记录统计；统计失败不影响复制成功反馈。
    try {
      const stat = await recordCopy(detail.value.id)
      detail.value.copyCount = stat.copyCount
      detail.value.lastCopyTime = stat.lastCopyTime
    } catch {
      // 3. 接受少量统计误差，避免自动重试造成重复计数。
    }
  } catch (error) {
    showError(error instanceof Error ? error.message : '复制失败，请稍后重试')
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
    showError(error instanceof Error ? error.message : '置顶设置失败，请稍后重试')
  } finally {
    // 2. 请求结束后恢复置顶按钮状态。
    pinning.value = false
  }
}

/**
 * 经移动端确认后逻辑删除当前内容。
 */
async function removeContent() {
  // 1. 先通过 Vant 确认弹层避免误删除。
  try {
    await showConfirmDialog({ title: '删除这句话？', message: '删除后暂时无法在列表中找到。', confirmButtonText: '删除' })
  } catch {
    return
  }
  // 2. 请求删除并在成功后反馈、返回首页。
  deleting.value = true
  try {
    await deleteContent(Number(route.params.id))
    showSuccess('已删除这句话。')
    await router.replace({ name: 'home' })
  } catch (error) {
    showError(error instanceof Error ? error.message : '删除失败，请稍后重试')
  } finally {
    // 3. 无论成功或失败都恢复删除按钮状态。
    deleting.value = false
  }
}

// 1. 页面首次进入时读取当前内容详情。
onMounted(loadDetail)
</script>

<template>
  <main class="detail-page">
    <header class="top-bar"><button type="button" aria-label="返回" @click="router.back()"><ArrowLeft :size="21" /></button><h1>内容详情</h1><span aria-hidden="true"></span></header>
    <van-loading v-if="loading" class="loading" vertical>正在翻找这句话……</van-loading>
    <van-empty v-else-if="errorMessage" :description="errorMessage"><van-button type="primary" round @click="router.replace({ name: 'home' })">回到首页</van-button></van-empty>
    <article v-else-if="detail" class="detail-card">
      <p v-if="detail.pinned" class="pinned-mark"><Pin :size="14" fill="currentColor" />已置顶</p>
      <p class="content-text">{{ detail.content }}</p>
      <div class="tag-row"><span v-if="detail.categoryName" class="tag category-tag">{{ detail.categoryName }}</span><span v-for="tag in detail.tags" :key="tag.id" class="tag">#{{ tag.name }}</span></div>
      <div class="detail-actions"><van-button type="primary" :loading="copying" @click="copyContent"><Copy :size="16" />复制</van-button><van-button plain :loading="pinning" @click="togglePinned"><Pin :size="16" />{{ detail.pinned ? '取消置顶' : '置顶' }}</van-button><van-button plain @click="editContent"><Pencil :size="16" />编辑</van-button><button class="delete-button" type="button" :disabled="deleting" @click="removeContent"><Trash2 :size="15" />删除</button></div>
      <div class="info-list"><div><span>分类</span><strong>{{ detail.categoryName ?? '未分类' }}</strong></div><div><span>已用</span><strong>{{ detail.copyCount }} 次</strong></div><div><span>最近使用</span><strong>{{ detail.lastCopyTime ?? '还没有复制过' }}</strong></div><div><span>创建时间</span><strong>{{ detail.createTime }}</strong></div><div><span>修改时间</span><strong>{{ detail.updateTime }}</strong></div></div>
      <section v-if="detail.remark" class="remark-block"><p>备注</p><span>{{ detail.remark }}</span></section>
    </article>
  </main>
</template>

<style scoped>
.detail-page { min-height: 100vh; padding: calc(12px + env(safe-area-inset-top)) 16px 32px; }.top-bar { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; margin-bottom: 16px; }.top-bar button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); }.top-bar h1 { margin: 0; color: var(--color-text-primary); font-size: 18px; text-align: center; }.loading { padding: 80px 0; }.detail-card { padding: 20px 17px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }.pinned-mark { display: flex; align-items: center; gap: 5px; margin: 0; color: var(--color-brand); font-size: 12px; font-weight: 700; }.content-text { margin: 14px 0 18px; color: var(--color-text-primary); font-size: 20px; line-height: 1.82; white-space: pre-wrap; }.tag-row { display: flex; flex-wrap: wrap; gap: 7px; }.tag { padding: 5px 8px; border-radius: 999px; background: #f1f2ef; color: #737878; font-size: 11px; }.category-tag { background: #fff1ec; color: #cf5a41; }.detail-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 21px; }.detail-actions :deep(.van-button) { display: inline-flex; align-items: center; gap: 5px; height: 40px; border-radius: 12px; font-weight: 700; }.delete-button { display: inline-flex; align-items: center; gap: 4px; margin-left: auto; padding: 8px 0; border: 0; background: transparent; color: var(--color-muted); font: inherit; font-size: 12px; }.info-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; margin-top: 22px; }.info-list div { min-height: 67px; padding: 12px; border-radius: 14px; background: #f8f8f5; }.info-list span, .info-list strong { display: block; }.info-list span { color: var(--color-muted); font-size: 10px; }.info-list strong { margin-top: 5px; color: var(--color-text-secondary); font-size: 12px; font-weight: 500; line-height: 1.35; overflow-wrap: anywhere; }.remark-block { margin-top: 12px; padding: 15px; border: 1px solid var(--color-border); border-radius: 15px; background: #fafaf8; }.remark-block p { margin: 0 0 8px; color: var(--color-muted); font-size: 11px; font-weight: 700; }.remark-block span { color: var(--color-text-secondary); font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
</style>
