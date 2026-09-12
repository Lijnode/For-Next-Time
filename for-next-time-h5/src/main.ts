import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ActionSheet, Button, CellGroup, Field, Form, Tag } from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import './styles/base.css'

createApp(App)
  .use(createPinia())
  .use(router)
  .use(Button)
  .use(ActionSheet)
  .use(CellGroup)
  .use(Field)
  .use(Form)
  .use(Tag)
  .mount('#app')
