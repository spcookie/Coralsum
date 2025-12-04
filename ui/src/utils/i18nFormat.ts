import {useI18n} from 'vue-i18n'

// 提供日期与数字格式化工具，使用 Intl 与当前语言
export function useI18nFormat() {
    const {locale} = useI18n()

    function formatDate(d: Date | number, options?: Intl.DateTimeFormatOptions) {
        const dt = typeof d === 'number' ? new Date(d) : d
        return new Intl.DateTimeFormat(locale.value, options || {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        }).format(dt)
    }

    function formatNumber(n: number, options?: Intl.NumberFormatOptions) {
        return new Intl.NumberFormat(locale.value, options || {maximumFractionDigits: 2}).format(n)
    }

    return {formatDate, formatNumber}
}

