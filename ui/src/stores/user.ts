import {defineStore} from 'pinia'

export type UserTier = 'FREE' | 'PLUS' | 'PRO'

function simpleFingerprint() {
    const nav = navigator
    const data = [nav.userAgent, nav.language, nav.platform, screen.width, screen.height].join('|')
    let hash = 0
    for (let i = 0; i < data.length; i++) hash = (hash << 5) - hash + data.charCodeAt(i)
    return String(hash >>> 0)
}

export const useUserStore = defineStore('user', {
    state: () => ({
        name: '',
        points: 0,
        tier: 'FREE' as UserTier,
        email: '',
        token: '',
        profileReady: false,
        showLoginModal: false,
        showProfileModal: false
    }),
    actions: {
        init() {
        },
        setName(n: string) {
            this.name = n
            this.profileReady = true
        },
        setTier(t: UserTier) {
            this.tier = t
        },
        addPoints(p: number) {
            this.points += p
        },
        setProfile(payload: { email: string; name: string; points: number; tier: UserTier; token?: string }) {
            this.email = payload.email
            this.name = payload.name
            this.points = payload.points
            this.tier = payload.tier
            if (payload.token && payload.token.length > 0) {
                this.token = payload.token
            }
            this.profileReady = true
            this.showLoginModal = false
            this.showProfileModal = false
        },
        requireLogin() {
            this.showLoginModal = true
        },
        requireProfile() {
            this.showProfileModal = true
        }
    }
})