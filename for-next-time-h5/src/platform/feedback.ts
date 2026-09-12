import { showConfirmDialog, showToast } from 'vant'

export function showSuccess(message: string) {
  showToast({ type: 'success', message })
}

export function showError(message: string) {
  showToast({ type: 'fail', message })
}

export async function confirm(message: string) {
  try {
    await showConfirmDialog({ message })
    return true
  } catch {
    return false
  }
}
