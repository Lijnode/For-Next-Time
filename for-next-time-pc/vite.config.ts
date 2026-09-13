import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 1. 本地开发时将 API 请求转发至 Spring Boot 服务。
      '/api': {
        target: 'http://127.0.0.1:6688',
        changeOrigin: true,
      },
      // 2. 本地开发时代理头像静态文件，保持与生产环境一致的同源地址。
      '/files': {
        target: 'http://127.0.0.1:6688',
        changeOrigin: true,
      },
    },
  },
})
