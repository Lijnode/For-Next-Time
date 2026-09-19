import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ActionSheet, Button, CellGroup, Empty, Field, Form, List, Search, Tag, Uploader } from 'vant'
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
  .use(Empty)
  .use(Field)
  .use(Form)
  .use(List)
  .use(Search)
  .use(Tag)
  .use(Uploader)
  .mount('#app')
