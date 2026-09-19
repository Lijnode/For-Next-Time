/**
 * 将服务端返回的文件保存到当前浏览器设备。
 *
 * @param blob     服务端返回的文件二进制内容
 * @param fileName 建议保存时使用的文件名
 */
export function downloadFile(blob: Blob, fileName: string) {
  // 1. 为当前文件创建短生命周期的本地下载地址。
  const objectUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  anchor.style.display = 'none'

  // 2. 临时挂载并触发浏览器下载，兼容移动浏览器的用户手势限制。
  document.body.append(anchor)
  anchor.click()
  anchor.remove()

  // 3. 下载任务已交给浏览器后释放对象地址，避免内存泄漏。
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0)
}
