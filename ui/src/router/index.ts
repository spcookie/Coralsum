import type {RouteRecordRaw} from 'vue-router'

const routes: RouteRecordRaw[] = [
    {path: '/', name: 'home', component: () => import('@/pages/ImageGen.vue')},
    {path: '/register', name: 'register', component: () => import('@/pages/Register.vue')},
    {path: '/forgot', name: 'forgot', component: () => import('@/pages/ForgotPassword.vue')},
    {path: '/ctl/keys', name: 'ctl-keys', component: () => import('@/pages/PointsKeyAdmin.vue')},
    {path: '/tools', name: 'tools', component: () => import('@/pages/Tools.vue')},
    {path: '/ideas', name: 'ideas-admin', component: () => import('@/pages/IdeaAdmin.vue')}
]

export default routes
