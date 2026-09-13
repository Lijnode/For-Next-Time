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
  categoryName: string | null
  pinned: boolean
  copyCount: number
  lastCopyTime: string | null
  createTime: string
  updateTime: string
  tags: Array<{ id: number; name: string }>
}

export interface ContentDetail extends ContentListItem {
  remark: string | null
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

/**
 * 获取当前用户的一条内容详情。
 */
export async function getContentDetail(id: number) {
  // 1. 请求受当前登录状态保护的内容详情接口。
  const response = await request.get<ContentDetail>(`/contents/${id}`)
  // 2. 返回接口已解包的详情数据。
  return response.data
}

/**
 * 更新当前用户的一条内容。
 */
export async function updateContent(id: number, data: ContentCreateReq) {
  // 1. 提交与新增共用字段模型的编辑请求。
  await request.put(`/contents/${id}`, data)
}

/**
 * 逻辑删除当前用户的一条内容。
 */
export async function deleteContent(id: number) {
  // 1. 请求后端在当前用户数据范围内执行逻辑删除。
  await request.delete(`/contents/${id}`)
}

/**
 * 按关键词搜索当前用户内容。
 */
export async function searchContents(keyword: string, categoryCode: string | null = null) {
  // 1. 请求默认分页大小的内容搜索接口。
  const response = await request.get<{ list: ContentListItem[] }>('/contents/search', {
    params: { keyword, pageNum: 1, pageSize: 20, categoryCode: categoryCode ?? undefined },
  })
  // 2. 返回当前页搜索结果数组。
  return response.data.list
}
