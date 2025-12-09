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
        uid: '',
        permissions: [] as string[],
        name: '',
        nickTag: 0 as number | null,
        points: 0,
        permanentPoints: 0,
        subscribePoints: 0,
        giftPoints: 0,
        subscribeExpireTime: null as number | null,
        giftExpireTime: null as number | null,
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
        setProfile(payload: {
            uid?: string;
            permissions?: string[];
            email: string;
            name: string;
            nickTag?: number | null;
            points?: number;
            permanentPoints?: number;
            subscribePoints?: number;
            giftPoints?: number;
            subscribeExpireTime?: number | null;
            giftExpireTime?: number | null;
            tier: UserTier;
            token?: string
        }) {
            if (payload.uid) this.uid = payload.uid
            if (Array.isArray(payload.permissions)) this.permissions = payload.permissions
            this.email = payload.email
            this.name = payload.name
            if (typeof payload.nickTag === 'number' || payload.nickTag === null) this.nickTag = payload.nickTag ?? null
            if (typeof payload.points === 'number') this.points = payload.points
            this.permanentPoints = payload.permanentPoints ?? this.permanentPoints
            this.subscribePoints = payload.subscribePoints ?? this.subscribePoints
            this.giftPoints = payload.giftPoints ?? this.giftPoints
            this.subscribeExpireTime = typeof payload.subscribeExpireTime === 'number' ? payload.subscribeExpireTime : (payload.subscribeExpireTime ?? this.subscribeExpireTime)
            this.giftExpireTime = typeof payload.giftExpireTime === 'number' ? payload.giftExpireTime : (payload.giftExpireTime ?? this.giftExpireTime)
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
        ,
        logout() {
            try {
                try {
                    localStorage.removeItem('pinia_user')
                } catch {
                }
                try {
                    sessionStorage.clear()
                } catch {
                }
            } catch {
            }
            this.$reset()
            this.showLoginModal = true
        }
    }
})
