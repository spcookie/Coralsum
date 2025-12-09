package coralsum.infrastructure.cache

import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.serde.annotation.Serdeable
import jakarta.inject.Singleton
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serdeable
data class UploadedImageRef(val uri: String, val mimeType: String, val size: Int = 0) : Serializable

@Singleton
class UploadedImageCache {

    private val locks = ConcurrentHashMap<String, Any>()

    @Cacheable(cacheNames = ["uploaded-image-urls"], parameters = ["uid", "sid"])
    fun list(uid: String, sid: String): List<UploadedImageRef>? = null

    @CachePut(cacheNames = ["uploaded-image-urls"], parameters = ["uid", "sid"], condition = "#{refs != null}")
    fun put(uid: String, sid: String, refs: List<UploadedImageRef>): List<UploadedImageRef> = refs

    fun append(uid: String, sid: String, ref: UploadedImageRef): List<UploadedImageRef> {
        val lock = locks.computeIfAbsent(uid) { Any() }
        synchronized(lock) {
            val current = list(uid, sid) ?: emptyList()
            val next = current + ref
            return put(uid, sid, next)
        }
    }

    @CacheInvalidate(cacheNames = ["uploaded-image-urls"], parameters = ["uid", "sid"])
    fun clear(uid: String, sid: String) {
    }

    fun createSession(uid: String): String {
        val sid = UUID.randomUUID().toString()
        put(uid, sid, emptyList())
        return sid
    }
}
