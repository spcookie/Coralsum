package coralsum.cache

import coralsum.common.enums.GenTaskStatue
import coralsum.service.GenResult
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton

@Singleton
class GenerateTaskCache {


    @CachePut(cacheNames = ["generate-task-result"], parameters = ["uid"], condition = "#{genResult != null}")
    fun cacheGenerateTaskResult(uid: String, genResult: GenResult): GenResult {
        return genResult
    }

    @Cacheable(cacheNames = ["generate-task-result"], parameters = ["uid"])
    fun getGenerateTaskResult(uid: String): GenResult? = /*resultMem[uid]*/ null

    @CachePut(cacheNames = ["generate-task-statue"], parameters = ["uid"], condition = "#{statue != null}")
    fun cacheGenerateTaskStatue(uid: String, statue: GenTaskStatue): GenTaskStatue {
        return statue
    }

    @Cacheable(cacheNames = ["generate-task-statue"], parameters = ["uid"])
    fun getGenerateTaskStatue(uid: String): GenTaskStatue? = /*statueMem[uid]*/ null

    @CacheInvalidate(cacheNames = ["generate-task-statue"], parameters = ["uid"])
    fun clearStatue(uid: String) {
    }

    @CacheInvalidate(cacheNames = ["generate-task-result"], parameters = ["uid"])
    fun clearResult(uid: String) {
    }

    suspend fun clearAll(uid: String) {
        clearResult(uid)
        clearStatue(uid)
    }

}