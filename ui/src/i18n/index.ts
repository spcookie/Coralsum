import {createI18n} from 'vue-i18n'

// 按需加载语言包，默认英文为兜底
export type SupportedLocale = 'en' | 'zh-CN' | 'zh-TW'

async function loadLocaleMessages(locale: SupportedLocale) {
    switch (locale) {
        case 'zh-CN':
            return (await import('../locales/zh-CN.json')).default
        case 'zh-TW':
            return (await import('../locales/zh-TW.json')).default
        default:
            return (await import('../locales/en.json')).default
    }
}

function detectInitialLocale(): SupportedLocale {
    const hash = String(location.hash || '')
    const hashPath = hash.startsWith('#/') ? hash.slice(2).split('?')[0] : ''
    const hashSeg = hashPath ? hashPath.split('/')[0] : ''
    const param = new URLSearchParams(hash.split('?')[1] || '').get('lang')
    const cookieLang = document.cookie.split('; ').find(s => s.startsWith('lang='))?.split('=')[1]
    const saved = localStorage.getItem('lang')
    const browser = navigator.language
    const candidate = param || hashSeg || cookieLang || saved || browser
    const normalized = (candidate || 'en').replace('_', '-').toLowerCase()
    if (normalized === 'zh-cn' || normalized.startsWith('zh-cn') || normalized.startsWith('zh-hans') || normalized === 'zh') return 'zh-CN'
    if (normalized === 'zh-tw' || normalized.startsWith('zh-tw') || normalized.startsWith('zh-hant')) return 'zh-TW'
    if (normalized === 'en' || normalized.startsWith('en')) return 'en'
    return 'en'
}

export async function createI18nInstance() {
    const locale = detectInitialLocale()
    const messages = await loadLocaleMessages(locale)
    return createI18n({
        legacy: false,
        locale,
        fallbackLocale: 'en',
        messages: {
            [locale]: messages
        }
    })
}
