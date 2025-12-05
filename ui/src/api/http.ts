import axios from 'axios'
import {useUserStore} from '@/stores/user'

const http = axios.create({baseURL: '/api', timeout: 1000 * 60 * 4})

http.interceptors.request.use((config) => {
    const user = useUserStore()
    if (user?.token) {
        config.headers = config.headers || {}
        ;(config.headers as any).Authorization = `Bearer ${user.token}`
    }
    // 附加 Accept-Language 头，来源于本地存储或浏览器首选
    try {
        const lang = localStorage.getItem('lang') || navigator.language
        config.headers = config.headers || {}
        ;(config.headers as any)['Accept-Language'] = (lang || 'en').replace('_', '-')
    } catch {
    }
    return config
})

http.interceptors.response.use(
    (res) => {
        const {status, data} = res
        if (status < 200 || status >= 300) {
            return Promise.reject(res)
        }
        // 统一响应：Res<T>，code 为枚举名称（如 SUCCESS、FAIL 等）
        if (data && typeof data === 'object' && 'code' in (data as any)) {
            const code = (data as any).code as string
            const message = (data as any).message as string
            const payload = (data as any).data
            if (code !== 'SUCCESS') {
                if (code === 'UNAUTHORIZED' || code === 'FORBIDDEN') {
                    const user = useUserStore()
                    const hadAuthHeader = !!((res as any)?.config?.headers)?.Authorization
                    const hadToken = !!user?.token
                    const authExpired = hadAuthHeader || hadToken
                    if (authExpired) {
                        ;(res as any).__authExpired = true
                    }
                    user.token = ''
                    user.requireLogin()
                    return Promise.reject({code, message, data: payload, __authExpired: true})
                }
                return Promise.reject({code, message, data: payload})
            }
            ;(res as any).data = payload
        }
        return res
    },
    (err) => {
        const user = useUserStore()
        const status = err?.response?.status
        const hadAuthHeader = !!(err?.config?.headers)?.Authorization
        const hadToken = !!user?.token
        const authExpired = hadAuthHeader || hadToken
        if (status === 401 || status === 403) {
            if (authExpired) {
                ;(err as any).__authExpired = true
            }
            user.token = ''
            user.requireLogin()
        }
        return Promise.reject(err)
    }
)

export default http
