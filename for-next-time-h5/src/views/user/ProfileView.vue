<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Camera, Check, ChevronRight, Download, Info, KeyRound, LogOut } from 'lucide-vue-next'
import type { UploaderAfterRead, UploaderFileListItem } from 'vant'
import { getUserProfile, uploadAvatar, type UserProfile } from '../../api/user'
import { exportContents } from '../../api/content'
import { downloadFile } from '../../platform/download'
import { showError, showSuccess } from '../../platform/feedback'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile>()
const loading = ref(false)
const uploadingAvatar = ref(false)
const avatarFiles = ref<UploaderFileListItem[]>([])
const cropDialogVisible = ref(false)
const cropPreviewUrl = ref('')
const cropImage = ref<HTMLImageElement>()
const cropNaturalWidth = ref(0)
const cropNaturalHeight = ref(0)
const cropBox = ref({ x: 0, y: 0, size: 0 })
const cropDragMode = ref<'move' | 'resize' | null>(null)
const cropDragStart = ref({ x: 0, y: 0, cropX: 0, cropY: 0, cropSize: 0 })
const cropViewportSize = 280
const cropMinSize = 88
const exportSheetVisible = ref(false)
const exporting = ref(false)
const exportFields = ref(['content', 'category', 'tags'])
const exportFieldOptions = [
  { value: 'content', label: '正文' },
  { value: 'category', label: '分类' },
  { value: 'tags', label: '标签' },
  { value: 'remark', label: '备注' },
  { value: 'pinned', label: '置顶状态' },
  { value: 'copyCount', label: '复制次数' },
  { value: 'lastCopyTime', label: '最近使用时间' },
  { value: 'createTime', label: '创建时间' },
  { value: 'updateTime', label: '修改时间' },
]

/**
 * 加载当前登录用户的个人资料。
 */
async function loadProfile() {
  // 1. 进入加载状态并请求当前用户资料。
  loading.value = true
  try {
    profile.value = await getUserProfile()
  // 2. 请求结束后恢复页面交互状态。
  } finally {
    loading.value = false
  }
}

/**
 * 校验 Vant 上传组件读取到的头像文件，并打开裁剪面板。
 *
 * @param item Vant 返回的已读取文件对象
 */
const handleAvatarRead: UploaderAfterRead = (item) => {
  // 1. 读取单文件上传结果，并忽略取消选择或不完整文件。
  const file = Array.isArray(item) ? item[0]?.file : item.file
  if (!file) {
    return
  }
  // 2. 在请求前提示不符合服务端约束的文件。
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) {
    showError('头像仅支持 JPG 或 PNG，且不能超过 2MB')
    return
  }
  // 3. 创建本地预览，待用户框选确认后再上传。
  revokeCropPreviewUrl()
  cropPreviewUrl.value = URL.createObjectURL(file)
  cropDialogVisible.value = true
}

/**
 * 计算图片在移动端裁剪面板中的等比完整预览比例。
 */
function getCropPreviewScale() {
  // 1. 图片尺寸尚未加载时不生成预览比例。
  if (!cropNaturalWidth.value || !cropNaturalHeight.value) {
    return 0
  }
  // 2. 以较小比例完整展示原图，避免图片外增加额外底板。
  return Math.min(cropViewportSize / cropNaturalWidth.value, cropViewportSize / cropNaturalHeight.value)
}

/**
 * 获取完整预览图在裁剪面板中的可操作边界。
 */
function getCropImageBounds() {
  // 1. 使用等比缩放后的图片尺寸作为裁剪区域边界。
  const scale = getCropPreviewScale()
  return { left: 0, top: 0, width: cropNaturalWidth.value * scale, height: cropNaturalHeight.value * scale }
}

/**
 * 在预览图加载后初始化位于图片中央的方形裁剪框。
 *
 * @param event 预览图加载事件
 */
function initializeCrop(event: Event) {
  // 1. 读取浏览器已经解析的图片原始尺寸。
  const image = event.target as HTMLImageElement
  cropNaturalWidth.value = image.naturalWidth
  cropNaturalHeight.value = image.naturalHeight
  // 2. 将初始方形框居中放入图片的可选区域。
  const bounds = getCropImageBounds()
  const size = Math.min(bounds.width, bounds.height) * 0.78
  cropBox.value = { x: (bounds.width - size) / 2, y: (bounds.height - size) / 2, size }
}

/**
 * 记录裁剪框拖动或缩放开始时的状态。
 *
 * @param event 指针按下事件
 * @param mode 当前操作是移动框选区域还是拉角缩放
 */
function startCropBoxDrag(event: PointerEvent, mode: 'move' | 'resize') {
  // 1. 图片未加载完成时不允许操作裁剪框。
  if (!cropNaturalWidth.value || !cropNaturalHeight.value) {
    return
  }
  // 2. 保存操作起点并捕获指针，保证移动过程不断开。
  cropDragMode.value = mode
  cropDragStart.value = { x: event.clientX, y: event.clientY, cropX: cropBox.value.x, cropY: cropBox.value.y, cropSize: cropBox.value.size }
  ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}

/**
 * 按手势移动裁剪框，或通过右下角调整方形裁剪范围。
 *
 * @param event 指针移动事件
 */
function moveCropBoxDrag(event: PointerEvent) {
  // 1. 未激活拖动或缩放时保持现状。
  if (!cropDragMode.value) {
    return
  }
  // 2. 限制裁剪框始终位于有效图片区域内。
  const bounds = getCropImageBounds()
  const deltaX = event.clientX - cropDragStart.value.x
  const deltaY = event.clientY - cropDragStart.value.y
  if (cropDragMode.value === 'move') {
    cropBox.value.x = Math.min(bounds.width - cropBox.value.size, Math.max(0, cropDragStart.value.cropX + deltaX))
    cropBox.value.y = Math.min(bounds.height - cropBox.value.size, Math.max(0, cropDragStart.value.cropY + deltaY))
    return
  }
  // 3. 以右下角为控制点等比缩放，保持头像输出为正方形。
  const maxSize = Math.min(bounds.width - cropDragStart.value.cropX, bounds.height - cropDragStart.value.cropY)
  const minSize = Math.min(cropMinSize, maxSize)
  const nextSize = cropDragStart.value.cropSize + Math.max(deltaX, deltaY)
  cropBox.value.size = Math.min(maxSize, Math.max(minSize, nextSize))
}

/**
 * 结束当前裁剪框拖动或缩放操作。
 */
function endCropBoxDrag() {
  // 1. 清除操作状态，保留有效的框选区域。
  cropDragMode.value = null
}

/**
 * 将当前框选区域生成统一尺寸的 JPEG 头像文件。
 */
function createCroppedAvatar() {
  // 1. 确认预览图已加载，才能读取可绘制的图片数据。
  if (!cropImage.value) {
    throw new Error('图片尚未加载完成')
  }
  // 2. 将框选坐标映射回原图，并输出为 512 像素正方形。
  const scale = getCropPreviewScale()
  const sourceSize = cropBox.value.size / scale
  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 512
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('无法创建头像画布')
  }
  context.drawImage(cropImage.value, cropBox.value.x / scale, cropBox.value.y / scale, sourceSize, sourceSize, 0, 0, canvas.width, canvas.height)
  // 3. 使用压缩 JPEG 保持上传文件稳定在服务端大小限制内。
  return new Promise<File>((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('头像裁剪失败'))
        return
      }
      resolve(new File([blob], 'avatar.jpg', { type: 'image/jpeg' }))
    }, 'image/jpeg', 0.9)
  })
}

/**
 * 确认当前框选结果并上传生成的头像。
 */
async function confirmAvatarCrop() {
  // 1. 锁定面板交互，生成并上传当前裁剪结果。
  uploadingAvatar.value = true
  try {
    profile.value = await uploadAvatar(await createCroppedAvatar())
    showSuccess('头像已更新')
  } catch (error) {
    // 2. 上传失败时保留裁剪面板，便于用户调整后重试。
    showError(error instanceof Error ? error.message : '头像上传失败，请稍后重试')
    return
  } finally {
    // 3. 恢复裁剪面板的操作能力。
    uploadingAvatar.value = false
  }
  // 4. 上传成功后关闭面板并释放临时图片资源。
  closeCropDialog()
}

/**
 * 释放当前本地预览地址，避免重复选图占用浏览器内存。
 */
function revokeCropPreviewUrl() {
  // 1. 释放上一张图片的对象 URL。
  if (cropPreviewUrl.value) {
    URL.revokeObjectURL(cropPreviewUrl.value)
  }
  // 2. 重置预览相关状态，避免沿用旧裁剪框。
  cropPreviewUrl.value = ''
  cropNaturalWidth.value = 0
  cropNaturalHeight.value = 0
  cropBox.value = { x: 0, y: 0, size: 0 }
}

/**
 * 关闭移动端裁剪面板并清理本次临时图片。
 */
function closeCropDialog() {
  // 1. 关闭面板。
  cropDialogVisible.value = false
  // 2. 清空上传组件的选中记录，并释放本地预览资源。
  avatarFiles.value = []
  revokeCropPreviewUrl()
}

/**
 * 进入修改当前账户密码的页面。
 */
async function openPasswordForm() {
  // 1. 跳转至同样受登录保护的密码修改路由。
  await router.push({ name: 'password-change' })
}

/**
 * 打开产品介绍页查看应用说明与版本信息。
 */
async function openAboutPage() {
  // 1. 跳转至不展示底部导航的独立关于页。
  await router.push({ name: 'about' })
}

/**
 * 打开内容导出底部面板。
 */
function openExportSheet() {
  // 1. 仅切换面板状态，保留用户上次调整过的字段。
  exportSheetVisible.value = true
}

/**
 * 切换一个固定导出字段的选择状态。
 *
 * @param field 需要切换的字段编码
 */
function toggleExportField(field: string) {
  // 1. 已选择时移除字段，未选择时按固定列表顺序追加字段。
  if (exportFields.value.includes(field)) {
    exportFields.value = exportFields.value.filter((item) => item !== field)
    return
  }
  exportFields.value = exportFieldOptions.filter((item) => [...exportFields.value, field].includes(item.value)).map((item) => item.value)
}

/**
 * 请求导出文件并通过移动端平台层保存文件。
 */
async function downloadContentExport() {
  // 1. 阻止空字段导出，确保用户能得到有意义的 TXT 文件。
  if (!exportFields.value.length) {
    showError('请至少选择一个导出字段')
    return
  }
  exporting.value = true
  try {
    // 2. 请求当前用户的导出文件，再通过平台层触发设备下载。
    const file = await exportContents(exportFields.value)
    downloadFile(file.blob, file.fileName)
    showSuccess('内容已导出')
    exportSheetVisible.value = false
  } catch (error) {
    // 3. 失败时保留字段选择，便于用户直接调整或重试。
    showError(error instanceof Error ? error.message : '导出失败，请稍后重试')
  } finally {
    // 4. 恢复导出按钮，避免下载失败后页面不可操作。
    exporting.value = false
  }
}

/**
 * 清除当前账户登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与平台存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护的资料页面。
  await router.replace({ name: 'login' })
}

// 1. 页面进入时加载当前账户资料。
onMounted(loadProfile)
</script>

<template>
  <main class="profile-page">
    <van-loading v-if="loading" class="loading" vertical>正在加载</van-loading>
    <template v-else-if="profile">
      <header class="page-header"><h1>我的</h1></header>
      <section class="profile-summary">
        <div class="profile-head">
          <van-uploader v-model="avatarFiles" class="avatar-uploader" accept="image/jpeg,image/png" :max-count="1" :preview-image="false" :disabled="uploadingAvatar" :after-read="handleAvatarRead">
            <button class="profile-mark" type="button" :aria-label="uploadingAvatar ? '正在上传头像' : '更换头像'">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="`${profile.username} 的头像`">
              <span v-else>{{ profile.username.slice(0, 1) }}</span>
              <span class="avatar-edit" aria-hidden="true"><Camera :size="14" /></span>
            </button>
          </van-uploader>
          <div><h1>{{ profile.username }}</h1><p>已经留下 {{ profile.contentCount }} 条内容</p></div>
        </div>
      </section>
      <section class="profile-group profile-stats" aria-label="账户概览">
        <div class="profile-row"><span>内容总数</span><strong>{{ profile.contentCount }}</strong></div>
        <div class="profile-row"><span>加入时间</span><strong>{{ profile.createTime }}</strong></div>
      </section>
      <section class="profile-group" aria-label="账户设置">
        <button class="profile-row settings-row" type="button" @click="openPasswordForm"><span><KeyRound :size="18" />修改密码</span><ChevronRight :size="18" /></button>
        <button class="profile-row settings-row" type="button" @click="openExportSheet"><span><Download :size="18" />导出内容</span><ChevronRight :size="18" /></button>
        <button class="profile-row settings-row" type="button" @click="openAboutPage"><span><Info :size="18" />关于下次一定</span><ChevronRight :size="18" /></button>
      </section>
      <section class="profile-group logout-group" aria-label="账户操作">
        <button class="profile-row logout-button" type="button" @click="logout"><span><LogOut :size="18" />退出登录</span></button>
      </section>
    </template>
    <div v-if="cropDialogVisible" class="crop-overlay" role="dialog" aria-modal="true" aria-label="裁剪头像">
      <section class="crop-sheet">
        <header class="crop-sheet-header"><h2>裁剪头像</h2><button type="button" :disabled="uploadingAvatar" @click="closeCropDialog">取消</button></header>
        <p>拖动框选区域，或拉动右下角调整裁剪范围</p>
        <div class="crop-viewport" :style="{ width: `${getCropImageBounds().width}px`, height: `${getCropImageBounds().height}px` }" @pointermove="moveCropBoxDrag" @pointerup="endCropBoxDrag" @pointercancel="endCropBoxDrag">
          <img v-if="cropPreviewUrl" ref="cropImage" class="crop-source-image" :src="cropPreviewUrl" :style="{ width: `${getCropImageBounds().width}px`, height: `${getCropImageBounds().height}px` }" draggable="false" alt="待裁剪头像" @load="initializeCrop">
          <div v-if="cropBox.size" class="crop-selection" :style="{ width: `${cropBox.size}px`, height: `${cropBox.size}px`, left: `${cropBox.x}px`, top: `${cropBox.y}px` }" @pointerdown.stop="startCropBoxDrag($event, 'move')">
            <span class="crop-resize-handle" aria-label="调整裁剪范围" @pointerdown.stop="startCropBoxDrag($event, 'resize')"></span>
          </div>
        </div>
        <button class="crop-confirm" type="button" :disabled="uploadingAvatar" @click="confirmAvatarCrop">{{ uploadingAvatar ? '正在保存…' : '使用此头像' }}</button>
      </section>
    </div>
    <van-action-sheet v-model:show="exportSheetVisible" title="导出内容">
      <section class="export-sheet">
        <p>选择要导出的字段</p>
        <div class="export-field-list">
          <button v-for="field in exportFieldOptions" :key="field.value" class="export-field" :class="{ 'is-selected': exportFields.includes(field.value) }" type="button" :aria-pressed="exportFields.includes(field.value)" @click="toggleExportField(field.value)">
            <span class="export-checkbox" aria-hidden="true"><Check v-if="exportFields.includes(field.value)" :size="12" :stroke-width="3" /></span>
            <span>{{ field.label }}</span>
          </button>
        </div>
<!--        <div class="export-format"><span>文件格式</span><strong>TXT 文本文件</strong></div>-->
        <button class="export-confirm" type="button" :disabled="exporting" @click="downloadContentExport">{{ exporting ? '正在导出…' : '导出 TXT' }}</button>
      </section>
    </van-action-sheet>
  </main>
</template>

<style scoped>
.profile-page { min-height: 100vh; padding: calc(12px + env(safe-area-inset-top)) 16px 32px; }
.loading { margin-top: 80px; }
.page-header { padding: 7px 0 18px; }.page-header h1 { margin: 0; color: var(--color-text-primary); font-size: 22px; letter-spacing: .02em; }
.profile-summary { padding: 18px 16px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); box-shadow: 0 6px 16px rgb(31 33 33 / 3%); }.profile-head { display: flex; align-items: center; gap: 14px; }
.avatar-uploader { display: block; flex: 0 0 64px; width: 64px; height: 64px; }.avatar-uploader :deep(.van-uploader__wrapper) { display: block; width: 64px; height: 64px; }.avatar-uploader :deep(.van-uploader__input) { width: 64px; height: 64px; }
.profile-mark { position: relative; display: grid; box-sizing: border-box; width: 64px; height: 64px; padding: 0; place-items: center; overflow: hidden; border: 0; border-radius: 22px; background: var(--color-brand); color: #fff; font: inherit; font-size: 24px; font-weight: 800; box-shadow: 0 7px 15px rgb(255 111 78 / 20%); }.profile-mark:disabled { opacity: 0.75; }.profile-mark img { position: absolute; inset: 0; display: block; width: 100% !important; height: 100% !important; max-width: none; object-fit: cover; }.avatar-edit { position: absolute; right: 3px; bottom: 3px; z-index: 1; display: grid; width: 22px; height: 22px; place-items: center; border: 2px solid #fff; border-radius: 50%; background: var(--color-brand); }
.profile-head h1 { margin: 0; color: var(--color-text-primary); font-size: 19px; }.profile-head p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }
.profile-group { overflow: hidden; margin-top: 14px; border: 1px solid var(--color-border); border-radius: 17px; background: var(--color-card); }.profile-row { display: flex; width: 100%; min-height: 54px; align-items: center; justify-content: space-between; padding: 0 15px; border: 0; border-bottom: 1px solid #efefeb; background: transparent; color: var(--color-text-secondary); font: inherit; font-size: 14px; text-align: left; }.profile-row:last-child { border-bottom: 0; }.profile-row strong { color: var(--color-text-primary); font-size: 13px; font-weight: 700; }.settings-row { color: var(--color-text-primary); }.settings-row span, .logout-button span { display: inline-flex; align-items: center; gap: 10px; }.settings-row > svg { color: var(--color-muted); }
.logout-group { margin-top: 22px; }.logout-button { color: #d95d4a; }
.crop-overlay { position: fixed; z-index: 20; inset: 0; display: flex; align-items: flex-end; background: rgb(24 31 29 / 42%); }
.crop-sheet { box-sizing: border-box; width: 100%; padding: 20px 20px calc(20px + env(safe-area-inset-bottom)); border-radius: 24px 24px 0 0; background: #fff; }
.crop-sheet-header { display: flex; align-items: center; justify-content: space-between; }.crop-sheet-header h2 { margin: 0; color: var(--color-text-primary); font-size: 19px; }.crop-sheet-header button { min-height: 44px; padding: 0; border: 0; background: transparent; color: var(--color-brand); font: inherit; font-weight: 600; }.crop-sheet-header button:disabled { color: var(--color-muted); }
.crop-sheet > p { margin: 4px 0 18px; color: var(--color-muted); font-size: 13px; line-height: 1.6; }
.crop-viewport { position: relative; max-width: min(280px, calc(100vw - 40px)); max-height: min(280px, calc(100vw - 40px)); margin: 0 auto; overflow: hidden; border-radius: 18px; touch-action: none; user-select: none; }
.crop-source-image { position: absolute; display: block; border-radius: inherit; }
.crop-selection { position: absolute; box-sizing: border-box; border: 2px solid #fff; cursor: move; box-shadow: 0 0 0 200vmax rgb(20 28 26 / 46%); }.crop-selection::before, .crop-selection::after { position: absolute; top: 50%; left: 50%; width: 1px; height: 100%; background: rgb(255 255 255 / 58%); content: ''; transform: translate(-50%, -50%); pointer-events: none; }.crop-selection::after { width: 100%; height: 1px; }
.crop-resize-handle { position: absolute; right: -12px; bottom: -12px; width: 24px; height: 24px; border: 3px solid #fff; border-radius: 50%; background: var(--color-brand); cursor: nwse-resize; }
.crop-confirm { display: block; width: min(100%, 360px); min-height: 48px; margin: 22px auto 0; border: 0; border-radius: 14px; background: var(--color-brand); color: #fff; font: inherit; font-weight: 700; }.crop-confirm:disabled { opacity: .68; }
.export-sheet { padding: 0 16px calc(18px + env(safe-area-inset-bottom)); }.export-sheet > p { margin: 0; color: var(--color-muted); font-size: 13px; line-height: 1.65; }
.export-field-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; margin-top: 18px; }.export-field { display: inline-flex; min-height: 46px; align-items: center; gap: 9px; padding: 0 12px; border: 1px solid var(--color-border); border-radius: 13px; background: #fff; color: var(--color-text-secondary); font: inherit; font-size: 14px; text-align: left; }.export-field.is-selected { border-color: rgb(255 111 78 / 45%); background: #fff4ef; color: var(--color-text-primary); }.export-checkbox { display: grid; width: 17px; height: 17px; flex: 0 0 17px; place-items: center; border: 1px solid #c6c9c6; border-radius: 5px; color: transparent; }.export-field.is-selected .export-checkbox { border-color: var(--color-brand); background: var(--color-brand); color: #fff; }.export-checkbox :deep(svg) { display: block; }
.export-format { display: flex; align-items: center; justify-content: space-between; margin-top: 18px; padding-top: 15px; border-top: 1px solid #efefeb; color: var(--color-muted); font-size: 13px; }.export-format strong { color: var(--color-text-secondary); font-weight: 600; }.export-confirm { display: block; width: 100%; min-height: 48px; margin-top: 20px; border: 0; border-radius: 14px; background: var(--color-brand); color: #fff; font: inherit; font-weight: 700; }.export-confirm:disabled { opacity: .68; }
</style>
