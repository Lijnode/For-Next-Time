import axios from 'axios'
import type { ApiResult } from '../types/api'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
})

request.interceptors.request.use((config) => {
  // 1. 读取本地已保存的登录令牌。
  const token = localStorage.getItem('for-next-time-token')
  // 2. 有令牌时按 Bearer 标准附加到请求头。
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use((response) => {
  // 1. 读取后端统一响应结构。
  const result = response.data as ApiResult<unknown>
  if (result.code !== 0) {
    return Promise.reject(new Error(result.message))
  }
  // 2. 对成功响应仅向 API 层暴露业务数据。
  response.data = result.data
  return response
}, (error) => {
  // 1. 认证失效时清除本地令牌并返回登录页。
  if (error.response?.status === 401) {
    localStorage.removeItem('for-next-time-token')
    if (window.location.pathname !== '/login') {
      window.location.assign('/login')
    }
  }
  // 2. 优先使用后端返回的可读错误信息。
  const message = error.response?.data?.message
  return Promise.reject(new Error(typeof message === 'string' ? message : '请求失败，请稍后重试'))
})

export default request
