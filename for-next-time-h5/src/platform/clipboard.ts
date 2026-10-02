/**
 * 在安全上下文优先使用标准剪贴板 API；HTTP 临时部署时兼容旧式浏览器复制。
 */
export async function copyText(text: string) {
  // 1. HTTPS 等安全上下文优先使用浏览器标准剪贴板能力。
  if (window.isSecureContext && navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  // 2. HTTP 临时访问时在用户点击事件内使用兼容复制方案。
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.append(textarea)
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)
  const copied = document.execCommand('copy')
  textarea.remove()

  // 3. 浏览器拒绝兼容复制时向页面返回明确失败信息。
  if (!copied) throw new Error('当前浏览器无法复制，请使用 HTTPS 域名访问')
}
