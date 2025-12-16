import {defineStore} from 'pinia'

export const useWsStore = defineStore('ws', {
    state: () => ({
        connected: false as boolean,
        count: 0 as number,
        ws: null as WebSocket | null,
        retryTimer: 0 as any,
        pingTimer: 0 as any,
        lastUrl: '' as string,
        reconnecting: false as boolean
    }),
    actions: {
        connect(token?: string) {
            const isDev = import.meta.env.DEV
            let url: string
            if (isDev) {
                url = `ws://localhost:8080/api/ws/online${token ? `?token=${encodeURIComponent(token)}` : ''}`
            } else {
                const proto = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws'
                url = `${proto}://${window.location.host}/api/ws/online${token ? `?token=${encodeURIComponent(token)}` : ''}`
            }
            this.lastUrl = url
            if (this.ws && (this.connected || this.ws.readyState === WebSocket.CONNECTING)) return
            try {
                this.ws = new WebSocket(url)
                this.ws.onopen = () => {
                    this.connected = true
                    this.reconnecting = false
                    if (this.pingTimer) clearInterval(this.pingTimer)
                    this.pingTimer = setInterval(() => {
                        try {
                            if (this.ws?.readyState === WebSocket.OPEN) {
                                this.ws?.send('ping')
                            }
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
                    this.ws = null
                    if (this.pingTimer) {
                        try {
                            clearInterval(this.pingTimer)
                            this.pingTimer = 0
                        } catch {
                        }
                    }
                    if (this.retryTimer) {
                        try {
                            clearTimeout(this.retryTimer)
                        } catch {
                        }
                    }
                    if (!this.reconnecting) {
                        this.reconnecting = true
                        this.retryTimer = setTimeout(() => {
                            this.reconnecting = false
                            this.connect(token)
                        }, 3000)
                    }
                }
            } catch {
            }
        },
        disconnect() {
            this.reconnecting = false
            if (this.retryTimer) {
                try {
                    clearTimeout(this.retryTimer)
                    this.retryTimer = 0
                } catch {
                }
            }
            if (this.pingTimer) {
                try {
                    clearInterval(this.pingTimer)
                    this.pingTimer = 0
                } catch {
                }
            }
            if (this.ws) {
                try {
                    this.ws.onclose = null
                    this.ws.onerror = null
                    this.ws.close()
                } catch {
                }
            }
            this.ws = null
            this.connected = false
        }
    }
})
