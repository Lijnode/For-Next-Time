import request from './request'

export interface ContentCreateReq {
  content: string
  categoryCode: string | null
  tagNames: string[]
  remark: string | null
}

export interface ContentListItem {
  id: number
  content: string
  categoryCode: string | null
  pinned: boolean
  copyCount: number
  tags: Array<{ id: number; name: string }>
}

export async function createContent(data: ContentCreateReq) {
  // 1. 提交新增内容请求。
  const response = await request.post<{ id: number }>('/contents', data)
  // 2. 返回新建内容的 ID。
  return response.data
}

/**
 * 获取当前用户的第一页内容。
 */
export async function getContentList() {
  // 1. 请求首页所需的默认分页数据。
  const response = await request.get<{ list: ContentListItem[] }>('/contents', { params: { pageNum: 1, pageSize: 20 } })
  // 2. 返回当前页内容数组。
  return response.data.list
}
