<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { createContent, getContentDetail, updateContent } from '../../api/content'
import { getTagList, type TagItem } from '../../api/tag'
import { confirm as confirmDialog, showError, showSuccess } from '../../platform/feedback'

const route = useRoute()
const router = useRouter()
const form = reactive({ content: '', categoryCode: null as string | null, tagNames: [] as string[], remark: '' })
const tagInput = ref('')
const suggestedTags = ref<TagItem[]>([])
const tagLoading = ref(false)
const showMore = ref(false)
const showCategorySheet = ref(false)
const showTagSheet = ref(false)
const loading = ref(false)
const initialLoading = ref(false)
const errorMessage = ref('')
const initialSnapshot = ref('')
const allowLeave = ref(false)
const categories = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]
const categoryActions = [{ name: '不分类', value: null }, ...categories.map(([value, name]) => ({ name, value }))]
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
    showMore.value = Boolean(detail.remark)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载内容失败，请稍后重试'
    showError(errorMessage.value)
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
  // 2. 通过移动端平台反馈层确认，避免用户误丢正文和标签。
  return confirmDialog('当前修改尚未保存，确定离开吗？')
}

/**
 * 查询当前用户已有标签，为移动端标签面板提供可复用建议。
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
 * 打开标签 Bottom Sheet 并加载当前用户已有标签。
 */
function openTagSheet() {
  // 1. 展开移动端标签选择面板。
  showTagSheet.value = true
  // 2. 异步加载默认标签建议，不阻塞面板展示。
  void loadTags()
}

/**
 * 从分类 Bottom Sheet 选择分类或清空分类。
 */
function selectCategory(action: { value: string | null }) {
  // 1. 写入用户选择的固定分类编码。
  form.categoryCode = action.value
  // 2. 关闭移动端选择面板。
  showCategorySheet.value = false
}

/**
 * 将输入标签加入表单，最多保留十个。
 */
function addTag() {
  // 1. 清理输入并跳过空值、重复值。
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
 * 从当前表单中移除指定标签。
 */
function removeTag(tag: string) {
  // 1. 保留其余标签，维持表单输入不受影响。
  form.tagNames = form.tagNames.filter((item) => item !== tag)
}

/**
 * 新增或更新内容，失败时保留页面的全部输入。
 */
async function submit() {
  // 1. 锁定保存按钮避免重复提交。
  loading.value = true
  try {
    // 2. 按当前模式提交同一份内容表单数据。
    const payload = { ...form, remark: form.remark.trim() || null }
    if (isEditing.value) {
      await updateContent(editingId.value!, payload)
      allowLeave.value = true
      showSuccess('先留着，下次一定。')
      await router.replace({ name: 'content-detail', params: { id: editingId.value } })
    } else {
      await createContent(payload)
      allowLeave.value = true
      showSuccess('先留着，下次一定。')
      await router.replace({ name: 'home' })
    }
  } catch (error) {
    // 3. 失败时通过平台反馈层提示原因。
    showError(error instanceof Error ? error.message : '保存失败，请稍后重试')
  } finally {
    // 4. 恢复保存按钮状态。
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
  <main class="create-page">
    <header><button type="button" aria-label="返回" @click="router.back()">‹</button><h1>{{ isEditing ? '编辑' : '先留一句' }}</h1><span aria-hidden="true"></span></header>
    <van-loading v-if="initialLoading" class="loading" vertical>正在载入内容……</van-loading>
    <van-form v-else @submit="submit">
      <section class="editor-card">
        <div class="editor-field"><label for="content">正文 <em>*</em></label><van-field id="content" v-model="form.content" type="textarea" rows="8" maxlength="5000" show-word-limit placeholder="粘贴你想以后再用的内容……" :rules="[{ required: true, message: '请填写正文' }]" /></div>
        <div class="editor-field"><label>分类</label><button class="field-picker" type="button" @click="showCategorySheet = true"><span>{{ categories.find((item) => item[0] === form.categoryCode)?.[1] ?? '不分类' }}</span><b>›</b></button></div>
        <div class="editor-field"><label>标签</label><button class="field-picker" type="button" @click="openTagSheet"><span>{{ form.tagNames.length ? form.tagNames.length + ' 个标签' : '添加标签' }}</span><b>›</b></button><div v-if="form.tagNames.length" class="tags"><van-tag v-for="tag in form.tagNames" :key="tag" closeable @close="removeTag(tag)">#{{ tag }}</van-tag></div></div>
        <button class="more" type="button" @click="showMore = !showMore">{{ showMore ? '收起备注' : '＋ 添加备注' }}</button>
        <div v-if="showMore" class="editor-field remark-field"><label for="remark">备注</label><van-field id="remark" v-model="form.remark" type="textarea" rows="4" maxlength="1000" placeholder="这句适合什么时候用？" /></div>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <div class="save"><van-button block type="primary" native-type="submit" :loading="loading">{{ isEditing ? '保存修改' : '保存' }}</van-button></div>
      </section>
    </van-form>
    <van-action-sheet v-model:show="showCategorySheet" :actions="categoryActions" @select="selectCategory" />
    <van-action-sheet v-model:show="showTagSheet" title="标签"><div class="tag-sheet"><van-field v-model="tagInput" placeholder="输入标签后添加" @update:model-value="loadTags" @keyup.enter="addTag" /><van-button block type="primary" @click="addTag">添加标签</van-button><p v-if="suggestedTags.length || tagLoading" class="suggestion-title">已有标签</p><div v-if="suggestedTags.length || tagLoading" class="tags suggestions"><van-tag v-for="tag in suggestedTags" :key="tag.id" plain @click="selectSuggestedTag(tag.name)">#{{ tag.name }}</van-tag><span v-if="tagLoading">加载中…</span></div><div class="tags"><van-tag v-for="tag in form.tagNames" :key="tag" closeable @close="removeTag(tag)">#{{ tag }}</van-tag></div></div></van-action-sheet>
  </main>
</template>

<style scoped>
.create-page { min-height: 100vh; padding: calc(12px + env(safe-area-inset-top)) 16px 32px; }.create-page header { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; margin-bottom: 16px; }.create-page h1 { margin: 0; color: var(--color-text-primary); font-size: 18px; text-align: center; }.create-page header button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); font-size: 26px; line-height: 1; }.editor-card { padding: 20px 17px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }.editor-field { margin-bottom: 20px; }.editor-field label { display: block; margin-bottom: 8px; color: var(--color-text-primary); font-size: 13px; font-weight: 700; }.editor-field em { color: var(--color-brand); font-style: normal; }.editor-field :deep(.van-cell) { padding: 0; background: #fbfbf9; border: 1px solid var(--color-border); border-radius: 14px; }.editor-field :deep(.van-field__control) { min-height: 168px; padding: 13px; color: var(--color-text-primary); font-size: 16px; line-height: 1.72; }.editor-field :deep(.van-field__word-limit) { padding: 0 13px 8px; }.field-picker { display: flex; width: 100%; min-height: 48px; align-items: center; justify-content: space-between; padding: 0 14px; border: 1px solid var(--color-border); border-radius: 14px; background: #fbfbf9; color: var(--color-text-secondary); font: inherit; font-size: 14px; text-align: left; }.field-picker b { color: var(--color-muted); font-size: 22px; font-weight: 400; }.tags { display: flex; flex-wrap: wrap; gap: 8px; padding-top: 10px; }.tags :deep(.van-tag) { border-radius: 999px; }.more { margin: -2px 0 18px; padding: 0; border: 0; background: transparent; color: var(--color-text-secondary); font: inherit; font-size: 13px; }.remark-field { padding-top: 18px; border-top: 1px dashed var(--color-border); }.remark-field :deep(.van-field__control) { min-height: 92px; }.save { margin-top: 25px; }.save :deep(.van-button) { height: 48px; border-radius: 14px; font-weight: 700; }.tag-sheet { padding: 16px; }.tag-sheet :deep(.van-field) { margin-bottom: 12px; border: 1px solid var(--color-border); border-radius: 12px; }.tag-sheet :deep(.van-button) { border-radius: 12px; }.suggestion-title { margin: 16px 0 0; color: var(--color-muted); font-size: 13px; }.suggestions { padding-left: 0; padding-right: 0; }.loading { padding: 80px 0; }.error { margin: 16px 0; color: var(--color-danger); }
</style>
