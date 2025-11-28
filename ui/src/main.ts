import {createApp} from 'vue'
import {createPinia} from 'pinia'
import {persistPlugin} from '@/stores/persist'
import {createRouter, createWebHistory} from 'vue-router'
import App from './App.vue'
import routes from './router'
import './index.css'
import '@fontsource/geist-mono/500.css'
import '@fontsource/geist-mono/600.css'
import '@fontsource/jetbrains-mono/400.css'
import '@fontsource/jetbrains-mono/500.css'
import '@fontsource/jetbrains-mono/600.css'
import {addCollection} from '@iconify/vue'
import solarCollection from '@iconify-json/solar/icons.json'
import mdiCollection from '@iconify-json/mdi/icons.json'
import materialSymbolsCollection from '@iconify-json/material-symbols/icons.json'
import phCollection from '@iconify-json/ph/icons.json'

if (import.meta.env.DEV) {
    import('./api/mockjs')
}

// 预先应用暗黑模式，避免刷新时闪烁
try {
    const raw = localStorage.getItem('pinia_settings')
    if (raw) {
        const saved = JSON.parse(raw)
        const dark = !!saved?.darkMode
        document.documentElement.classList.toggle('dark', dark)
    }
} catch {
}

// 注册本地图标集合，避免运行时网络加载
addCollection(solarCollection as any)
addCollection(mdiCollection as any)
addCollection(materialSymbolsCollection as any)
addCollection(phCollection as any)

const pinia = createPinia()
pinia.use(persistPlugin)

const router = createRouter({
    history: createWebHistory(),
    routes
})

const app = createApp(App)
app.use(pinia)
app.use(router)
app.mount('#app')
