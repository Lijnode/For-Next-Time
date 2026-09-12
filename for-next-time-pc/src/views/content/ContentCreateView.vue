<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createContent } from '../../api/content'

const router = useRouter()
const form = reactive({ content: '', categoryCode: null as string | null, tagNames: [] as string[], remark: '' })
const tagInput = ref('')
const showRemark = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const categories = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]

/**
 * 将输入框中的标签加入当前表单，最多保留十个。
 */
function addTag() {
  // 1. 清理输入并跳过空标签、重复标签。
  const tag = tagInput.value.trim()
  if (!tag || form.tagNames.includes(tag) || form.tagNames.length >= 10) return
  // 2. 加入标签并清空输入框。
  form.tagNames.push(tag)
  tagInput.value = ''
}

/**
 * 从表单中移除指定标签。
 */
function removeTag(tag: string) {
  // 1. 保留所有与目标标签不同的值。
  form.tagNames = form.tagNames.filter((item) => item !== tag)
}

/**
 * 保存内容；失败时保持当前表单数据不变。
 */
async function submit() {
  // 1. 清理旧错误并启用提交状态。
  errorMessage.value = ''
  loading.value = true
  try {
    // 2. 提交四个锁定字段，成功后返回首页。
    await createContent({ ...form, remark: form.remark.trim() || null })
    await router.replace({ name: 'home' })
  } catch (error) {
    // 3. 失败时只展示错误，不清空用户输入。
    errorMessage.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
  } finally {
    // 4. 恢复提交按钮状态。
    loading.value = false
  }
}
</script>

<template>
  <main class="create-page"><section class="editor-card"><p class="eyebrow">FOR NEXT TIME</p><h1>先留一句</h1>
    <el-form @submit.prevent="submit"><el-form-item label="正文 *"><el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="粘贴你想以后再用的内容……" /></el-form-item>
      <el-form-item label="分类"><el-select v-model="form.categoryCode" clearable placeholder="请选择 / 不分类"><el-option v-for="category in categories" :key="category[0]" :label="category[1]" :value="category[0]" /></el-select></el-form-item>
      <el-form-item label="标签"><el-tag v-for="tag in form.tagNames" :key="tag" closable @close="removeTag(tag)">#{{ tag }}</el-tag><el-input v-model="tagInput" class="tag-input" placeholder="添加标签" @keyup.enter="addTag" /><el-button text type="primary" @click="addTag">添加</el-button></el-form-item>
      <el-button text @click="showRemark = !showRemark">更多信息</el-button><el-form-item v-if="showRemark" label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p><div class="actions"><el-button @click="router.back()">取消</el-button><el-button type="primary" native-type="submit" :loading="loading">保存</el-button></div>
    </el-form></section></main>
</template>

<style scoped>
.create-page{padding:64px}.editor-card{width:min(760px,100%);margin:auto;padding:40px;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-xl)}.eyebrow{color:var(--color-brand);font-size:12px;font-weight:700;letter-spacing:.15em}.tag-input{width:180px;margin-left:8px}.actions{display:flex;justify-content:flex-end;gap:12px}.error{color:var(--color-danger)}
</style>
