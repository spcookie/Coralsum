package coralsum.component.i18n

import io.micronaut.http.HttpRequest
import io.micronaut.http.cookie.Cookie
import java.util.*

object LocaleUtils {
    // 将语言字符串转换为 Locale，支持 zh-CN、zh-TW、en
    fun parseLocale(code: String?): Locale {
        if (code.isNullOrBlank()) return Locale.ENGLISH
        val normalized = code.replace('_', '-').lowercase()
        return when (normalized) {
            "zh-cn" -> Locale.SIMPLIFIED_CHINESE
            "zh-tw", "zh-hant" -> Locale.TRADITIONAL_CHINESE
            "en", "en-us", "en-gb" -> Locale.ENGLISH
            else -> {
                // 尝试按照语言-地区拆分
                val parts = normalized.split('-')
                if (parts.size >= 2) Locale(parts[0], parts[1].uppercase()) else Locale(parts[0])
            }
        }
    }

    // 按策略从请求中解析 Locale（参数→Cookie→头→默认）
    fun resolveFromRequest(request: HttpRequest<*>): Locale {
        val paramLang = request.parameters["lang"]
        if (!paramLang.isNullOrBlank()) return parseLocale(paramLang)

        val cookieLang = request.cookies.findCookie("lang").map(Cookie::getValue).orElse(null)
        if (!cookieLang.isNullOrBlank()) return parseLocale(cookieLang)

        val headerLang = request.headers.get("Accept-Language")
        if (!headerLang.isNullOrBlank()) {
            val first = headerLang.split(',').firstOrNull()?.trim()
            if (!first.isNullOrBlank()) return parseLocale(first)
        }

        return Locale.ENGLISH
    }
}

