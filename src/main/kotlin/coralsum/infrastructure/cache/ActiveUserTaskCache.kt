package coralsum.infrastructure.cache

import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.CachePut
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton

@Singleton
class ActiveUserTaskCache {

    @CachePut(cacheNames = ["active-user-tasks"], parameters = ["uid"], condition = "#{sid != null}")
    fun setActiveUserTask(uid: String, sid: String): String {
        return sid
    }

    @Cacheable(cacheNames = ["active-user-tasks"], parameters = ["uid"])
    fun getActiveUserTask(uid: String): String? = null

    @CacheInvalidate(cacheNames = ["active-user-tasks"], parameters = ["uid"])
    fun clearActiveUserTask(uid: String) {
    }
}