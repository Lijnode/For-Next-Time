<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const showNavigation = computed(() => route.meta.requiresAuth === true)
</script>

<template>
  <header v-if="showNavigation" class="top-navigation">
    <RouterLink class="brand" to="/">
      <span>FOR NEXT TIME</span>
      <strong>下次一定</strong>
    </RouterLink>
    <nav aria-label="主导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/search">找一句</RouterLink>
    </nav>
    <div class="header-actions">
      <RouterLink class="create-link" to="/contents/new">先留一句</RouterLink>
      <RouterLink class="profile-link" to="/profile">我的</RouterLink>
    </div>
  </header>
  <RouterView />
</template>

<style scoped>
.top-navigation { display: flex; align-items: center; justify-content: space-between; width: min(1240px, calc(100% - 64px)); min-height: 72px; margin: 0 auto; border-bottom: 1px solid var(--color-divider); }
.brand { display: flex; flex-direction: column; color: var(--color-text-primary); text-decoration: none; }.brand span { color: var(--color-brand); font-size: 10px; font-weight: 700; letter-spacing: .14em; }.brand strong { margin-top: 3px; font-size: 18px; }
nav { display: flex; gap: 28px; margin-right: auto; margin-left: 80px; } nav a, .profile-link { color: var(--color-text-secondary); text-decoration: none; } nav a.router-link-exact-active, .profile-link.router-link-exact-active { color: var(--color-brand); font-weight: 600; }
.header-actions { display: flex; align-items: center; gap: 20px; }.create-link { padding: 10px 16px; border-radius: 999px; background: var(--color-brand); color: #fff; text-decoration: none; }
</style>
