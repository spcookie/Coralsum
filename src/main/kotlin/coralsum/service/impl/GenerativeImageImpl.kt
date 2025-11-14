package coralsum.service.impl

import coralsum.config.CloudflareConfig
import coralsum.config.NanoConfig
import coralsum.entity.GenerateImageReqRecord
import coralsum.repository.GenerateImageReqRecordRepository
import coralsum.service.IGenerativeImage
import coralsum.toolkit.NanoBanana
import coralsum.toolkit.RuntimePath
import coralsum.toolkit.Upscayl
import coralsum.toolkit.logger
import io.micronaut.http.HttpRequest
import io.micronaut.json.JsonMapper
import io.micronaut.objectstorage.aws.AwsS3Operations
import io.micronaut.objectstorage.request.PresignRequest
import io.micronaut.objectstorage.request.UploadRequest
import io.micronaut.retry.annotation.Retryable
import io.micronaut.security.utils.SecurityService
import io.micronaut.serde.annotation.Serdeable
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.apache.commons.lang3.time.StopWatch
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory

@Singleton
class GenerativeImageImpl(
    val store: AwsS3Operations,
    val nanoConfig: NanoConfig,
    val cloudflareConfig: CloudflareConfig,
    val securityService: SecurityService,
    val jsonMapper: JsonMapper,
    val generateImageReqRecordRepository: GenerateImageReqRecordRepository
) : IGenerativeImage {

    private lateinit var nano: NanoBanana

    private lateinit var upscayl: Upscayl

    companion object {
        @JvmStatic
        private val log = logger<IGenerativeImage>()
    }

    @PostConstruct
    fun init() {
        nano = NanoBanana(nanoConfig.apiKey)
        upscayl = Upscayl(RuntimePath.getExecutableDir().resolve("upscayl-bin").toString())
    }

    @PreDestroy
    fun destroy() {
        nano.close()
    }

    override suspend fun generate(genRequest: GenRequest, request: HttpRequest<*>): GenResult {
        val watch = StopWatch.createStarted()
        val conf = jsonMapper.writeValueAsString(genRequest.copy(text = null, image = null))
        val imageReqRecord = GenerateImageReqRecord(
            requestText = genRequest.text,
            requestImage = null,
            requestConfig = conf,
            userCode = securityService.authentication.get().name,
        )
        val genResult = withContext(Dispatchers.IO) {
            val fs = FileSystem.SYSTEM
            val tempDir = createTempDirectory("coralsum-nano-").toAbsolutePath().toString().toPath()

            try {
                val pairs = doGenerate(genRequest, imageReqRecord)

                val groups = pairs.mapIndexed { index, pair ->
                    val (text, bufferedImage) = pair

                    // 生成临时文件路径
                    val imagePath = tempDir / "image-$index.${genRequest.format}"
                    val upscaledPath = tempDir / "image-upscayl-$index.${genRequest.format}"

                    ImageIO.write(bufferedImage, genRequest.format, imagePath.toFile())

                    // 调用 upscayl
                    var targetPath: okio.Path
                    val upscaylModel = genRequest.upscaylModel
                    val upscaylScale = genRequest.upscaylScale
                    if (upscaylModel != null && upscaylScale != null) {
                        targetPath = upscaledPath
                        upscayl.input(imagePath.toString())
                            .output(upscaledPath.toString())
                            .model(upscaylModel)
                            .scale(upscaylScale)
                            .format(genRequest.format)
                            .run()
                    } else {
                        targetPath = imagePath
                    }

                    val bytes = fs.read(targetPath) { readByteArray() }
                    val key = tempDir.name + "." + genRequest.format
                    val uploadRequest = UploadRequest.fromBytes(bytes, key)
                    store.upload(uploadRequest) { builder ->
                        builder.contentDisposition("attachment; filename=AIGI.${genRequest.format}")
                    }
                    val url = cloudflareConfig.host + request.path
                    GenResult.Group(text, url + "?ref=${key}")
                }
                GenResult(groups)
            } finally {
                try {
                    fs.list(tempDir).forEach { fs.delete(it) }
                    fs.delete(tempDir)
                } catch (e: Exception) {
                    log.warn("failed to clean temp directory: ${e.message}")
                }
            }
        }
        watch.stop()
        imageReqRecord.durationMs = watch.duration.toMillis()
        generateImageReqRecordRepository.save(imageReqRecord)
        return genResult
    }

    @Retryable(attempts = "3", delay = "500ms")
    fun doGenerate(genRequest: GenRequest, imageReqRecord: GenerateImageReqRecord): List<Pair<String?, BufferedImage>> {
        val pairs = nano.gen(
            genRequest.text!!,
            genRequest.image,
            genRequest.candidateCount,
            genRequest.aspectRatio,
            genRequest.system,
            genRequest.temperature,
            genRequest.maxOutputTokens,
            genRequest.topP
        )
        val usageMetadata = pairs.second
        imageReqRecord.inputTokens = imageReqRecord.inputTokens + usageMetadata.promptTokenCount().get()
        imageReqRecord.outputTokens = imageReqRecord.outputTokens + usageMetadata.candidatesTokenCount().get()
        imageReqRecord.retryCount = imageReqRecord.retryCount + 1
        return pairs.first
    }

    override suspend fun preview(ref: String): String {
        val presignRequest = PresignRequest
            .builder(ref, PresignRequest.Operation.DOWNLOAD)
            .build()
        val response = store.presign(presignRequest)
        return response.url.toString()
    }
}

@Serdeable
data class GenRequest(
    val text: String? = null,
    var image: ByteArray? = null,
    var candidateCount: Int = 1,
    var aspectRatio: String? = null,
    var system: String? = null,
    var temperature: Float = 1f,
    var maxOutputTokens: Int = 32768,
    var topP: Float = 1f,
    var format: String = "png",
    var upscaylModel: String? = null,
    var upscaylScale: Int? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenRequest) return false

        if (candidateCount != other.candidateCount) return false
        if (temperature != other.temperature) return false
        if (maxOutputTokens != other.maxOutputTokens) return false
        if (topP != other.topP) return false
        if (upscaylScale != other.upscaylScale) return false
        if (text != other.text) return false
        if (!image.contentEquals(other.image)) return false
        if (aspectRatio != other.aspectRatio) return false
        if (system != other.system) return false
        if (format != other.format) return false
        if (upscaylModel != other.upscaylModel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = candidateCount
        result = 31 * result + temperature.hashCode()
        result = 31 * result + maxOutputTokens
        result = 31 * result + topP.hashCode()
        result = 31 * result + (upscaylScale ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (aspectRatio?.hashCode() ?: 0)
        result = 31 * result + (system?.hashCode() ?: 0)
        result = 31 * result + format.hashCode()
        result = 31 * result + (upscaylModel?.hashCode() ?: 0)
        return result
    }

}

data class GenResult(
    val groups: List<Group>
) {
    data class Group(
        val text: String? = null,
        val url: String? = null
    )
}