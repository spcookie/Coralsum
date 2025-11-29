import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5173,
        host: true,
        allowedHosts: ['localhost', 'coralsum.spcookie.top'],
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                headers: {
                    Origin: 'http://localhost:8080'
                }
            }
        }
    },
    resolve: {
        alias: {
            '@': '/src'
        }
    }
})
