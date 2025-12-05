package coralsum.job

import com.aliyun.oss.OSS
import coralsum.config.OssConfig
import coralsum.infrastructure.repository.GenerateImageReqRecordRepository
import coralsum.infrastructure.repository.GenerateImageReqRefRepository
import coralsum.toolkit.logger
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.annotations.Recurring
import java.time.LocalDateTime

@Singleton
class PreviewImageCleanupJob(
    private val oss: OSS,
    private val ossConfig: OssConfig,
    private val generateImageReqRecordRepository: GenerateImageReqRecordRepository,
    private val generateImageReqRefRepository: GenerateImageReqRefRepository,
) {

    private val log = logger<PreviewImageCleanupJob>()

    @Recurring(id = "preview-image-cleanup", interval = "PT1M")
    @Job(name = "清理过期预览图片")
    fun cleanup() {
        val cutoff = LocalDateTime.now().minusMinutes(30)
        val expired = runBlocking { generateImageReqRecordRepository.findExpiredBefore(cutoff) }
        expired.forEach { record ->
            val refs = runBlocking { generateImageReqRefRepository.findAllByRecordId(record.id!!) }
            refs.forEach { r ->
                try {
                    val bucket = ossConfig.bucket ?: throw RuntimeException("OSS桶未配置")
                    oss.deleteObject(bucket, r.imageRef)
                } catch (e: Exception) {
                    log.warn("预览图片删除失败: {}", e.message, e)
                }
            }
            runBlocking { generateImageReqRefRepository.deleteAllByRecordId(record.id!!) }
        }
    }

}
