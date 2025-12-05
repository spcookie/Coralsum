package coralsum.component.i18n

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpRequest
import io.micronaut.http.server.util.locale.HttpLocaleResolver
import jakarta.inject.Singleton
import java.util.*

@Singleton
class LangLocaleResolver : HttpLocaleResolver, Ordered {
    // 使用自定义策略解析语言（参数→Cookie→头），优先级最高
    override fun resolve(context: HttpRequest<*>): Optional<Locale> {
        val locale = LocaleUtils.resolveFromRequest(context)
        return Optional.of(locale)
    }

    override fun resolveOrDefault(context: @NonNull HttpRequest<*>?): @NonNull Locale {
        // 当上下文为空或无法解析时，返回英文作为默认语言
        return if (context == null) Locale.ENGLISH else resolve(context).orElse(Locale.ENGLISH)
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}
