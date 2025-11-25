import type {PiniaPluginContext} from 'pinia'

export function persistPlugin({store}: PiniaPluginContext) {
    if (store.$id === 'history') {
        return
    }
    const key = `pinia_${store.$id}`
    const raw = localStorage.getItem(key)
    if (raw) {
        try {
            store.$patch(JSON.parse(raw))
        } catch {
        }
    }
    store.$subscribe((_mutation, state) => {
        localStorage.setItem(key, JSON.stringify(state))
    })
}