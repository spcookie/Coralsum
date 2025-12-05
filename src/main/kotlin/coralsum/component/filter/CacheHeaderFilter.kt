package coralsum.component.filter

import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

@Singleton
@Filter("/**")
class CacheHeaderFilter : HttpServerFilter {
    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val path = request.path
        val isStaticAsset = path.startsWith("/assets/")
        val isFavicon = path == "/favicon.svg" || path == "/favicon.ico"
        val isIndex = path == "/" || path.endsWith("/index.html") || path == "/index.html"
        return Flux.from(chain.proceed(request)).doOnNext { response ->
            if (request.method == HttpMethod.GET) {
                when {
                    isStaticAsset -> {
                        response.headers.add("Cache-Control", "public, max-age=31536000, immutable")
                        response.headers.add("Vary", "Accept-Encoding")
                    }

                    isFavicon -> {
                        response.headers.add("Cache-Control", "public, max-age=2592000")
                    }

                    isIndex -> {
                        response.headers.add("Cache-Control", "public, max-age=0, must-revalidate")
                    }
                }
            }
        }
    }
}