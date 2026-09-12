import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { Button } from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import './styles/base.css'

createApp(App)
  .use(createPinia())
  .use(router)
  .use(Button)
  .mount('#app')
