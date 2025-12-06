package coralsum.component.filter

import coralsum.common.dto.Res
import coralsum.component.turnstile.TurnstileProtectedRegistry
import coralsum.service.ITurnstileService
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import jakarta.inject.Singleton
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@Singleton
@Filter("/**")
class TurnstileFilter(
    private val turnstileService: ITurnstileService,
    private val registry: TurnstileProtectedRegistry,
    private val lms: LocalizedMessageSource,
) : HttpServerFilter {
    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val needVerify = registry.isProtectedPath(request.path)
        if (!needVerify) {
            return chain.proceed(request)
        }

        val token = request.headers.get("CF-Turnstile-Response") ?: request.headers.get("X-Turnstile-Token")
        val remoteIp = request.headers.get("CF-Connecting-IP") ?: request.headers.get("X-Forwarded-For")
        return Mono.deferContextual { ctx ->
            mono(ctx.asCoroutineContext()) {
                turnstileService.validate(token, remoteIp)
            }
        }.flatMapMany { passed ->
            if (!passed) {
                val msg = lms.getMessage("auth.turnstile.invalid").orElse("Invalid verification")
                val resp: MutableHttpResponse<Res<Any>> = HttpResponse.status<Res<Any>>(HttpStatus.UNAUTHORIZED)
                    .body(Res.fail(msg))
                Publishers.just(resp)
            } else {
                chain.proceed(request)
            }
        }
    }
}
