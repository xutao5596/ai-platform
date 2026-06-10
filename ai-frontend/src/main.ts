import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import i18n from './locales'
import './styles/index.scss'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as any)
}

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus, { locale: i18n.global.locale.value === 'en-US' ? en : zhCn })
app.mount('#app')
