<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { createContent, getContentDetail, updateContent } from '../../api/content'
import { getTagList, type TagItem } from '../../api/tag'

const route = useRoute()
const router = useRouter()
const form = reactive({ content: '', categoryCode: null as string | null, tagNames: [] as string[], remark: '' })
const tagInput = ref('')
const suggestedTags = ref<TagItem[]>([])
const tagLoading = ref(false)
const showRemark = ref(false)
const loading = ref(false)
const initialLoading = ref(false)
const errorMessage = ref('')
const initialSnapshot = ref('')
const allowLeave = ref(false)
const categories = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]
const editingId = computed(() => route.name === 'content-edit' ? Number(route.params.id) : null)
const isEditing = computed(() => editingId.value !== null)

/**
 * 编辑模式下加载原内容并回填到与新增共用的表单。
 */
async function loadEditingContent() {
  // 1. 新增模式无需读取已有内容。
  if (!isEditing.value) return
  // 2. 校验编辑路由中的内容主键。
  if (!Number.isInteger(editingId.value) || editingId.value! < 1) {
    errorMessage.value = '内容不存在或无权操作'
    return
  }
  // 3. 读取详情并回填正文、分类、标签和备注。
  initialLoading.value = true
  try {
    const detail = await getContentDetail(editingId.value!)
    form.content = detail.content
    form.categoryCode = detail.categoryCode
    form.tagNames = detail.tags.map((tag) => tag.name)
    form.remark = detail.remark ?? ''
    showRemark.value = Boolean(detail.remark)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载内容失败，请稍后重试'
  } finally {
    // 4. 无论成功或失败都结束初始加载状态。
    initialLoading.value = false
  }
}

/**
 * 将当前可保存字段序列化为稳定快照，用于比较未保存改动。
 */
function createFormSnapshot() {
  // 1. 只保留会提交给后端的字段，忽略标签输入框等临时界面状态。
  return JSON.stringify({
    content: form.content,
    categoryCode: form.categoryCode,
    tagNames: form.tagNames,
    remark: form.remark,
  })
}

/**
 * 记录当前表单为已保存基线，后续离开页面时用于判断是否需要确认。
 */
function saveFormSnapshot() {
  // 1. 覆盖页面进入或数据加载完成后的初始表单状态。
  initialSnapshot.value = createFormSnapshot()
}

/**
 * 在有未保存表单改动时询问用户是否放弃这些输入。
 */
async function confirmDiscardChanges() {
  // 1. 无改动或已成功保存时允许直接离开。
  if (allowLeave.value || createFormSnapshot() === initialSnapshot.value) return true
  // 2. 使用桌面端确认框，避免用户误丢正文和标签。
  try {
    await ElMessageBox.confirm('当前修改尚未保存，确定离开吗？', '放弃修改', {
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
      type: 'warning',
    })
    return true
  } catch {
    // 3. 用户取消时继续停留在表单页面。
    return false
  }
}

/**
 * 查询当前用户已有标签，为输入框提供可复用建议。
 */
async function loadTags(keyword = tagInput.value) {
  // 1. 发起标签查询并标记建议区域加载状态。
  tagLoading.value = true
  try {
    suggestedTags.value = await getTagList(keyword)
  } catch {
    // 2. 查询失败时不影响手动输入新标签的主流程。
    suggestedTags.value = []
  } finally {
    // 3. 无论成功或失败都结束建议区域加载状态。
    tagLoading.value = false
  }
}

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
 * 点选已有标签并加入当前内容表单。
 */
function selectSuggestedTag(tagName: string) {
  // 1. 将建议标签写入现有输入后复用统一添加逻辑。
  tagInput.value = tagName
  addTag()
}

/**
 * 从表单中移除指定标签。
 */
function removeTag(tag: string) {
  // 1. 保留所有与目标标签不同的值。
  form.tagNames = form.tagNames.filter((item) => item !== tag)
}

/**
 * 新增或更新内容；失败时保持当前表单数据不变。
 */
async function submit() {
  // 1. 清理旧错误并启用提交状态。
  errorMessage.value = ''
  loading.value = true
  try {
    // 2. 按当前模式提交同一份内容表单数据。
    const payload = { ...form, remark: form.remark.trim() || null }
    if (isEditing.value) {
      await updateContent(editingId.value!, payload)
      allowLeave.value = true
      await router.replace({ name: 'content-detail', params: { id: editingId.value } })
    } else {
      await createContent(payload)
      allowLeave.value = true
      await router.replace({ name: 'home' })
    }
  } catch (error) {
    // 3. 失败时只展示错误，不清空用户输入。
    errorMessage.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
  } finally {
    // 4. 恢复提交按钮状态。
    loading.value = false
  }
}

// 1. 编辑路由首次进入时回填现有内容，并记录表单初始状态。
onMounted(async () => {
  await loadEditingContent()
  saveFormSnapshot()
})

// 2. 路由切换前确认未保存改动，防止误丢用户输入。
onBeforeRouteLeave(confirmDiscardChanges)
</script>

<template>
  <main class="create-page"><section class="editor-card"><p class="eyebrow">FOR NEXT TIME</p><h1>{{ isEditing ? '修改这一句' : '先留一句' }}</h1>
    <p v-if="initialLoading">正在载入内容……</p><el-form v-else @submit.prevent="submit"><el-form-item label="正文 *"><el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="粘贴你想以后再用的内容……" /></el-form-item>
      <el-form-item label="分类"><el-select v-model="form.categoryCode" clearable placeholder="请选择 / 不分类"><el-option v-for="category in categories" :key="category[0]" :label="category[1]" :value="category[0]" /></el-select></el-form-item>
      <el-form-item label="标签"><el-tag v-for="tag in form.tagNames" :key="tag" closable @close="removeTag(tag)">#{{ tag }}</el-tag><el-input v-model="tagInput" class="tag-input" placeholder="添加标签" @focus="loadTags()" @input="loadTags(tagInput)" @keyup.enter="addTag" /><el-button text type="primary" native-type="button" @click="addTag">添加</el-button></el-form-item><div v-if="suggestedTags.length || tagLoading" class="tag-suggestions"><span>已有标签</span><el-button v-for="tag in suggestedTags" :key="tag.id" text size="small" native-type="button" @click="selectSuggestedTag(tag.name)">#{{ tag.name }}</el-button><span v-if="tagLoading">加载中…</span></div>
      <el-button text @click="showRemark = !showRemark">更多信息</el-button><el-form-item v-if="showRemark" label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p><div class="actions"><el-button @click="router.back()">取消</el-button><el-button type="primary" native-type="submit" :loading="loading">{{ isEditing ? '保存修改' : '保存' }}</el-button></div>
    </el-form></section></main>
</template>

<style scoped>
.create-page{padding:64px}.editor-card{width:min(760px,100%);margin:auto;padding:40px;background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-xl)}.eyebrow{color:var(--color-brand);font-size:12px;font-weight:700;letter-spacing:.15em}.tag-input{width:180px;margin-left:8px}.tag-suggestions{display:flex;flex-wrap:wrap;align-items:center;gap:4px;margin:-14px 0 18px 74px;color:var(--color-text-tertiary);font-size:13px}.actions{display:flex;justify-content:flex-end;gap:12px}.error{color:var(--color-danger)}
</style>
