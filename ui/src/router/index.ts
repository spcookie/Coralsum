import type {RouteRecordRaw} from 'vue-router'
import ImageGen from '@/pages/ImageGen.vue'
import Register from '@/pages/Register.vue'
import ForgotPassword from '@/pages/ForgotPassword.vue'
import PointsKeyAdmin from '@/pages/PointsKeyAdmin.vue'

const routes: RouteRecordRaw[] = [
    {path: '/', name: 'home', component: ImageGen},
    {path: '/register', name: 'register', component: Register},
    {path: '/forgot', name: 'forgot', component: ForgotPassword},
    {path: '/ctl/keys', name: 'ctl-keys', component: PointsKeyAdmin}
]

export default routes