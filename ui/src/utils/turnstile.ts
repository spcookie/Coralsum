type WidgetConfig = {
    sitekey: string
    theme?: string
    size?: string
    onSuccess?: (token: string, widgetId: string) => void
    onError?: (err: any, widgetId: string) => void
    waitMs?: number
    language?: string
}

class TurnstileManager {
    private widgets = new Map<string, string>()

    createWidget(container: string | HTMLElement, config: WidgetConfig): Promise<string> {
        return new Promise((resolve) => {
            const tryRender = () => {
                const ts = (window as any).turnstile
                const el = typeof container === 'string' ? document.querySelector(container) as HTMLElement : container as HTMLElement
                if (!ts || !el) {
                    const t = setTimeout(tryRender, 120)
                    const deadline = (config.waitMs ?? 5000)
                    ;(this as any)._timeouts = (this as any)._timeouts || new Map()
                    const k = this.key(container)
                    const first = !(this as any)._timeouts.has(k)
                    if (first) (this as any)._timeouts.set(k, {start: Date.now(), h: t})
                    else {
                        const rec = (this as any)._timeouts.get(k)
                        if (Date.now() - rec.start >= deadline) {
                            clearTimeout(rec.h)
                            ;(this as any)._timeouts.delete(k)
                            config.onError && config.onError('timeout', '')
                            resolve('')
                            return
                        }
                    }
                    return
                }
                const id = ts.render(el, {
                    sitekey: config.sitekey,
                    theme: config.theme || 'auto',
                    size: config.size || 'normal',
                    language: config.language,
                    callback: (token: string) => {
                        config.onSuccess && config.onSuccess(token, id)
                    },
                    'error-callback': (e: any) => {
                        config.onError && config.onError(e, id)
                    }
                })
                this.widgets.set(this.key(container), id)
                resolve(id)
            }
            const ready = (window as any).turnstile?.ready
            if (ready) ready(tryRender)
            else tryRender()
        })
    }

    removeWidget(container: string | HTMLElement) {
        const k = this.key(container)
        if (!k) return
        const id = this.widgets.get(k)
        if (id) {
            (window as any).turnstile?.remove(id)
            this.widgets.delete(k)
        }
    }

    resetWidget(container: string | HTMLElement) {
        const k = this.key(container)
        if (!k) return
        const id = this.widgets.get(k)
        if (id) (window as any).turnstile?.reset(id)
    }

    private key(c: string | HTMLElement) {
        if (!c) return ''
        if (typeof c === 'string') return c
        const el: any = c as any
        if (!el.__tid) el.__tid = `ts_${Math.random().toString(36).slice(2)}`
        return el.__tid
    }
}

export const turnstileManager = new TurnstileManager()

export function markTurnstileVerified(token?: string) {
    try {
        sessionStorage.setItem('turnstile_last_verified_at', String(Date.now()))
        if (token) sessionStorage.setItem('turnstile_session_token', token)
        sessionStorage.setItem('turnstile_session_token_used', 'false')
    } catch {
    }
}

export function isTurnstileRecentlyVerified(windowMs: number = 270000) {
    try {
        const nav = (performance?.getEntriesByType?.('navigation') as any) || []
        const isReload = nav[0]?.type === 'reload'
        if (isReload) return false
        const raw = sessionStorage.getItem('turnstile_last_verified_at')
        if (!raw) return false
        const ts = Number(raw)
        if (!Number.isFinite(ts)) return false
        return Date.now() - ts < windowMs
    } catch {
        return false
    }
}

export function getSessionTokenIfValid(windowMs: number = 300000) {
    try {
        const used = sessionStorage.getItem('turnstile_session_token_used') === 'true'
        if (used) return ''
        const token = sessionStorage.getItem('turnstile_session_token') || ''
        const tsRaw = sessionStorage.getItem('turnstile_last_verified_at') || ''
        const ts = Number(tsRaw)
        if (!token || !Number.isFinite(ts)) return ''
        if (Date.now() - ts >= windowMs) return ''
        return token
    } catch {
        return ''
    }
}

export function markSessionTokenUsed() {
    try {
        sessionStorage.setItem('turnstile_session_token_used', 'true')
    } catch {
    }
}

export function shouldShowTurnstile(windowMs: number = 300000) {
    return !getSessionTokenIfValid(windowMs)
}
