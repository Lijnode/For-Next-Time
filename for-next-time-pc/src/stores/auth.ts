import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login, register, type LoginReq, type LoginRes, type RegisterReq, type UserSimple } from '../api/auth'

const TOKEN_KEY = 'for-next-time-token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY))
  const user = ref<UserSimple | null>(null)
  const isAuthenticated = computed(() => Boolean(token.value))

  async function loginByPassword(request: LoginReq) {
    applyLogin(await login(request))
  }

  async function registerAccount(request: RegisterReq) {
    applyLogin(await register(request))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  function applyLogin(response: LoginRes) {
    token.value = response.token
    user.value = response.user
    localStorage.setItem(TOKEN_KEY, response.token)
  }

  return { token, user, isAuthenticated, loginByPassword, registerAccount, logout }
})
