<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteContent, getContentDetail, recordCopy, type ContentDetail } from '../../api/content'

const route = useRoute()
const router = useRouter()
const detail = ref<ContentDetail | null>(null)
const loading = ref(true)
const deleting = ref(false)
const copying = ref(false)
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
    <article v-else-if="detail" class="detail-card">
      <header class="detail-header">
        <div>
          <p class="eyebrow">FOR NEXT TIME</p>
          <h1>留住的这一句</h1>
        </div>
        <div class="header-actions"><el-button text @click="router.back()">返回</el-button><el-button text type="primary" :loading="copying" @click="copyContent">复制</el-button><el-button text type="primary" @click="editContent">编辑</el-button><el-popconfirm title="确定删除这句话吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="removeContent"><template #reference><el-button text type="danger" :loading="deleting">删除</el-button></template></el-popconfirm></div>
      </header>

      <p class="content-text">{{ detail.content }}</p>

      <div class="tag-row">
        <el-tag v-if="detail.categoryName" effect="plain" type="warning">{{ detail.categoryName }}</el-tag>
        <el-tag v-for="tag in detail.tags" :key="tag.id" effect="plain">#{{ tag.name }}</el-tag>
      </div>

      <section v-if="detail.remark" class="remark-block">
        <p class="section-label">备注</p>
        <p>{{ detail.remark }}</p>
      </section>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <p v-if="copyMessage" class="copy-message">{{ copyMessage }}</p>

      <dl class="meta-list">
        <div><dt>复制次数</dt><dd>{{ detail.copyCount }} 次</dd></div>
        <div><dt>最近使用</dt><dd>{{ detail.lastCopyTime ?? '还没有复制过' }}</dd></div>
        <div><dt>创建时间</dt><dd>{{ detail.createTime }}</dd></div>
        <div><dt>修改时间</dt><dd>{{ detail.updateTime }}</dd></div>
      </dl>
    </article>
  </main>
</template>

<style scoped>
.detail-page { width: min(860px, calc(100% - 64px)); margin: 0 auto; padding: 72px 0; }
.detail-card, .state-card { padding: 42px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); }
.state-card { color: var(--color-text-secondary); text-align: center; }
.detail-header, .header-actions { display: flex; align-items: flex-start; gap: 8px; }.detail-header { justify-content: space-between; gap: 24px; }
.eyebrow, .section-label { margin: 0; color: var(--color-brand); font-size: 12px; font-weight: 700; letter-spacing: .14em; }
h1 { margin: 10px 0 0; font-size: 28px; }
.content-text { margin: 36px 0 24px; color: var(--color-text-primary); font-size: 18px; line-height: 1.8; white-space: pre-wrap; }
.tag-row { display: flex; flex-wrap: wrap; gap: 8px; }
.remark-block { margin-top: 28px; padding: 20px; background: var(--color-brand-soft); border-radius: var(--radius-md); }
.remark-block p:last-child { margin: 10px 0 0; color: var(--color-text-secondary); line-height: 1.7; white-space: pre-wrap; }
.error-message { margin: 20px 0 0; color: var(--color-danger); }
.copy-message { margin: 20px 0 0; color: var(--color-brand); }
.meta-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px 32px; margin: 32px 0 0; padding-top: 24px; border-top: 1px solid var(--color-divider); }
.meta-list div { display: flex; justify-content: space-between; gap: 16px; }
dt { color: var(--color-text-tertiary); } dd { margin: 0; color: var(--color-text-secondary); text-align: right; }
</style>
