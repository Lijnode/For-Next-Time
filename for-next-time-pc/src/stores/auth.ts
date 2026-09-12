import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login, register, type LoginReq, type LoginRes, type RegisterReq, type UserSimple } from '../api/auth'

const TOKEN_KEY = 'for-next-time-token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY))
  const user = ref<UserSimple | null>(null)
  const isAuthenticated = computed(() => Boolean(token.value))

  /**
   * 使用账号密码登录，并将返回的认证状态写入本地。
   */
  async function loginByPassword(request: LoginReq) {
    // 1. 请求后端校验账号密码。
    applyLogin(await login(request))
  }

  /**
   * 注册账号并保存后端自动签发的登录状态。
   */
  async function registerAccount(request: RegisterReq) {
    // 1. 请求后端创建账号并接收令牌。
    applyLogin(await register(request))
  }

  /**
   * 清除当前用户的内存状态和持久化令牌。
   */
  function logout() {
    // 1. 重置跨页面共享的认证状态。
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  /**
   * 将登录响应同步到 Pinia 和浏览器本地存储。
   */
  function applyLogin(response: LoginRes) {
    // 1. 更新内存中的令牌和用户摘要。
    token.value = response.token
    user.value = response.user
    // 2. 持久化令牌，支持刷新页面后保持登录。
    localStorage.setItem(TOKEN_KEY, response.token)
  }

  return { token, user, isAuthenticated, loginByPassword, registerAccount, logout }
})
