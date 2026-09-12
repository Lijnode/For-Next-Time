<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createContent } from '../../api/content'
import { showError, showSuccess } from '../../platform/feedback'

const router = useRouter()
const form = reactive({ content: '', categoryCode: null as string | null, tagNames: [] as string[], remark: '' })
const tagInput = ref('')
const showMore = ref(false)
const showCategorySheet = ref(false)
const showTagSheet = ref(false)
const loading = ref(false)
const categories = [['FUNNY', '搞笑'], ['ABSTRACT', '抽象'], ['EMOTION', '感情'], ['LITERARY', '文艺'], ['HEALING', '治愈'], ['SOBER', '清醒'], ['SARCASM', '阴阳'], ['OTHER', '其他']]
const categoryActions = [{ name: '不分类', value: null }, ...categories.map(([value, name]) => ({ name, value }))]

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
 * 从当前表单中移除指定标签。
 */
function removeTag(tag: string) {
  // 1. 保留其余标签，维持表单输入不受影响。
  form.tagNames = form.tagNames.filter((item) => item !== tag)
}

/**
 * 保存内容，失败时保留页面的全部输入。
 */
async function submit() {
  // 1. 锁定保存按钮避免重复提交。
  loading.value = true
  try {
    // 2. 提交锁定字段并提示成功。
    await createContent({ ...form, remark: form.remark.trim() || null })
    showSuccess('先留着，下次一定。')
    await router.replace({ name: 'home' })
  } catch (error) {
    // 3. 失败时通过平台反馈层提示原因。
    showError(error instanceof Error ? error.message : '保存失败，请稍后重试')
  } finally {
    // 4. 恢复保存按钮状态。
    loading.value = false
  }
}
</script>

<template>
  <main class="create-page"><header><button type="button" @click="router.back()">‹</button><h1>先留一句</h1></header>
    <van-form @submit="submit"><van-cell-group inset><van-field v-model="form.content" type="textarea" rows="7" maxlength="5000" show-word-limit label="正文 *" placeholder="粘贴你想以后再用的内容……" :rules="[{ required: true, message: '请填写正文' }]" />
      <van-field :model-value="categories.find((item) => item[0] === form.categoryCode)?.[1] ?? '不分类'" label="分类" is-link readonly @click="showCategorySheet = true" />
      <van-field :model-value="form.tagNames.length ? `${form.tagNames.length} 个标签` : '添加标签'" label="标签" is-link readonly @click="showTagSheet = true" /><div class="tags"><van-tag v-for="tag in form.tagNames" :key="tag" closeable type="primary" @close="removeTag(tag)">#{{ tag }}</van-tag></div>
    </van-cell-group><button class="more" type="button" @click="showMore = !showMore">更多信息</button><van-cell-group v-if="showMore" inset><van-field v-model="form.remark" type="textarea" rows="3" maxlength="1000" label="备注" /></van-cell-group><div class="save"><van-button block round type="primary" native-type="submit" :loading="loading">保存</van-button></div></van-form>
    <van-action-sheet v-model:show="showCategorySheet" :actions="categoryActions" @select="selectCategory" /><van-action-sheet v-model:show="showTagSheet" title="标签"><div class="tag-sheet"><van-field v-model="tagInput" placeholder="输入标签后添加" @keyup.enter="addTag" /><van-button block type="primary" @click="addTag">添加标签</van-button><div class="tags"><van-tag v-for="tag in form.tagNames" :key="tag" closeable type="primary" @close="removeTag(tag)">#{{ tag }}</van-tag></div></div></van-action-sheet></main>
</template>

<style scoped>
.create-page{min-height:100vh;padding:calc(12px + env(safe-area-inset-top)) 0 32px}.create-page header{display:flex;align-items:center;padding:0 16px 18px}.create-page h1{margin:0 auto;font-size:18px}.create-page header button,.more{border:0;background:none;color:var(--color-brand);font:inherit}.create-page header button{font-size:28px}.tags{display:flex;flex-wrap:wrap;gap:8px;padding:8px 16px 16px}.tag-sheet{padding:16px}.more{margin:16px}.save{margin:28px 16px 0}
</style>
