<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { CircleUserRound, Clock3, House, Plus } from 'lucide-vue-next'

const route = useRoute()
const showTabBar = computed(() => route.name === 'home' || route.name === 'content-recent' || route.name === 'profile')
</script>

<template>
  <div :class="{ 'with-tab-bar': showTabBar }">
    <RouterView />
  </div>
  <RouterLink v-if="showTabBar" to="/contents/new" class="floating-create" aria-label="先留一句"><Plus :size="25" /></RouterLink>
  <nav v-if="showTabBar" class="tab-bar" aria-label="底部导航">
    <RouterLink to="/" class="tab-item">
      <House class="tab-icon" :size="21" />
      <span>首页</span>
    </RouterLink>
    <RouterLink to="/recent" class="tab-item">
      <Clock3 class="tab-icon" :size="21" />
      <span>历史</span>
    </RouterLink>
    <RouterLink to="/profile" class="tab-item">
      <CircleUserRound class="tab-icon" :size="21" />
      <span>我的</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.with-tab-bar :deep(.home-page), .with-tab-bar :deep(.recent-page), .with-tab-bar :deep(.profile-page) { padding-bottom: calc(108px + env(safe-area-inset-bottom)); }
.tab-bar { position: fixed; right: 12px; bottom: calc(12px + env(safe-area-inset-bottom)); left: 12px; z-index: 10; display: grid; grid-template-columns: repeat(3, 1fr); align-items: center; width: auto; max-width: 406px; height: 64px; margin: 0 auto; padding: 6px 18px; border: 1px solid var(--color-border); border-radius: 22px; background: rgb(255 255 255 / 96%); box-shadow: 0 8px 24px rgb(48 39 32 / 10%); backdrop-filter: blur(12px); }
.tab-item { display: grid; justify-items: center; gap: 3px; min-height: 44px; color: var(--color-muted); font-size: 11px; text-decoration: none; }.tab-item.router-link-exact-active { color: var(--color-brand); }.tab-icon { line-height: 20px; }
.floating-create { position: fixed; right: max(20px, calc((100vw - 430px) / 2 + 16px)); bottom: calc(100px + env(safe-area-inset-bottom)); z-index: 11; display: grid; width: 56px; height: 56px; place-items: center; border: 4px solid var(--color-page); border-radius: 50%; background: var(--color-brand); color: #fff; line-height: 1; text-decoration: none; box-shadow: 0 8px 18px rgb(255 111 78 / 30%); }
</style>
