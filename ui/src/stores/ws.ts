import {defineStore} from 'pinia'

export const useWsStore = defineStore('ws', {
    state: () => ({
        connected: false as boolean,
        count: 0 as number,
        ws: null as WebSocket | null,
        retryTimer: 0 as any,
        pingTimer: 0 as any,
        lastUrl: '' as string
    }),
    actions: {
        connect(token?: string) {
            const proto = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws'
            const url = `${proto}://${window.location.host}/api/ws/online${token ? `?token=${encodeURIComponent(token)}` : ''}`
            this.lastUrl = url
            if (this.ws && (this.connected || this.ws.readyState === WebSocket.CONNECTING)) return
            try {
                this.ws = new WebSocket(url)
                this.ws.onopen = () => {
                    this.connected = true
                    if (this.pingTimer) clearInterval(this.pingTimer)
                    this.pingTimer = setInterval(() => {
                        try {
                            this.ws?.send('ping')
                        } catch {
                        }
                    }, 30000)
                }
                this.ws.onmessage = (ev) => {
                    try {
                        const data = JSON.parse(String(ev.data || '{}'))
                        if (typeof data.count === 'number') this.count = data.count
                    } catch {
                        const n = Number(ev.data)
                        if (!Number.isNaN(n)) this.count = n
                    }
                }
                this.ws.onerror = () => {
                    this.connected = false
                }
                this.ws.onclose = () => {
                    this.connected = false
                    if (this.pingTimer) {
                        try {
                            clearInterval(this.pingTimer)
                        } catch {
                        }
                    }
                    if (this.retryTimer) {
                        try {
                            clearTimeout(this.retryTimer)
                        } catch {
                        }
                    }
                    this.retryTimer = setTimeout(() => {
                        this.connect(token)
                    }, 2000)
                }
            } catch {
            }
        },
        disconnect() {
            try {
                this.ws?.close()
            } catch {
            }
            this.ws = null
            this.connected = false
            if (this.pingTimer) {
                try {
                    clearInterval(this.pingTimer)
                } catch {
                }
            }
            if (this.retryTimer) {
                try {
                    clearTimeout(this.retryTimer)
                } catch {
                }
            }
        }
    }
})

