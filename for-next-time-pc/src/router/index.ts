import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/auth/LoginView.vue'
import RegisterView from '../views/auth/RegisterView.vue'
import ContentCreateView from '../views/content/ContentCreateView.vue'
import HomeView from '../views/home/HomeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
    },
    { path: '/contents/new', name: 'content-create', component: ContentCreateView, meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => {
  // 1. 读取当前持久化的认证状态。
  const authStore = useAuthStore()
  // 2. 未登录访问受保护页面时重定向到登录页。
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }
  // 3. 已登录用户不应重复进入认证页。
  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    return { name: 'home' }
  }
})

export default router
