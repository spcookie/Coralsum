package coralsum.service.impl

import coralsum.common.response.TurnstileVerifyResponse
import coralsum.config.TurnstileConfig
import coralsum.service.ITurnstileService
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class TurnstileServiceImpl(
    @Client("https://challenges.cloudflare.com") private val client: HttpClient,
    private val cfg: TurnstileConfig,
) : ITurnstileService {
    private val log = LoggerFactory.getLogger(TurnstileServiceImpl::class.java)

    override fun validate(token: String?, remoteIp: String?): Boolean {
        val secret = cfg.secret ?: ""
        if (secret.isBlank()) return true
        if (token.isNullOrBlank()) return false
        val body = mapOf(
            "secret" to secret,
            "response" to token,
            "remoteip" to (remoteIp ?: "")
        )
        val req = HttpRequest.POST("/turnstile/v0/siteverify", body)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
        return try {
            val resp = client.toBlocking().retrieve(req, TurnstileVerifyResponse::class.java)
            resp.success
        } catch (e: Exception) {
            log.warn("Turnstile verify failed", e)
            false
        }
    }
}

