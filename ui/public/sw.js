const CACHE_NAME = 'coralsum-pwa-v1'

self.addEventListener('install', (event) => {
    self.skipWaiting()
})

self.addEventListener('activate', (event) => {
    event.waitUntil((async () => {
        const keys = await caches.keys()
        await Promise.all(keys.map(k => (k !== CACHE_NAME) ? caches.delete(k) : Promise.resolve()))
        await self.clients.claim()
    })())
})

self.addEventListener('fetch', (event) => {
    const req = event.request
    if (req.method !== 'GET') return

    const url = new URL(req.url)

    if (event.request.mode === 'navigate') {
        event.respondWith((async () => {
            try {
                const netRes = await fetch(req)
                const cache = await caches.open(CACHE_NAME)
                // 缓存最新的入口页面
                if (netRes && netRes.status === 200 && (netRes.headers.get('content-type') || '').includes('text/html')) {
                    cache.put('/index.html', netRes.clone())
                }
                return netRes
            } catch (e) {
                const cache = await caches.open(CACHE_NAME)
                const cached = await cache.match('/index.html')
                if (cached) return cached
                return new Response('离线不可用', {status: 503, statusText: 'Service Unavailable'})
            }
        })())
        return
    }

    if (url.origin === self.location.origin) {
        event.respondWith((async () => {
            const cache = await caches.open(CACHE_NAME)
            const cached = await cache.match(req)
            const fetchPromise = fetch(req).then(res => {
                if (res && res.status === 200) {
                    cache.put(req, res.clone())
                }
                return res
            }).catch(() => cached)
            return cached || fetchPromise
        })())
    }
})
