import {createApp} from 'vue'
import {createPinia} from 'pinia'
import {persistPlugin} from '@/stores/persist'
import {createRouter, createWebHistory} from 'vue-router'
import App from './App.vue'
import routes from './router'
import './index.css'

if (import.meta.env.DEV) {
    import('./api/mockjs')
}

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