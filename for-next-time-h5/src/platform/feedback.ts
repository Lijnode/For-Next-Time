import { showConfirmDialog, showNotify } from 'vant'

/**
 * 以顶部悬浮通知展示移动端成功反馈。
 */
export function showSuccess(message: string) {
  // 1. 使用不遮挡操作区的顶部成功通知展示结果。
  showNotify({
    type: 'success',
    message,
    position: 'top',
    duration: 2200,
    lockScroll: false,
    color: '#267561',
    background: '#eaf8f1',
    className: 'app-feedback app-feedback--success',
  })
}

/**
 * 以顶部悬浮通知展示移动端错误反馈。
 */
export function showError(message: string) {
  // 1. 使用显眼但不刺眼的错误通知提示可恢复问题。
  showNotify({
    type: 'danger',
    message,
    position: 'top',
    duration: 2800,
    lockScroll: false,
    color: '#bb4b3d',
    background: '#fff1ed',
    className: 'app-feedback app-feedback--error',
  })
}

export async function confirm(message: string) {
  try {
    await showConfirmDialog({ message })
    return true
  } catch {
    return false
  }
}
