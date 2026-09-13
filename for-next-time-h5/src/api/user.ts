import request from './request'

/** 当前登录用户的个人资料。 */
export interface UserProfile {
  /** 用户主键。 */
  id: number

  /** 用户登录名。 */
  username: string

  /** 已上传头像的同源访问地址；未上传时为 null。 */
  avatarUrl: string | null

  /** 未删除内容的数量。 */
  contentCount: number

  /** 账号创建时间。 */
  createTime: string
}

/** 修改当前用户密码的请求参数。 */
export interface ChangePasswordReq {
  /** 当前密码。 */
  oldPassword: string

  /** 新密码。 */
  newPassword: string

  /** 新密码确认值。 */
  confirmPassword: string
}

/**
 * 获取当前登录用户的个人资料。
 */
export async function getUserProfile() {
  // 1. 调用当前用户资料接口。
  const response = await request.get<UserProfile>('/user/profile')
  // 2. 返回响应拦截器拆出的业务数据。
  return response.data
}

/**
 * 上传当前登录用户的头像并返回最新资料。
 */
export async function uploadAvatar(file: File) {
  // 1. 以 multipart 格式封装头像文件。
  const formData = new FormData()
  formData.append('file', file)
  // 2. 调用当前用户头像上传接口。
  const response = await request.post<UserProfile>('/user/avatar', formData)
  // 3. 返回响应拦截器拆出的最新资料。
  return response.data
}

/**
 * 校验旧密码后更新当前登录用户的密码。
 */
export async function changePassword(data: ChangePasswordReq) {
  // 1. 调用密码修改接口，不接收敏感响应数据。
  await request.put('/user/password', data)
}
