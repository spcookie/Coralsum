import {defineStore} from 'pinia'

export interface HistoryItem {
    id: string
    createdAt: number
    inputTokens: number
    outputTokens: number
    durationMs: number
    images: string[]
    text?: string
    expanded?: boolean
    loading?: boolean
    error?: string
}

export const useHistoryStore = defineStore('history', {
    state: () => ({items: [] as HistoryItem[]}),
    actions: {
        setItems(items: HistoryItem[]) {
            this.items = items
        },
        add(item: HistoryItem) {
            this.items.unshift(item)
        },
        toggle(id: string) {
            const it = this.items.find(i => i.id === id)
            if (it) it.expanded = !it.expanded
        },
        setLoading(id: string, v: boolean) {
            const it = this.items.find(i => i.id === id)
            if (it) it.loading = v
        },
        setError(id: string, msg?: string) {
            const it = this.items.find(i => i.id === id)
            if (it) it.error = msg || ''
        }
    }
})