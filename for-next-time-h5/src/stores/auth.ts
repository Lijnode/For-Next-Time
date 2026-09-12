import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login, register, type LoginReq, type LoginRes, type RegisterReq, type UserSimple } from '../api/auth'
import { getToken, removeToken, setToken } from '../platform/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
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
    removeToken()
  }

  function applyLogin(response: LoginRes) {
    token.value = response.token
    user.value = response.user
    setToken(response.token)
  }

  return { token, user, isAuthenticated, loginByPassword, registerAccount, logout }
})
