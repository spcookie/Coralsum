import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    base: './',
    build: {
        chunkSizeWarningLimit: 1200,
        rollupOptions: {
            output: {
                entryFileNames: 'assets/[name]-[hash].js',
                chunkFileNames: 'assets/[name]-[hash].js',
                assetFileNames: 'assets/[name]-[hash][extname]',
                manualChunks(id) {
                    if (!id.includes('node_modules')) return undefined
                    if (id.includes('/naive-ui/')) return 'naive-ui'
                    if (id.includes('/@iconify/vue/')) return 'iconify'
                    if (id.includes('/vue/')) return 'vue'
                    if (id.includes('/vue-router/')) return 'vue'
                    if (id.includes('/pinia/')) return 'vue'
                    const parts = id.split('node_modules/')[1].split('/')
                    if (parts[0]?.startsWith('@')) return parts.slice(0, 2).join('/')
                    return parts[0]
                }
            }
        }
    },
    server: {
        port: 5173,
        host: true,
        allowedHosts: ['localhost', 'coralsum.spcookie.top'],
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
    resolve: {
        alias: {
            '@': '/src'
        }
    }
})
