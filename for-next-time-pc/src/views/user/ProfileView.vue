<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronRight, Download, House, Info, KeyRound, LogOut, SquarePen } from 'lucide-vue-next'
import { getUserProfile, uploadAvatar, type UserProfile } from '../../api/user'
import { exportContents } from '../../api/content'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile>()
const loading = ref(false)
const uploadingAvatar = ref(false)
const aboutDialogVisible = ref(false)
const exportDialogVisible = ref(false)
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
const avatarUploader = ref<UploadInstance>()
const cropDialogVisible = ref(false)
const cropPreviewUrl = ref('')
const cropImage = ref<HTMLImageElement>()
const cropNaturalWidth = ref(0)
const cropNaturalHeight = ref(0)
const cropBox = ref({ x: 0, y: 0, size: 0 })
const cropDragMode = ref<'move' | 'resize' | null>(null)
const cropDragStart = ref({ x: 0, y: 0, cropX: 0, cropY: 0, cropSize: 0 })
const cropViewportSize = 300
const cropMinSize = 96

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
 * 校验用户选择的原始头像格式和大小。
 */
function validateAvatar(file: File) {
  // 1. 检查文件是否符合后端允许的 JPEG、PNG 与 2MB 限制。
  const valid = ['image/jpeg', 'image/png'].includes(file.type) && file.size <= 2 * 1024 * 1024
  // 2. 阻止不合格文件进入上传流程并展示一致的错误提示。
  if (!valid) {
    ElMessage.error('头像仅支持 JPG 或 PNG，且不能超过 2MB')
  }
  return valid
}

/**
 * 接收选中的原始头像，并打开裁剪面板而不立即上传。
 *
 * @param file Element Plus 提供的本次文件信息
 */
function selectAvatar(file: UploadFile) {
  // 1. 取出原始文件并在打开裁剪面板前校验约束。
  const rawFile = file.raw
  if (!rawFile || !validateAvatar(rawFile)) {
    avatarUploader.value?.clearFiles()
    return
  }
  // 2. 释放旧预览并创建本次图片的本地预览地址。
  revokeCropPreviewUrl()
  cropPreviewUrl.value = URL.createObjectURL(rawFile)
  cropDialogVisible.value = true
}

/**
 * 计算图片在裁剪面板中的等比完整预览比例。
 */
function getCropPreviewScale() {
  // 1. 宽高未加载前不计算比例，避免出现无效样式。
  if (!cropNaturalWidth.value || !cropNaturalHeight.value) {
    return 0
  }
  // 2. 取较小比例，确保整张图片完整显示在裁剪面板中。
  return Math.min(cropViewportSize / cropNaturalWidth.value, cropViewportSize / cropNaturalHeight.value)
}

/**
 * 获取完整预览图在裁剪面板中的左上角坐标。
 */
function getCropImageBounds() {
  // 1. 根据等比预览尺寸计算图片自身的可操作边界。
  const scale = getCropPreviewScale()
  const width = cropNaturalWidth.value * scale
  const height = cropNaturalHeight.value * scale
  return { left: 0, top: 0, width, height }
}

/**
 * 在预览图加载后初始化位于图片中央的方形裁剪框。
 *
 * @param event 预览图加载事件
 */
function initializeCrop(event: Event) {
  // 1. 读取浏览器已解析的图片原始尺寸。
  const image = event.target as HTMLImageElement
  cropNaturalWidth.value = image.naturalWidth
  cropNaturalHeight.value = image.naturalHeight
  // 2. 让初始裁剪框以正方形形式居中覆盖图片可选区域。
  const bounds = getCropImageBounds()
  const size = Math.min(bounds.width, bounds.height) * 0.78
  cropBox.value = { x: bounds.left + (bounds.width - size) / 2, y: bounds.top + (bounds.height - size) / 2, size }
}

/**
 * 记录裁剪框拖动或缩放开始时的状态。
 *
 * @param event 指针按下事件
 * @param mode 当前操作是移动框选区域还是拉角缩放
 */
function startCropBoxDrag(event: PointerEvent, mode: 'move' | 'resize') {
  // 1. 未加载图片时不允许操作裁剪框。
  if (!cropNaturalWidth.value || !cropNaturalHeight.value) {
    return
  }
  // 2. 记录起点并捕获指针，保证越出裁剪框仍可连续操作。
  cropDragMode.value = mode
  cropDragStart.value = { x: event.clientX, y: event.clientY, cropX: cropBox.value.x, cropY: cropBox.value.y, cropSize: cropBox.value.size }
  ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}

/**
 * 按指针移动裁剪框，或通过右下角调整方形裁剪框的尺寸。
 *
 * @param event 指针移动事件
 */
function moveCropBoxDrag(event: PointerEvent) {
  // 1. 没有激活操作时保持当前框选状态。
  if (!cropDragMode.value) {
    return
  }
  // 2. 获取图片边界，以确保裁剪框始终位于有效图片区域内。
  const bounds = getCropImageBounds()
  const deltaX = event.clientX - cropDragStart.value.x
  const deltaY = event.clientY - cropDragStart.value.y
  if (cropDragMode.value === 'move') {
    cropBox.value.x = Math.min(bounds.left + bounds.width - cropBox.value.size, Math.max(bounds.left, cropDragStart.value.cropX + deltaX))
    cropBox.value.y = Math.min(bounds.top + bounds.height - cropBox.value.size, Math.max(bounds.top, cropDragStart.value.cropY + deltaY))
    return
  }
  // 3. 从右下角缩放，保持正方形并限制在图片边界和最小可用尺寸内。
  const maxSize = Math.min(bounds.left + bounds.width - cropDragStart.value.cropX, bounds.top + bounds.height - cropDragStart.value.cropY)
  const nextSize = cropDragStart.value.cropSize + Math.max(deltaX, deltaY)
  const minSize = Math.min(cropMinSize, maxSize)
  cropBox.value.size = Math.min(maxSize, Math.max(minSize, nextSize))
}

/**
 * 结束当前裁剪框拖动或缩放操作。
 */
function endCropBoxDrag() {
  // 1. 清除操作状态，保留最后一次有效的裁剪框位置与尺寸。
  cropDragMode.value = null
}

/**
 * 将裁剪区域绘制为固定尺寸的 JPEG 文件。
 */
function createCroppedAvatar() {
  // 1. 确认预览图已加载，才能取得可绘制的图像数据。
  if (!cropImage.value) {
    throw new Error('图片尚未加载完成')
  }
  // 2. 将当前框选区域映射回原图坐标，并绘制为统一的 512 像素头像。
  const scale = getCropPreviewScale()
  const bounds = getCropImageBounds()
  const sourceSize = cropBox.value.size / scale
  const sourceX = (cropBox.value.x - bounds.left) / scale
  const sourceY = (cropBox.value.y - bounds.top) / scale
  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 512
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('无法创建头像画布')
  }
  context.drawImage(cropImage.value, sourceX, sourceY, sourceSize, sourceSize, 0, 0, canvas.width, canvas.height)
  // 3. 输出较小的 JPEG 文件，保证仍符合现有上传大小限制。
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
 * 确认裁剪结果并上传生成后的头像文件。
 */
async function confirmAvatarCrop() {
  // 1. 生成当前裁剪框中的方形头像并锁定交互。
  uploadingAvatar.value = true
  try {
    const croppedAvatar = await createCroppedAvatar()
    // 2. 复用现有接口上传裁剪结果，并刷新页面资料。
    profile.value = await uploadAvatar(croppedAvatar)
    ElMessage.success('头像已更新')
  } catch (error) {
    // 3. 将可读错误反馈给用户，并保留裁剪面板以便重新调整。
    const message = error instanceof Error ? error.message : '头像上传失败，请稍后重试'
    ElMessage.error(message)
    return
  } finally {
    // 4. 上传完成后恢复头像入口的交互能力。
    uploadingAvatar.value = false
  }
  // 5. 仅在上传成功后关闭裁剪面板并清理临时资源。
  closeCropDialog()
}

/**
 * 释放当前图片预览地址，避免重复选择图片时保留浏览器内存。
 */
function revokeCropPreviewUrl() {
  // 1. 有旧地址时释放对应的浏览器对象 URL。
  if (cropPreviewUrl.value) {
    URL.revokeObjectURL(cropPreviewUrl.value)
  }
  // 2. 重置地址与原始尺寸，避免下次选择图片沿用旧状态。
  cropPreviewUrl.value = ''
  cropNaturalWidth.value = 0
  cropNaturalHeight.value = 0
}

/**
 * 关闭裁剪面板并清除本次选择的临时图片。
 */
function closeCropDialog() {
  // 1. 关闭面板并复位上传组件，允许再次选择相同文件。
  cropDialogVisible.value = false
  avatarUploader.value?.clearFiles()
  // 2. 释放预览资源，避免本地对象 URL 泄漏。
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
 * 打开产品介绍弹窗，展示与移动端一致的应用说明。
 */
function openAboutDialog() {
  // 1. 仅切换本页弹窗状态，不离开当前个人页。
  aboutDialogVisible.value = true
}

/**
 * 打开内容导出面板并保留本次已勾选字段。
 */
function openExportDialog() {
  // 1. 切换导出弹窗状态，默认字段仅在首次进入时初始化。
  exportDialogVisible.value = true
}

/**
 * 请求导出文件并交由浏览器保存到本地。
 */
async function downloadContentExport() {
  // 1. 在发起下载前阻止空字段导出，避免无内容的文件。
  if (!exportFields.value.length) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }
  exporting.value = true
  try {
    // 2. 请求当前用户的 TXT 文件，并通过临时链接触发浏览器保存。
    const file = await exportContents(exportFields.value)
    const objectUrl = URL.createObjectURL(file.blob)
    const anchor = document.createElement('a')
    anchor.href = objectUrl
    anchor.download = file.fileName
    document.body.append(anchor)
    anchor.click()
    anchor.remove()
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0)
    ElMessage.success('内容已导出')
    exportDialogVisible.value = false
  } catch (error) {
    // 3. 下载失败时保留字段选择，便于用户直接重试。
    ElMessage.error(error instanceof Error ? error.message : '导出失败，请稍后重试')
  } finally {
    // 4. 无论结果如何都恢复导出按钮可用状态。
    exporting.value = false
  }
}

/**
 * 返回内容首页继续浏览已保存的内容。
 */
async function backHome() {
  // 1. 返回当前用户的内容首页。
  await router.push({ name: 'home' })
}

/**
 * 清除当前账户登录状态并返回登录页。
 */
async function logout() {
  // 1. 清理 Pinia 与浏览器本地存储中的认证信息。
  authStore.logout()
  // 2. 替换路由，避免浏览器返回受保护的资料页面。
  await router.replace({ name: 'login' })
}

// 1. 页面进入时加载当前账户资料。
onMounted(loadProfile)
</script>

<template>
  <main class="profile-page">
    <section class="profile-card" v-loading="loading">
      <template v-if="profile">
        <div class="profile-head">
          <el-upload
            ref="avatarUploader"
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
            :disabled="uploadingAvatar"
            :on-change="selectAvatar"
          >
            <span class="profile-mark" :class="{ 'is-uploading': uploadingAvatar }">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="`${profile.username} 的头像`">
              <span v-else>{{ profile.username.slice(0, 1) }}</span>
              <span class="avatar-edit" aria-hidden="true"><SquarePen :size="14" /></span>
            </span>
          </el-upload>
          <div>
            <h1>{{ profile.username }}</h1>
            <p>已经留下 {{ profile.contentCount }} 条内容</p>
          </div>
        </div>
        <div class="menu">
          <div class="menu-item"><span>内容总数</span><strong>{{ profile.contentCount }}</strong></div>
          <button class="menu-item" type="button" @click="openPasswordForm"><span>修改密码</span><KeyRound :size="17" /></button>
          <button class="menu-item" type="button" @click="openExportDialog"><span>导出内容</span><Download :size="17" /></button>
          <div class="menu-item"><span>加入时间</span><strong>{{ profile.createTime }}</strong></div>
        </div>
        <section class="about-menu" aria-label="产品信息">
          <button class="menu-item about-row" type="button" @click="openAboutDialog"><span><Info :size="17" />关于下次一定</span><ChevronRight :size="17" /></button>
        </section>
        <div class="actions">
          <el-button @click="backHome"><span class="action-content"><House :size="16" /><span>返回首页</span></span></el-button>
          <el-button class="logout-button" text @click="logout"><span class="action-content"><LogOut :size="16" /><span>退出登录</span></span></el-button>
        </div>
      </template>
    </section>
    <el-dialog v-model="aboutDialogVisible" width="440px" align-center class="about-dialog">
      <section class="about-dialog-content">
        <img class="brand-mark" src="/brand-mark.png?v=3" alt="">
        <h2>下次一定</h2>
        <p class="english-name">FOR NEXT TIME</p>
        <p class="slogan">这次先留着，下次一定用得上</p>
        <p class="intro">一个用于收藏、搜索和快速复用句子与内容素材的私人小工具</p>
        <p class="version">V1.0</p>
      </section>
      <template #footer><el-button type="primary" @click="aboutDialogVisible = false">知道了</el-button></template>
    </el-dialog>
    <el-dialog v-model="exportDialogVisible" width="440px" align-center class="export-dialog">
      <template #header><h2 class="export-dialog-title">导出内容</h2></template>
      <section class="export-dialog-content">
        <p>选择要写入文件的字段</p>
        <el-checkbox-group v-model="exportFields" class="export-fields">
          <el-checkbox v-for="field in exportFieldOptions" :key="field.value" :value="field.value">{{ field.label }}</el-checkbox>
        </el-checkbox-group>
<!--        <div class="export-format"><span>文件格式</span><strong>TXT 文本文件</strong></div>-->
      </section>
      <template #footer><el-button :disabled="exporting" @click="exportDialogVisible = false">取消</el-button><el-button type="primary" :loading="exporting" @click="downloadContentExport">导出 TXT</el-button></template>
    </el-dialog>
    <el-dialog v-model="cropDialogVisible" width="520px" align-center :close-on-click-modal="false" :close-on-press-escape="!uploadingAvatar" class="avatar-crop-dialog" @closed="closeCropDialog">
      <template #header><h2 class="crop-dialog-title">裁剪头像</h2></template>
      <section class="crop-dialog-content">
        <p>拖动框选区域，或拉动右下角调整裁剪范围</p>
        <div class="crop-viewport" :style="{ width: `${getCropImageBounds().width}px`, height: `${getCropImageBounds().height}px` }" @pointermove="moveCropBoxDrag" @pointerup="endCropBoxDrag" @pointercancel="endCropBoxDrag">
          <img v-if="cropPreviewUrl" ref="cropImage" class="crop-source-image" :src="cropPreviewUrl" :style="{ width: `${getCropImageBounds().width}px`, height: `${getCropImageBounds().height}px`, left: `${getCropImageBounds().left}px`, top: `${getCropImageBounds().top}px` }" draggable="false" alt="待裁剪头像" @load="initializeCrop">
          <div v-if="cropBox.size" class="crop-selection" :class="{ 'is-resizing': cropDragMode === 'resize' }" :style="{ width: `${cropBox.size}px`, height: `${cropBox.size}px`, left: `${cropBox.x}px`, top: `${cropBox.y}px` }" @pointerdown.stop="startCropBoxDrag($event, 'move')">
            <span class="crop-resize-handle" aria-label="调整裁剪范围" @pointerdown.stop="startCropBoxDrag($event, 'resize')"></span>
          </div>
        </div>
      </section>
      <template #footer><el-button :disabled="uploadingAvatar" @click="closeCropDialog">取消</el-button><el-button type="primary" :loading="uploadingAvatar" @click="confirmAvatarCrop">使用此头像</el-button></template>
    </el-dialog>
  </main>
</template>

<style scoped>
.profile-page { width: min(760px, calc(100% - 64px)); margin: 0 auto; padding: 42px 0 72px; }
.profile-card { padding: 32px; border: 1px solid var(--color-border); border-radius: 22px; background: var(--color-surface); box-shadow: 0 10px 25px rgb(31 33 33 / 4%); }
.profile-head { display: flex; align-items: center; gap: 16px; }
.avatar-uploader :deep(.el-upload) { display: block; border-radius: 20px; cursor: pointer; outline: none; }
.avatar-uploader :deep(.el-upload:focus-visible) { outline: 2px solid var(--color-brand); outline-offset: 3px; }
.profile-mark { position: relative; display: grid; width: 62px; height: 62px; place-items: center; overflow: hidden; border-radius: 20px; background: var(--color-brand); color: #fff; font-size: 22px; font-weight: 800; box-shadow: 0 7px 15px rgb(255 111 78 / 18%); transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease; }
.avatar-uploader:hover .profile-mark:not(.is-uploading) { transform: translateY(-1px); box-shadow: 0 10px 19px rgb(255 111 78 / 27%); }
.profile-mark.is-uploading { cursor: wait; opacity: 0.75; }
.profile-mark img { width: 100%; height: 100%; object-fit: cover; }
.avatar-edit { position: absolute; right: 4px; bottom: 4px; z-index: 1; display: grid; width: 18px; height: 18px; place-items: center; color: #24324A; font-size: 14px; }
.avatar-uploader:hover .avatar-edit { color: #151D2B; }
.profile-head h1 { margin: 0; color: var(--color-text-primary); font-size: 21px; }
.profile-head p { margin: 6px 0 0; color: var(--color-text-tertiary); font-size: 13px; }
.menu { overflow: hidden; margin-top: 26px; border: 1px solid var(--color-border); border-radius: 18px; }
.menu-item { display: flex; width: 100%; align-items: center; justify-content: space-between; min-height: 55px; padding: 0 18px; border: 0; border-bottom: 1px solid #efefeb; background: #fff; color: var(--color-text-secondary); font: inherit; font-size: 14px; text-align: left; }
.menu-item:last-child { border-bottom: 0; }
.menu-item strong { color: var(--color-text-primary); font-weight: 700; }
.menu-item button { cursor: pointer; }
.about-menu { overflow: hidden; margin-top: 16px; border: 1px solid var(--color-border); border-radius: 18px; }
.about-row { cursor: pointer; color: var(--color-text-primary); }
.about-row > span { display: inline-flex; align-items: center; gap: 9px; }
.about-row > svg { color: var(--color-text-tertiary); }
.actions { display: flex; align-items: center; gap: 10px; margin-top: 22px; }
.actions :deep(.el-button) { min-height: 40px; border-radius: 12px; }
.action-content { display: inline-flex; align-items: center; gap: 9px; }
.logout-button { margin-left: auto; color: var(--color-text-tertiary); }
.about-dialog-content { padding: 20px 16px 8px; text-align: center; }
.brand-mark { display: block; width: 68px; height: 68px; margin: 0 auto 17px; }
.about-dialog-content h2 { margin: 0; color: var(--color-text-primary); font-size: 24px; }.english-name { margin: 7px 0 30px; color: var(--color-brand); font-size: 11px; font-weight: 750; letter-spacing: .16em; }.slogan { margin: 0; color: var(--color-text-primary); font-size: 16px; font-weight: 600; }.intro { max-width: 270px; margin: 16px auto 0; color: var(--color-text-tertiary); font-size: 13px; line-height: 1.75; }.version { display: inline-block; margin: 30px 0 0; padding: 5px 10px; border-radius: 999px; background: #fff0eb; color: var(--color-brand); font-size: 12px; font-weight: 700; }
:global(.about-dialog) { border-radius: 20px; }
:global(.about-dialog .el-dialog__header) { margin-right: 0; padding: 20px 24px 0; }
:global(.about-dialog .el-dialog__title) { color: var(--color-text-primary); font-weight: 700; }
:global(.about-dialog .el-dialog__body) { padding: 0 24px; }
:global(.about-dialog .el-dialog__footer) { padding: 8px 24px 22px; text-align: center; }
:global(.about-dialog .el-dialog__footer .el-button) { min-width: 106px; height: 40px; border-radius: 12px; font-weight: 700; }
.export-dialog-title { margin: 0; color: var(--color-text-primary); font-size: 18px; font-weight: 600; }
.export-dialog-content > p { margin: 0; color: var(--color-text-tertiary); font-size: 13px; line-height: 1.7; }
.export-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 10px; margin-top: 22px; }
.export-fields :deep(.el-checkbox) { margin-right: 0; color: var(--color-text-secondary); }
.export-fields :deep(.el-checkbox__label) { font-size: 14px; }
.export-format { display: flex; align-items: center; justify-content: space-between; margin-top: 22px; padding-top: 16px; border-top: 1px solid #efefeb; color: var(--color-text-tertiary); font-size: 13px; }.export-format strong { color: var(--color-text-secondary); font-weight: 600; }
:global(.export-dialog) { border-radius: 20px; }
:global(.export-dialog .el-dialog__header) { margin-right: 0; padding: 22px 24px 0; }
:global(.export-dialog .el-dialog__body) { padding: 16px 24px 0; }
:global(.export-dialog .el-dialog__footer) { padding: 22px 24px; }
:global(.export-dialog .el-dialog__footer .el-button) { min-width: 92px; height: 40px; border-radius: 12px; font-weight: 700; }
.crop-dialog-title { margin: 0; color: var(--color-text-primary); font-size: 19px; }
.crop-dialog-content > p { margin: 0 0 18px; color: var(--color-text-tertiary); font-size: 13px; }
.crop-viewport { position: relative; max-width: 300px; max-height: 300px; margin: 0 auto; overflow: hidden; border-radius: 20px; touch-action: none; user-select: none; }
.crop-source-image { position: absolute; display: block; border-radius: inherit; }
.crop-selection { position: absolute; box-sizing: border-box; border: 2px solid #fff; cursor: move; box-shadow: 0 0 0 200vmax rgb(20 28 26 / 46%); }
.crop-selection::before, .crop-selection::after { position: absolute; top: 50%; left: 50%; width: 1px; height: 100%; background: rgb(255 255 255 / 58%); content: ''; transform: translate(-50%, -50%); pointer-events: none; }
.crop-selection::after { width: 100%; height: 1px; }
.crop-resize-handle { position: absolute; right: -7px; bottom: -7px; width: 14px; height: 14px; border: 2px solid #fff; border-radius: 50%; background: var(--color-brand); cursor: nwse-resize; }
:global(.avatar-crop-dialog) { border-radius: 20px; }
:global(.avatar-crop-dialog .el-dialog__header) { margin-right: 0; padding: 22px 24px 0; }
:global(.avatar-crop-dialog .el-dialog__body) { padding: 20px 24px 4px; }
:global(.avatar-crop-dialog .el-dialog__footer) { padding: 14px 24px 22px; text-align: center; }
:global(.avatar-crop-dialog .el-dialog__footer .el-button) { min-width: 106px; height: 40px; border-radius: 12px; font-weight: 700; }
</style>
