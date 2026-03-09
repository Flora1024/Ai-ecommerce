import { createApp } from 'vue'
import App from './App.vue'
import './assets/main.css'
import router from './router'
// 注册 SVG Icon
import 'virtual:svg-icons-register'
// 引入 Pinia
import { createPinia } from 'pinia'
import piniaPluginPersistedstated from 'pinia-plugin-persistedstate'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstated)

// 应用pinia
app.use(pinia)

// 应用路由
app.use(router)
app.mount('#app')
