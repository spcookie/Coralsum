import type {RouteRecordRaw} from 'vue-router'
import ImageGen from '@/pages/ImageGen.vue'
import Register from '@/pages/Register.vue'

const routes: RouteRecordRaw[] = [
    {path: '/', name: 'home', component: ImageGen},
    {path: '/register', name: 'register', component: Register}
]

export default routes