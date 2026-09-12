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

export async function login(data: LoginReq) {
  const response = await request.post<LoginRes>('/auth/login', data)
  return response.data
}

export async function register(data: RegisterReq) {
  const response = await request.post<LoginRes>('/auth/register', data)
  return response.data
}
