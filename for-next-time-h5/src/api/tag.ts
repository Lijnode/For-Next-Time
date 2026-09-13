import request from './request'

export interface TagItem {
  id: number
  name: string
}

/**
 * 获取当前用户已有的可复用标签。
 */
export async function getTagList(keyword = '') {
  // 1. 请求当前用户范围内的标签列表。
  const response = await request.get<TagItem[]>('/tags', { params: { keyword, limit: 20 } })
  // 2. 返回接口已解包的标签数据。
  return response.data
}
