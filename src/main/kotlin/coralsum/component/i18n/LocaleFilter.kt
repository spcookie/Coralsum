package coralsum.component.i18n

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import java.util.*

@Filter("/**")
class LocaleFilter : HttpServerFilter {
    // 解析请求的语言并设置响应的 Content-Language，供前后端参考
    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val locale: Locale = LocaleUtils.resolveFromRequest(request)
        val upstream = chain.proceed(request)
        return Publishers.map(upstream) { response ->
            response.headers.add("Content-Language", locale.toLanguageTag())
            response
        }
    }
}
