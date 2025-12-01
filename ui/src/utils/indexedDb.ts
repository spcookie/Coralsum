const DB_NAME = 'coralsum-history'
const DB_VERSION = 4
const STORE_NAME = 'history'
const IMG_STORE_NAME = 'images'
const PREVIEW_STORE_NAME = 'preview_cache'

export type HistoryRecord = {
    id: string
    email: string
    createdAt: number
    inputTokens: number
    outputTokens: number
    durationMs: number
    imageCount: number
    images: string[]
    prompt?: string
    systemPrompt?: string
    config?: any
}

type AddHistoryPayload = {
    prompt: string
    systemPrompt?: string
    config?: any
    result: {
        images?: Array<Blob | string>
        inputTokens?: number
        outputTokens?: number
        durationMs?: number
        [key: string]: any
    }
}

function openDb(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        if (!('indexedDB' in window)) {
            reject(new Error('IndexedDB not supported'))
            return
        }
        const req = indexedDB.open(DB_NAME, DB_VERSION)
        req.onupgradeneeded = () => {
            const db = req.result
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                const store = db.createObjectStore(STORE_NAME, {keyPath: 'id'})
                store.createIndex('email', 'email', {unique: false})
                store.createIndex('createdAt', 'createdAt', {unique: false})
            }
            if (!db.objectStoreNames.contains(IMG_STORE_NAME)) {
                db.createObjectStore(IMG_STORE_NAME, {keyPath: 'id'})
            }
            if (!db.objectStoreNames.contains(PREVIEW_STORE_NAME)) {
                const store = db.createObjectStore(PREVIEW_STORE_NAME, {keyPath: 'key'})
                store.createIndex('createdAt', 'createdAt', {unique: false})
            }
        }
        req.onsuccess = () => resolve(req.result)
        req.onerror = () => reject(req.error || new Error('Failed to open DB'))
    })
}

function wrapRequest<T = any>(req: IDBRequest<T>): Promise<T> {
    return new Promise((resolve, reject) => {
        req.onsuccess = () => resolve(req.result)
        req.onerror = () => reject(req.error)
    })
}

function genId(): string {
    const g = (crypto as any)?.randomUUID?.()
    if (g) return g
    return `h_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

// 不进行网络请求：仅处理 Blob 和 data URL

async function toBlob(it: any): Promise<Blob | null> {
    try {
        if (it instanceof Blob) return it
        if (typeof it === 'string' && it) {
            if (it.startsWith('data:')) {
                const comma = it.indexOf(',')
                const header = it.slice(0, comma)
                const data = it.slice(comma + 1)
                const mime = header.match(/data:(.*);base64/)?.[1] || 'application/octet-stream'
                const bin = Uint8Array.from(atob(data), c => c.charCodeAt(0))
                return new Blob([bin], {type: mime})
            }
            return null
        }
        return null
    } catch {
        return null
    }
}

async function blobToBase64(b: Blob): Promise<{ base64: string; type: string; size: number }> {
    return new Promise((resolve, reject) => {
        const r = new FileReader()
        r.onload = () => {
            try {
                const s = String(r.result || '')
                const parts = s.split(',')
                const mime = parts[0].match(/data:(.*);base64/)?.[1] || b.type || 'application/octet-stream'
                const data = parts[1] || ''
                resolve({base64: data, type: mime, size: b.size})
            } catch (e) {
                reject(e)
            }
        }
        r.onerror = () => reject(r.error)
        r.readAsDataURL(b)
    })
}

export async function addHistoryFromResult(email: string, payload: AddHistoryPayload): Promise<void> {
    try {
        const srcImages = payload.result?.images || []
        const blobs: Blob[] = srcImages.filter((it): it is Blob => it instanceof Blob)
        const encodes: { id: string; base64: string; type: string; size: number }[] = []
        for (const b of blobs) {
            const id = `img_${genId()}`
            const enc = await blobToBase64(b)
            encodes.push({id, base64: enc.base64, type: enc.type, size: enc.size})
        }
        for (const it of srcImages) {
            if (typeof it === 'string' && it.startsWith('data:')) {
                try {
                    const comma = it.indexOf(',')
                    const header = it.slice(0, comma)
                    const data = it.slice(comma + 1)
                    const type = header.match(/data:(.*);base64/)?.[1] || 'application/octet-stream'
                    const id = `img_${genId()}`
                    encodes.push({id, base64: data, type, size: 0})
                } catch {
                }
            }
        }

        const db = await openDb()
        const tx = db.transaction([STORE_NAME, IMG_STORE_NAME], 'readwrite')
        const store = tx.objectStore(STORE_NAME)
        const imgStore = tx.objectStore(IMG_STORE_NAME)

        const imageIds: string[] = []
        for (const rec of encodes) {
            imageIds.push(rec.id)
            imgStore.put(rec)
        }

        const record = {
            id: genId(),
            email: email || '',
            createdAt: Date.now(),
            inputTokens: Number(payload.result?.inputTokens || 0),
            outputTokens: Number(payload.result?.outputTokens || 0),
            durationMs: Number(payload.result?.durationMs || 0),
            imageCount: imageIds.length > 0 ? imageIds.length : srcImages.filter((x) => !!x).length,
            imageIds,
            prompt: payload.prompt,
            systemPrompt: payload.systemPrompt,
            config: payload.config
        }

        store.add(record as any)
        await new Promise<void>((resolve, reject) => {
            tx.oncomplete = () => resolve()
            tx.onerror = () => reject(tx.error)
            tx.onabort = () => reject(tx.error)
        })
        db.close()
    } catch (e) {
        console.log('Failed to add history', e)
    }
}

export async function listHistory(email: string, limit = 50, offset = 0): Promise<HistoryRecord[]> {
    try {
        const db = await openDb()
        const tx = db.transaction([STORE_NAME, IMG_STORE_NAME], 'readonly')
        const store = tx.objectStore(STORE_NAME)
        const imgStore = tx.objectStore(IMG_STORE_NAME)
        const idx = store.index('email')
        const range = IDBKeyRange.only(email || '')
        let stored = await wrapRequest<any[]>(idx.getAll(range))
        if (!Array.isArray(stored)) stored = []
        stored.sort((a, b) => b.createdAt - a.createdAt)
        const page = stored.slice(offset, offset + limit)

        const out: HistoryRecord[] = []
        for (const r of page) {
            const urls: string[] = []
            if (Array.isArray(r.imageIds) && r.imageIds.length > 0) {
                for (const id of r.imageIds) {
                    const rec = await wrapRequest<any>(imgStore.get(id))
                    if (rec?.base64 && rec?.type) {
                        urls.push(`data:${rec.type};base64,${rec.base64}`)
                    } else if (rec?.blob instanceof Blob) {
                        try {
                            const enc = await blobToBase64(rec.blob)
                            urls.push(`data:${enc.type};base64,${enc.base64}`)
                        } catch {
                        }
                    }
                }
            }
            out.push({
                id: r.id,
                email: r.email,
                createdAt: r.createdAt,
                inputTokens: r.inputTokens,
                outputTokens: r.outputTokens,
                durationMs: r.durationMs,
                imageCount: r.imageCount,
                images: urls,
                prompt: r.prompt,
                systemPrompt: r.systemPrompt,
                config: r.config
            })
        }
        db.close()
        return out
    } catch {
        return []
    }
}

export async function countHistory(email: string): Promise<number> {
    try {
        const db = await openDb()
        const tx = db.transaction([STORE_NAME], 'readonly')
        const store = tx.objectStore(STORE_NAME)
        const idx = store.index('email')
        const range = IDBKeyRange.only(email || '')
        let stored = await wrapRequest<any[]>(idx.getAll(range))
        if (!Array.isArray(stored)) stored = []
        db.close()
        return stored.length
    } catch {
        return 0
    }
}

export async function deleteHistory(id: string): Promise<void> {
    const db = await openDb()
    const tx = db.transaction([STORE_NAME, IMG_STORE_NAME], 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const imgStore = tx.objectStore(IMG_STORE_NAME)
    const rec = await wrapRequest<any>(store.get(id))
    if (rec && Array.isArray(rec.imageIds)) {
        for (const imgId of rec.imageIds) {
            try {
                imgStore.delete(imgId)
            } catch {
            }
        }
    }
    try {
        store.delete(id)
    } catch {
    }
    await new Promise<void>((resolve, reject) => {
        tx.oncomplete = () => resolve()
        tx.onerror = () => reject(tx.error)
        tx.onabort = () => reject(tx.error)
    })
    db.close()
}

function normalizeKey(src: string): string {
    try {
        const u = new URL(src, window.location.origin)
        const ref = u.searchParams.get('ref')
        const pt = u.searchParams.get('pt')
        if (ref) return `ref:${ref}${pt ? `|pt:${pt}` : ''}`
        return u.toString()
    } catch {
        const idxQ = src.indexOf('ref=')
        const idxPt = src.indexOf('&pt=')
        const ref = idxQ >= 0 ? decodeURIComponent(src.slice(idxQ + 4)) : ''
        const pt = idxPt >= 0 ? decodeURIComponent(src.slice(idxPt + 4)) : ''
        if (ref) return `ref:${ref}${pt ? `|pt:${pt}` : ''}`
        return src
    }
}

function splitDataUrl(u: string): { type: string; base64: string } | null {
    if (typeof u !== 'string' || !u.startsWith('data:')) return null
    const comma = u.indexOf(',')
    if (comma < 0) return null
    const header = u.slice(0, comma)
    const data = u.slice(comma + 1)
    const type = header.match(/data:(.*);base64/)?.[1] || 'application/octet-stream'
    return {type, base64: data}
}

export async function savePreview(src: string, dataUrl: string): Promise<void> {
    const parts = splitDataUrl(dataUrl)
    if (!parts) return
    const key = normalizeKey(src)
    const db = await openDb()
    const tx = db.transaction([PREVIEW_STORE_NAME], 'readwrite')
    const store = tx.objectStore(PREVIEW_STORE_NAME)
    const size = Math.floor((parts.base64.length * 3) / 4)
    store.put({key, base64: parts.base64, type: parts.type, size, createdAt: Date.now()})
    await new Promise<void>((resolve, reject) => {
        tx.oncomplete = () => resolve()
        tx.onerror = () => reject(tx.error)
        tx.onabort = () => reject(tx.error)
    })
    db.close()
}

export async function getPreview(src: string): Promise<string | null> {
    const key = normalizeKey(src)
    const db = await openDb()
    const tx = db.transaction([PREVIEW_STORE_NAME], 'readonly')
    const store = tx.objectStore(PREVIEW_STORE_NAME)
    let rec: any = null
    try {
        rec = await wrapRequest<any>(store.get(key))
    } catch {
    }
    db.close()
    if (rec && rec.base64 && rec.type) return `data:${rec.type};base64,${rec.base64}`
    return null
}

export async function deletePreview(src: string): Promise<void> {
    const key = normalizeKey(src)
    const db = await openDb()
    const tx = db.transaction([PREVIEW_STORE_NAME], 'readwrite')
    const store = tx.objectStore(PREVIEW_STORE_NAME)
    try {
        store.delete(key)
    } catch {
    }
    await new Promise<void>((resolve, reject) => {
        tx.oncomplete = () => resolve()
        tx.onerror = () => reject(tx.error)
        tx.onabort = () => reject(tx.error)
    })
    db.close()
}

export async function evictPreview(maxEntries: number): Promise<void> {
    if (!Number.isFinite(maxEntries) || maxEntries <= 0) return
    const db = await openDb()
    const tx = db.transaction([PREVIEW_STORE_NAME], 'readwrite')
    const store = tx.objectStore(PREVIEW_STORE_NAME)
    let all: any[] = []
    try {
        all = await wrapRequest<any[]>(store.getAll())
    } catch {
        all = []
    }
    if (Array.isArray(all) && all.length > maxEntries) {
        all.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0))
        const toDelete = all.slice(0, all.length - maxEntries)
        for (const r of toDelete) {
            try {
                store.delete(r.key)
            } catch {
            }
        }
    }
    await new Promise<void>((resolve, reject) => {
        tx.oncomplete = () => resolve()
        tx.onerror = () => reject(tx.error)
        tx.onabort = () => reject(tx.error)
    })
    db.close()
}
