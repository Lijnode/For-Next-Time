<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetail, type ContentDetail } from '../../api/content'
import { showError } from '../../platform/feedback'

const route = useRoute()
const router = useRouter()
const detail = ref<ContentDetail | null>(null)
const loading = ref(true)
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

// 1. 页面首次进入时读取当前内容详情。
onMounted(loadDetail)
</script>

<template>
  <main class="detail-page">
    <header class="top-bar"><button type="button" aria-label="返回" @click="router.back()">‹</button><h1>内容详情</h1></header>
    <van-loading v-if="loading" class="loading" vertical>正在翻找这句话……</van-loading>
    <van-empty v-else-if="errorMessage" :description="errorMessage"><van-button type="primary" round @click="router.replace({ name: 'home' })">回到首页</van-button></van-empty>
    <article v-else-if="detail" class="detail-card">
      <p class="content-text">{{ detail.content }}</p>
      <div class="tag-row"><van-tag v-if="detail.categoryName" plain type="warning">{{ detail.categoryName }}</van-tag><van-tag v-for="tag in detail.tags" :key="tag.id" plain type="primary">#{{ tag.name }}</van-tag></div>
      <section v-if="detail.remark" class="remark-block"><p>备注</p><span>{{ detail.remark }}</span></section>
      <van-cell-group inset class="meta-list"><van-cell title="复制次数" :value="`${detail.copyCount} 次`" /><van-cell title="最近使用" :value="detail.lastCopyTime ?? '还没有复制过'" /><van-cell title="创建时间" :value="detail.createTime" /><van-cell title="修改时间" :value="detail.updateTime" /></van-cell-group>
    </article>
  </main>
</template>

<style scoped>
.detail-page{min-height:100vh;padding:calc(10px + env(safe-area-inset-top)) 0 32px}.top-bar{display:flex;align-items:center;padding:0 16px 18px}.top-bar button{border:0;background:none;color:var(--color-brand);font-size:28px;line-height:1}.top-bar h1{margin:0 auto;font-size:18px;transform:translateX(-14px)}.loading{padding:80px 0}.detail-card{padding:6px 16px}.content-text{margin:20px 0 18px;color:var(--color-text);font-size:17px;line-height:1.8;white-space:pre-wrap}.tag-row{display:flex;flex-wrap:wrap;gap:8px}.remark-block{margin:24px 0;padding:16px;background:#fff1ec;border-radius:var(--radius-card)}.remark-block p{margin:0 0 8px;color:var(--color-brand);font-size:12px;font-weight:700}.remark-block span{color:var(--color-muted);line-height:1.7;white-space:pre-wrap}.meta-list{margin-top:24px}
</style>
