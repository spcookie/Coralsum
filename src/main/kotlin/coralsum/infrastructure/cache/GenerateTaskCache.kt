package coralsum.infrastructure.cache

import coralsum.common.enums.GenTaskStatue
import coralsum.service.GenResult
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton

@Singleton
class GenerateTaskCache {

    @CachePut(cacheNames = ["generate-task-result"], parameters = ["uid", "sid"], condition = "#{genResult != null}")
    fun cacheGenerateTaskResult(uid: String, sid: String, genResult: GenResult): GenResult {
        return genResult
    }

    @Cacheable(cacheNames = ["generate-task-result"], parameters = ["uid", "sid"])
    fun getGenerateTaskResult(uid: String, sid: String): GenResult? = null

    @CachePut(cacheNames = ["generate-task-statue"], parameters = ["uid", "sid"], condition = "#{statue != null}")
    fun cacheGenerateTaskStatue(uid: String, sid: String, statue: GenTaskStatue): GenTaskStatue {
        return statue
    }

    @Cacheable(cacheNames = ["generate-task-statue"], parameters = ["uid", "sid"])
    fun getGenerateTaskStatue(uid: String, sid: String): GenTaskStatue? = null

    @CacheInvalidate(cacheNames = ["generate-task-statue"], parameters = ["uid", "sid"])
    fun clearStatue(uid: String, sid: String) {
    }

    @CacheInvalidate(cacheNames = ["generate-task-result"], parameters = ["uid", "sid"])
    fun clearResult(uid: String, sid: String) {
    }

    suspend fun clearAll(uid: String, sid: String) {
        clearResult(uid, sid)
        clearStatue(uid, sid)
    }

}
