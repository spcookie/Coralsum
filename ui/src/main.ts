import {createApp} from 'vue'
import {createPinia} from 'pinia'
import {persistPlugin} from '@/stores/persist'
import {createRouter, createWebHashHistory} from 'vue-router'
import App from './App.vue'
import routes from './router'
import './index.css'
import '@fontsource/geist-mono/500.css'
import '@fontsource/geist-mono/600.css'
import '@fontsource/jetbrains-mono/400.css'
import '@fontsource/jetbrains-mono/500.css'
import '@fontsource/jetbrains-mono/600.css'
import {addIcon} from '@iconify/vue'
import mdiAccount from '@iconify-icons/mdi/account'
import mdiAccountCog from '@iconify-icons/mdi/account-cog'
import mdiAccountArrowRight from '@iconify-icons/mdi/account-arrow-right'
import mdiLogout from '@iconify-icons/mdi/logout'
import mdiKey from '@iconify-icons/mdi/key'
import mdiHistory from '@iconify-icons/mdi/history'
import mdiWeatherNight from '@iconify-icons/mdi/weather-night'
import mdiWhiteBalanceSunny from '@iconify-icons/mdi/white-balance-sunny'
import mdiRefresh from '@iconify-icons/mdi/refresh'
import mdiChevronUp from '@iconify-icons/mdi/chevron-up'
import mdiChevronDown from '@iconify-icons/mdi/chevron-down'
import mdiDeleteOutline from '@iconify-icons/mdi/delete-outline'
import mdiEyeOffOutline from '@iconify-icons/mdi/eye-off-outline'
import mdiEyeOutline from '@iconify-icons/mdi/eye-outline'
import mdiCloudUpload from '@iconify-icons/mdi/cloud-upload'
import mdiReload from '@iconify-icons/mdi/reload'
import mdiClose from '@iconify-icons/mdi/close'
import mdiMagnifyMinusOutline from '@iconify-icons/mdi/magnify-minus-outline'
import mdiMagnifyPlusOutline from '@iconify-icons/mdi/magnify-plus-outline'
import mdiRestart from '@iconify-icons/mdi/restart'
import mdiRotateLeft from '@iconify-icons/mdi/rotate-left'
import mdiRotateRight from '@iconify-icons/mdi/rotate-right'
import mdiContentCopy from '@iconify-icons/mdi/content-copy'
import mdiDownload from '@iconify-icons/mdi/download'
import mdiImage from '@iconify-icons/mdi/image'
import mdiKeyChain from '@iconify-icons/mdi/key-chain'
import msBoltRounded from '@iconify-icons/material-symbols/bolt-rounded'
import phWarning from '@iconify-icons/ph/warning'
import phCode from '@iconify-icons/ph/code'
import phSparkle from '@iconify-icons/ph/sparkle'
import phTimer from '@iconify-icons/ph/timer'
import phImage from '@iconify-icons/ph/image'
import phShare from '@iconify-icons/ph/share'
import phInfo from '@iconify-icons/ph/info'
import solarStarsBoldDuotone from '@iconify-icons/solar/stars-bold-duotone'

if (import.meta.env.DEV) {
    import('./api/mockjs')
}

// 预先应用暗黑模式，避免刷新时闪烁
try {
    const raw = localStorage.getItem('pinia_settings')
    if (raw) {
        const saved = JSON.parse(raw || '{}')
        const dark = !!saved?.darkMode
        document.documentElement.classList.toggle('dark', dark)
    }
} catch {
}

addIcon('mdi:account', mdiAccount)
addIcon('mdi:account-cog', mdiAccountCog)
addIcon('mdi:account-arrow-right', mdiAccountArrowRight)
addIcon('mdi:logout', mdiLogout)
addIcon('mdi:key', mdiKey)
addIcon('mdi:history', mdiHistory)
addIcon('mdi:weather-night', mdiWeatherNight)
addIcon('mdi:white-balance-sunny', mdiWhiteBalanceSunny)
addIcon('mdi:refresh', mdiRefresh)
addIcon('mdi:chevron-up', mdiChevronUp)
addIcon('mdi:chevron-down', mdiChevronDown)
addIcon('mdi:delete-outline', mdiDeleteOutline)
addIcon('mdi:eye-off-outline', mdiEyeOffOutline)
addIcon('mdi:eye-outline', mdiEyeOutline)
addIcon('mdi:cloud-upload', mdiCloudUpload)
addIcon('mdi:reload', mdiReload)
addIcon('mdi:close', mdiClose)
addIcon('mdi:magnify-minus-outline', mdiMagnifyMinusOutline)
addIcon('mdi:magnify-plus-outline', mdiMagnifyPlusOutline)
addIcon('mdi:restart', mdiRestart)
addIcon('mdi:rotate-left', mdiRotateLeft)
addIcon('mdi:rotate-right', mdiRotateRight)
addIcon('mdi:content-copy', mdiContentCopy)
addIcon('mdi:download', mdiDownload)
addIcon('mdi:image', mdiImage)
addIcon('mdi:key-chain', mdiKeyChain)
addIcon('material-symbols:bolt-rounded', msBoltRounded)
addIcon('ph:warning', phWarning)
addIcon('ph:code', phCode)
addIcon('ph:sparkle', phSparkle)
addIcon('ph:timer', phTimer)
addIcon('ph:image', phImage)
addIcon('ph:share', phShare)
addIcon('ph:info', phInfo)
addIcon('solar:stars-bold-duotone', solarStarsBoldDuotone)

const pinia = createPinia()
pinia.use(persistPlugin)

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

const app = createApp(App)
app.use(pinia)
app.use(router)
app.mount('#app')
