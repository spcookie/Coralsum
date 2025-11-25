import axios from 'axios'
import {useUserStore} from '@/stores/user'

const http = axios.create({baseURL: '/api', timeout: 1000 * 60 * 4})

http.interceptors.request.use((config) => {
    const user = useUserStore()
    if (user?.token) {
        config.headers = config.headers || {}
        ;(config.headers as any).Authorization = `Bearer ${user.token}`
    }
    return config
})

http.interceptors.response.use(
    (res) => {
        const {status, data} = res
        if (status < 200 || status >= 300) {
            return Promise.reject(res)
        }
        if (data && typeof data === 'object') {
            const maybeStatus = (data as any).status
            if (typeof maybeStatus === 'number' && maybeStatus >= 400) {
                const message = (data as any).message
                const user = useUserStore()
                if (maybeStatus === 401 || maybeStatus === 403) {
                    user.token = ''
                    user.requireLogin()
                }
                return Promise.reject({status: maybeStatus, message})
            }
        }
        if (data && typeof data === 'object' && 'code' in (data as any)) {
            const code = (data as any).code as string
            const message = (data as any).message as string
            const payload = (data as any).data
            if (code !== 'SUCCESS') {
                const user = useUserStore()
                if (code === 'UNAUTHORIZED' || code === 'FORBIDDEN') {
                    user.token = ''
                    user.requireLogin()
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
        if (status === 401 || status === 403) {
            user.token = ''
            user.requireLogin()
        }
        return Promise.reject(err)
    }
)

export default http