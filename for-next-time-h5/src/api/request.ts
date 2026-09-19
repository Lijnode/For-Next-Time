import axios from 'axios'
import { replaceWithLogin } from '../platform/navigation'
import { getToken, removeToken } from '../platform/storage'
import type { ApiResult } from '../types/api'

const request = axios.create({
  baseURL: import.meta.env.DEV ? '/api/v1' : '/time/api/v1',
  timeout: 10_000,
})

request.interceptors.request.use((config) => {
  // 1. 通过平台层读取已保存的登录令牌。
  const token = getToken()
  // 2. 有令牌时按 Bearer 标准附加到请求头。
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use((response) => {
  // 1. 文件下载使用非 JSON 响应，直接交由调用方处理二进制内容。
  if (response.config.responseType === 'blob') {
    return response
  }
  // 2. 读取后端统一响应结构。
  const result = response.data as ApiResult<unknown>
  if (result.code !== 0) {
    return Promise.reject(new Error(result.message))
  }
  // 3. 对成功响应仅向 API 层暴露业务数据。
  response.data = result.data
  return response
}, (error) => {
  // 1. 认证失效时通过平台层清理令牌并跳转登录页。
  if (error.response?.status === 401) {
    removeToken()
    replaceWithLogin()
  }
  // 2. 优先使用后端返回的可读错误信息。
  const message = error.response?.data?.message
  return Promise.reject(new Error(typeof message === 'string' ? message : '请求失败，请稍后重试'))
})

export default request
