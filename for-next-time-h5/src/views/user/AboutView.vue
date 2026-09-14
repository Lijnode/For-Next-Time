<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'

const router = useRouter()

/**
 * 返回个人页，继续查看账户设置。
 */
async function backToProfile() {
  // 1. 使用受保护的个人页作为关于页的固定返回目标。
  await router.push({ name: 'profile' })
}
</script>

<template>
  <main class="about-page">
    <header class="top-bar"><button type="button" aria-label="返回个人页" @click="backToProfile"><ArrowLeft :size="21" /></button><h1>关于下次一定</h1><span aria-hidden="true"></span></header>
    <section class="about-content">
      <div class="brand-mark" aria-hidden="true">次</div>
      <h2>下次一定</h2>
      <p class="english-name">FOR NEXT TIME</p>
<!--      <p class="slogan">这次先留着，下次一定用得上</p>-->
      <p class="intro">一个用于收藏、搜索和快速复用句子与内容素材的私人小工具</p>
      <p class="version">V1.0</p>
    </section>
  </main>
</template>

<style scoped>
.about-page { min-height: 100vh; padding: calc(12px + env(safe-area-inset-top)) 16px 32px; }.top-bar { display: grid; grid-template-columns: 42px 1fr 42px; align-items: center; }.top-bar button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--color-border); border-radius: 12px; background: #fff; color: var(--color-text-primary); }.top-bar h1 { margin: 0; color: var(--color-text-primary); font-size: 18px; text-align: center; }.about-content { padding: 72px 20px 32px; text-align: center; }.brand-mark { display: grid; width: 68px; height: 68px; margin: 0 auto 17px; place-items: center; border-radius: 23px; background: var(--color-brand); color: #fff; font-size: 27px; font-weight: 800; box-shadow: 0 9px 20px rgb(255 111 78 / 22%); }.about-content h2 { margin: 0; color: var(--color-text-primary); font-size: 24px; }.english-name { margin: 7px 0 30px; color: var(--color-brand); font-size: 11px; font-weight: 750; letter-spacing: .16em; }.slogan { margin: 0; color: var(--color-text-primary); font-size: 16px; font-weight: 600; }.intro { max-width: 250px; margin: 16px auto 0; color: var(--color-muted); font-size: 13px; line-height: 1.75; }.version { display: inline-block; margin: 30px 0 0; padding: 5px 10px; border-radius: 999px; background: #fff0eb; color: var(--color-brand); font-size: 12px; font-weight: 700; }
</style>
