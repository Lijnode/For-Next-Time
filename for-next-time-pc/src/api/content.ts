import request from './request'

export interface ContentCreateReq {
  content: string
  categoryCode: string | null
  tagNames: string[]
  remark: string | null
}

export async function createContent(data: ContentCreateReq) {
  // 1. 提交新增内容请求。
  const response = await request.post<{ id: number }>('/contents', data)
  // 2. 返回新建内容的 ID。
  return response.data
}
