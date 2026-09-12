import axios from 'axios'
import type { ApiResult } from '../types/api'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('for-next-time-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use((response) => {
  const result = response.data as ApiResult<unknown>
  if (result.code !== 0) {
    return Promise.reject(new Error(result.message))
  }
  response.data = result.data
  return response
}, (error) => {
  if (error.response?.status === 401) {
    localStorage.removeItem('for-next-time-token')
    if (window.location.pathname !== '/login') {
      window.location.assign('/login')
    }
  }
  const message = error.response?.data?.message
  return Promise.reject(new Error(typeof message === 'string' ? message : '请求失败，请稍后重试'))
})

export default request
