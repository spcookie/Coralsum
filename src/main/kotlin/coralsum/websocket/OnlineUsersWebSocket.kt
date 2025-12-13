package coralsum.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import coralsum.infrastructure.repository.OpenUserRepository
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnClose
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

@Singleton
@ServerWebSocket("/api/ws/online")
class OnlineUsersWebSocket(
    private val broadcaster: WebSocketBroadcaster,
    private val openUserRepository: OpenUserRepository,
) {

    private val mapper = ObjectMapper()
    private val sessionUidMap: MutableMap<WebSocketSession, String?> = ConcurrentHashMap()

    @OnOpen
    fun onOpen(session: WebSocketSession) {
        val uid = extractAndValidateUid(session)
        sessionUidMap[session] = uid
        broadcastCount()
    }

    @OnClose
    fun onClose(session: WebSocketSession) {
        sessionUidMap.remove(session)
        broadcastCount()
    }

    @OnMessage
    fun onMessage(message: String) {
        if (message.lowercase() == "ping") {
            return
        }
        broadcastCount()
    }

    private fun broadcastCount() {
        val uniqueUids = sessionUidMap.values.filterNotNull().toSet().size
        val payload = "{\"count\":$uniqueUids}"
        broadcaster.broadcastSync(payload) { _ -> true }
    }

    private fun extractAndValidateUid(session: WebSocketSession): String? {
        val token = extractToken(session) ?: return null
        val uid = parseUidFromJwt(token) ?: return null
        val exists = runBlocking { openUserRepository.findByUid(uid) != null }
        return if (exists) uid else null
    }

    private fun extractToken(session: WebSocketSession): String? {
        return try {
            val uri = session.requestURI
            val query = uri?.rawQuery ?: ""
            val params = query.split('&').mapNotNull {
                val p = it.trim()
                p.ifEmpty { null }
            }.associate {
                val idx = it.indexOf('=')
                if (idx < 0) it to "" else it.substring(0, idx) to URLDecoder.decode(
                    it.substring(idx + 1),
                    StandardCharsets.UTF_8
                )
            }
            val qToken = params["token"]?.trim()?.takeIf { it.isNotEmpty() }
            if (!qToken.isNullOrBlank()) qToken else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseUidFromJwt(token: String): String? {
        return try {
            val parts = token.split('.')
            if (parts.size != 3) return null
            val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            val node = mapper.readTree(payloadJson)
            val exp = node.path("exp").asLong(0)
            if (exp > 0 && Instant.ofEpochSecond(exp).isBefore(Instant.now())) return null
            node.path("sub").asText(null)
        } catch (_: Exception) {
            null
        }
    }
}
