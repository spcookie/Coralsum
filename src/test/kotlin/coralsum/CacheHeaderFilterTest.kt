package coralsum

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class CacheHeaderFilterTest {

    @Inject
    @field:io.micronaut.http.client.annotation.Client("/")
    lateinit var client: HttpClient

    @Test
    fun `assets have long cache headers`() {
        val resp = client.toBlocking().exchange(HttpRequest.GET<Any>("/assets/index-BM9EarTB.js"), String::class.java)
        val cache = resp.headers.get("Cache-Control") ?: ""
        assertTrue(cache.contains("max-age=31536000") && cache.contains("immutable"))
    }

    @Test
    fun `index html is not long cached`() {
        val resp = client.toBlocking().exchange(HttpRequest.GET<Any>("/"), String::class.java)
        val cache = resp.headers.get("Cache-Control") ?: ""
        assertTrue(cache.contains("max-age=0") || cache.contains("no-cache"))
    }

    @Test
    fun `favicon has cache`() {
        val resp = client.toBlocking().exchange(HttpRequest.GET<Any>("/favicon.svg"), String::class.java)
        val cache = resp.headers.get("Cache-Control") ?: ""
        assertTrue(cache.contains("max-age") && !cache.contains("no-store"))
    }
}
