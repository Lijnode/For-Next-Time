import request from './request'

export interface UserSimple {
  id: number
  username: string
}

export interface LoginRes {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserSimple
}

export interface LoginReq {
  username: string
  password: string
}

export interface RegisterReq extends LoginReq {
  confirmPassword: string
}

/**
 * 提交账号密码并取得登录令牌。
 */
export async function login(data: LoginReq) {
  // 1. 调用统一请求实例完成登录。
  const response = await request.post<LoginRes>('/auth/login', data)
  // 2. 返回已被响应拦截器拆出的业务数据。
  return response.data
}

/**
 * 提交注册信息，并取得注册后自动登录的令牌。
 */
export async function register(data: RegisterReq) {
  // 1. 调用统一请求实例完成注册。
  const response = await request.post<LoginRes>('/auth/register', data)
  // 2. 返回已被响应拦截器拆出的业务数据。
  return response.data
}
