package coralsum.cache

import io.micronaut.cache.CacheManager
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@MicronautTest(startApplication = false)
@Property(name = "store.base", value = "/tmp/coralsum-test-store")
@Property(name = "jobrunr.dashboard.enabled", value = "false")
@Property(name = "jobrunr.background-job-server.enabled", value = "false")
@Property(name = "jobrunr.job-scheduler.enabled", value = "false")
@Property(name = "micronaut.server.port", value = "-1")
class UploadedImageCacheTest {

    @Inject
    lateinit var cacheManager: CacheManager<*>

    @Test
    fun uploadedImageUrlsCacheExists() {
        val cache = cacheManager.getCache("uploaded-image-urls")
        assertThat(cache).isNotNull
    }
}
