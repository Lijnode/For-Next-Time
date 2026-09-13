<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { CircleUserRound, House, Plus } from 'lucide-vue-next'

const route = useRoute()
const showTabBar = computed(() => route.name === 'home' || route.name === 'profile')
</script>

<template>
  <div :class="{ 'with-tab-bar': showTabBar }">
    <RouterView />
  </div>
  <nav v-if="showTabBar" class="tab-bar" aria-label="底部导航">
    <RouterLink to="/" class="tab-item">
      <House class="tab-icon" :size="21" />
      <span>首页</span>
    </RouterLink>
    <RouterLink to="/contents/new" class="create-item" aria-label="先留一句"><Plus :size="26" /></RouterLink>
    <RouterLink to="/profile" class="tab-item">
      <CircleUserRound class="tab-icon" :size="21" />
      <span>我的</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.with-tab-bar :deep(.home-page), .with-tab-bar :deep(.profile-page) { padding-bottom: calc(96px + env(safe-area-inset-bottom)); }
.tab-bar { position: fixed; right: 0; bottom: 0; left: 0; z-index: 10; display: grid; grid-template-columns: 1fr 72px 1fr; align-items: end; width: min(100%, 430px); height: calc(68px + env(safe-area-inset-bottom)); margin: 0 auto; padding: 8px 28px env(safe-area-inset-bottom); border-top: 1px solid var(--color-border); background: rgb(255 255 255 / 96%); box-shadow: 0 -6px 20px rgb(48 39 32 / 4%); }
.tab-item { display: grid; justify-items: center; gap: 3px; min-height: 44px; color: var(--color-muted); font-size: 11px; text-decoration: none; }.tab-item.router-link-exact-active { color: var(--color-brand); }.tab-icon { line-height: 20px; }
.create-item { display: grid; width: 54px; height: 54px; margin: -26px auto 0; place-items: center; border: 4px solid var(--color-page); border-radius: 18px; background: var(--color-brand); color: #fff; line-height: 1; text-decoration: none; box-shadow: 0 6px 14px rgb(255 111 78 / 26%); }
</style>
