<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { CircleUserRound, House, Plus, Search } from 'lucide-vue-next'

const route = useRoute()
const showNavigation = computed(() => route.meta.requiresAuth === true)
</script>

<template>
  <header v-if="showNavigation" class="top-navigation-shell">
    <div class="top-navigation">
      <div class="brand-area">
        <RouterLink class="brand-mark" to="/" aria-label="下次一定首页">次</RouterLink>
        <RouterLink class="brand-copy" to="/">
          <strong>下次一定</strong>
          <span>FOR NEXT TIME</span>
        </RouterLink>
      </div>
      <nav aria-label="主导航">
        <RouterLink to="/"><House :size="16" />首页</RouterLink>
        <RouterLink to="/search"><Search :size="16" />找一句</RouterLink>
      </nav>
      <div class="header-actions">
        <RouterLink class="create-link" to="/contents/new"><Plus :size="17" />先留一句</RouterLink>
        <RouterLink class="profile-link" to="/profile" aria-label="我的"><CircleUserRound :size="21" /></RouterLink>
      </div>
    </div>
  </header>
  <RouterView />
</template>

<style scoped>
.top-navigation-shell {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid rgb(231 232 228 / 85%);
  background: rgb(247 247 244 / 88%);
  backdrop-filter: blur(14px);
}

.top-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(1240px, calc(100% - 64px));
  min-height: 72px;
  margin: 0 auto;
}

.brand-area,
.brand-copy,
.header-actions,
nav a,
.create-link {
  display: flex;
  align-items: center;
}

.brand-area {
  gap: 10px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: var(--color-brand);
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  text-decoration: none;
  box-shadow: 0 5px 12px rgb(255 111 78 / 22%);
}

.brand-copy {
  flex-direction: column;
  align-items: flex-start;
  color: var(--color-text-primary);
  line-height: 1;
  text-decoration: none;
}

.brand-copy strong {
  font-size: 18px;
  letter-spacing: .04em;
}

.brand-copy span {
  margin-top: 5px;
  color: var(--color-brand);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: .16em;
}

nav {
  display: flex;
  gap: 9px;
  margin-right: auto;
  margin-left: 72px;
}

nav a {
  gap: 7px;
  padding: 9px 13px;
  border-radius: 12px;
  color: var(--color-text-secondary);
  font-size: 14px;
  text-decoration: none;
}

nav a.router-link-exact-active {
  background: #fff;
  color: var(--color-text-primary);
  font-weight: 700;
  box-shadow: 0 3px 10px rgb(31 33 33 / 4%);
}

.header-actions {
  gap: 14px;
}

.create-link {
  gap: 6px;
  padding: 11px 16px;
  border-radius: 14px;
  background: var(--color-brand);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  box-shadow: 0 7px 15px rgb(255 111 78 / 18%);
}

.profile-link {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid #f0dfd9;
  border-radius: 14px;
  background: #fff1ec;
  color: var(--color-brand);
  text-decoration: none;
}

@media (max-width: 720px) {
  .top-navigation {
    width: min(100% - 32px, 1240px);
  }

  nav {
    margin-left: 22px;
  }

  nav a {
    padding: 8px;
    font-size: 0;
  }

  .create-link {
    padding: 11px;
    font-size: 0;
  }
}
</style>
