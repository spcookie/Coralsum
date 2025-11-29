package coralsum.service.impl

import cn.hutool.core.codec.Base64
import cn.hutool.crypto.digest.HMac
import cn.hutool.crypto.digest.HmacAlgorithm
import coralsum.cache.GenerateTaskCache
import coralsum.common.enums.GenTaskStatue
import coralsum.common.enums.ImageSize
import coralsum.common.enums.MediaResolution
import coralsum.common.event.GenerativeImageCostEvent
import coralsum.config.CloudflareConfig
import coralsum.config.GoogleConfig
import coralsum.config.PreviewConfig
import coralsum.entity.GenerateImageReqRecord
import coralsum.entity.RetrievalImageReqRecord
import coralsum.excption.BusinessException
import coralsum.repository.GenerateImageReqRecordRepository
import coralsum.repository.OpenUserRepository
import coralsum.repository.OutletUserRepository
import coralsum.repository.RetrievalImageReqRecordRepository
import coralsum.service.*
import coralsum.toolkit.NanoBanana
import coralsum.toolkit.Upscayl
import coralsum.toolkit.logger
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder
import dev.langchain4j.model.chat.Capability
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchema
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import io.github.resilience4j.micronaut.annotation.RateLimiter
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.http.HttpRequest
import io.micronaut.json.JsonMapper
import io.micronaut.json.tree.JsonNode
import io.micronaut.objectstorage.aws.AwsS3Operations
import io.micronaut.objectstorage.request.PresignRequest
import io.micronaut.objectstorage.request.UploadRequest
import io.micronaut.retry.annotation.Retryable
import io.micronaut.security.utils.SecurityService
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.apache.commons.lang3.time.StopWatch
import java.awt.image.BufferedImage
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.http.HttpClient
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.jvm.optionals.getOrElse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Singleton
class GenerativeImageImpl(
    val store: AwsS3Operations,
    val googleConfig: GoogleConfig,
    val cloudflareConfig: CloudflareConfig,
    val securityService: SecurityService,
    val jsonMapper: JsonMapper,
    val generateImageReqRecordRepository: GenerateImageReqRecordRepository,
    val generateImageReqRefRepository: coralsum.repository.GenerateImageReqRefRepository,
    val retrievalImageReqRecordRepository: RetrievalImageReqRecordRepository,
    val generateTaskCache: GenerateTaskCache,
    val applicationEventPublisher: ApplicationEventPublisher<GenerativeImageCostEvent>,
    val userPointsService: IUserPointsService,
    val openUserRepository: OpenUserRepository,
    val outletUserRepository: OutletUserRepository,
    val previewConfig: PreviewConfig,
) : IGenerativeImage {

    private lateinit var nano: NanoBanana

    private lateinit var upscayl: Upscayl

    private lateinit var gemini: ChatModel

    companion object {
        @JvmStatic
        private val log = logger<IGenerativeImage>()

        @JvmStatic
        private val executor = ThreadPoolExecutor(
            60, 60,
            0L, TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(1000),
            BasicThreadFactory.builder()
                .namingPattern("image-generator-%d")
                .build()
        )

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private const val MAX_PRE_COUNT = 5
    }

    @PostConstruct
    fun init() {
        nano = NanoBanana(googleConfig.geminiApiKey)
        val userDir = System.getProperty("user.dir")
        val libsDir = Path(userDir).resolve("libs")
        val osName = System.getProperty("os.name").lowercase()
        val candidates = listOf(
            libsDir.resolve("win").resolve("upscayl-bin.exe"),
            libsDir.resolve("mac").resolve("upscayl-bin"),
            Path(userDir).resolve("upscayl-bin"),
            Path(userDir).resolve("upscayl-bin.exe")
        )
        val executable = candidates.firstOrNull { it.toFile().exists() } ?: libsDir.resolve(
            if (osName.contains("win")) "win/upscayl-bin.exe" else "mac/upscayl-bin"
        )
        upscayl = Upscayl(executable.toAbsolutePath().toString())

        gemini = GoogleAiGeminiChatModel.builder()
            .apiKey(googleConfig.geminiApiKey)
            .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
            .modelName("gemini-2.5-flash-lite")
            .logRequestsAndResponses(true)
            .httpClientBuilder(
                JdkHttpClientBuilder().httpClientBuilder(
                    HttpClient.newBuilder()
                        .apply {
                            val host = System.getProperty("http.proxyHost")
                            val port = System.getProperty("http.proxyPort")?.toInt()
                            if (host != null && port != null) {
                                proxy(
                                    ProxySelector.of(InetSocketAddress(host, port))
                                )
                            }
                        }
                        .connectTimeout(1.minutes.toJavaDuration())
                )
            )
            .maxRetries(2)
            .build()
    }

    @PreDestroy
    fun destroy() {
        nano.close()
    }

    @RateLimiter(name = "callGenerateImage")
    override suspend fun generate(genRequest: GenRequest, request: HttpRequest<*>): GenResult {
        val uid = securityService.authentication.get().name
        val openUser = openUserRepository.findByUid(uid)
        if (!userPointsService.hasEnoughPoints(openUser!!.id!!)) {
            throw BusinessException("积分不足")
        }
        val watch = StopWatch.createStarted()
        val conf = jsonMapper.writeValueAsString(genRequest.copy(text = null, images = null))
        val imageReqRecord = GenerateImageReqRecord(
            requestText = genRequest.text,
            requestImage = null,
            requestConfig = conf,
            userCode = uid,
        )
        val genResult = withContext(Dispatchers.IO) {
            val fs = FileSystem.SYSTEM
            val tempDir = createTempDirectory("coralsum-").toAbsolutePath().toString().toPath()
            val refs = mutableListOf<String>()
            val sizes = mutableListOf<Int>()
            try {
                val pairs = doConcurrentGenerate(genRequest, imageReqRecord)
                val images = pairs.mapIndexed { index, pair ->
                    val (_, bufferedImage) = pair

                    if (bufferedImage == null) {
                        return@mapIndexed null
                    }

                    val imagePath = tempDir / "image-$index.${genRequest.format.ext}"
                    ImageIO.write(bufferedImage, genRequest.format.ext, imagePath.toFile())

                    var targetPath: Path
                    val upscaylModel = genRequest.upscaylModel
                    val upscaylScale = genRequest.upscaylScale
                    if (upscaylModel != null && upscaylScale != null && upscaylScale.scale > 1) {
                        val upscaledPath = tempDir / "image-upscayl-$index.${genRequest.format.ext}"
                        targetPath = upscaledPath
                        upscayl.input(imagePath.toString())
                            .output(upscaledPath.toString())
                            .model(upscaylModel.modelName)
                            .scale(upscaylScale.scale)
                            .format(genRequest.format.ext)
                            .run()
                    } else {
                        targetPath = imagePath
                    }

                    val key = "${tempDir.name}-${index}.${genRequest.format.ext}"
                    refs.add(key)

                    val bytes = fs.read(targetPath) { readByteArray() }

                    sizes.add(bytes.size)

                    val uploadRequest = UploadRequest.fromBytes(bytes, key)
                    store.upload(uploadRequest) { builder ->
                        builder.contentDisposition("attachment; filename=aigi.${genRequest.format.ext}")
                    }
                    cloudflareConfig.host + "/api/generative-image?ref=${key}"
                }
                watch.stop()
                imageReqRecord.durationMs = watch.duration.toMillis()

                val text = pairs.map { it.first }.firstOrNull { !it.isNullOrBlank() } ?: ""
                val linkImages = refs.map { cloudflareConfig.host + "/api/generative-image/link?ref=" + it }
                GenResult(
                    inputTokens = imageReqRecord.inputTokens,
                    outputTokens = imageReqRecord.outputTokens,
                    durationMs = imageReqRecord.durationMs.toInt(),
                    images = images,
                    linkImages = linkImages,
                    text = text
                )
            } finally {
                val saved = generateImageReqRecordRepository.save(imageReqRecord)
                val recordId = saved.id!!
                for ((i, ref) in refs.withIndex()) {
                    generateImageReqRefRepository.save(
                        coralsum.entity.GenerateImageReqRef(
                            recordId = recordId,
                            imageRef = ref,
                            imageSize = sizes.getOrNull(i)
                        )
                    )
                }
                applicationEventPublisher.publishEvent(
                    GenerativeImageCostEvent(
                        uid = imageReqRecord.userCode!!,
                        inputTokens = imageReqRecord.inputTokens,
                        outputTokens = imageReqRecord.outputTokens,
                        scale = (genRequest.upscaylScale?.scale ?: 1) - 1,
                        imageSize = sizes.sum()
                    )
                )
                try {
                    fs.list(tempDir).forEach { fs.delete(it) }
                    fs.delete(tempDir)
                } catch (e: Exception) {
                    log.warn("failed to clean temp directory: ${e.message}")
                }
            }
        }
        return genResult
    }

    override suspend fun assessIntent(userMessage: String): IntentAssessment {
        val schema = JsonSchema.builder()
            .name("IntentAssessment")
            .rootElement(
                JsonObjectSchema.builder()
                    .addBooleanProperty("generateIntent")
                    .addStringProperty("guideMessage")
                    .required("generateIntent")
                    .build()
            )
            .build()

        val responseFormat = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(schema)
            .build()

        val systemPrompt = """
            你是图像生成助手。请判断下面用户消息是否在表达“生成图片或修改图片”的意图。
            仅返回 JSON：{"generateIntent": boolean, "guideMessage": string}
            - 当 generateIntent 为 true 时，guideMessage 为空或省略。
            - 当 generateIntent 为 false 时，guideMessage 用中文简洁地引导用户给出与生成/修改图片相关的内容，举1-2个示例。
        """.trimIndent()

        return try {
            val chatResponse = gemini.chat(
                ChatRequest.builder()
                    .messages(
                        SystemMessage.systemMessage(systemPrompt),
                        UserMessage.from(userMessage)
                    )
                    .responseFormat(responseFormat)
                    .build()
            )

            val json = chatResponse.aiMessage().text()
            val node = jsonMapper.readValue(json, JsonNode::class.java)
            IntentAssessment(
                node["generateIntent"].booleanValue,
                node["guideMessage"]?.stringValue ?: ""
            )
//            jsonMapper.readValue(json.toByteArray(), IntentAssessment::class.java)
        } catch (e: Exception) {
            log.warn("assessIntent fallback due to: ${e.message}")
            IntentAssessment(
                generateIntent = false,
                guideMessage = ""
            )
        }
    }

    override suspend fun submitGenerateTask(
        genRequest: GenRequest,
        request: HttpRequest<*>,
    ) {
        withContext(Dispatchers.IO) {
            val uid = securityService.authentication.get().name
            generateTaskCache.cacheGenerateTaskStatue(uid, GenTaskStatue.PROCESSING)
            scope.launch(currentCoroutineContext().minusKey(Job).minusKey(CoroutineDispatcher)) {
                try {
                    val result = generate(genRequest, request)
                    generateTaskCache.cacheGenerateTaskResult(uid, result)
                } catch (e: Exception) {
                    log.error("生成失败: {}", e.message, e)
                    generateTaskCache.cacheGenerateTaskStatue(uid, GenTaskStatue.FAILED)
                }
            }
        }
    }

    override suspend fun getGenerateTaskResult(): GenTaskResult {
        val uid = securityService.authentication.get().name
        val cacheGenerateTaskResult = generateTaskCache.getGenerateTaskResult(uid)
        val statue = generateTaskCache.getGenerateTaskStatue(uid)
        return if (cacheGenerateTaskResult != null) {
            generateTaskCache.clearAll(uid)
            return GenTaskResult(
                status = GenTaskStatue.COMPLETED,
                result = cacheGenerateTaskResult
            )
        } else if (statue == null) {
            GenTaskResult(
                status = GenTaskStatue.NONE
            )
        } else {
            GenTaskResult(
                status = statue
            )
        }
    }

    private fun doConcurrentGenerate(
        genRequest: GenRequest,
        imageReqRecord: GenerateImageReqRecord,
    ): List<Pair<String?, BufferedImage?>> {
        val futures = (0 until genRequest.candidateCount).map {
            CompletableFuture.supplyAsync({ doGenerate(genRequest, imageReqRecord) }, executor)
        }.toList()
        CompletableFuture.allOf(*futures.toTypedArray()).get()
        return futures.map { it.get() }
    }

    @Retryable(attempts = "3", delay = "500ms")
    fun doGenerate(
        genRequest: GenRequest,
        imageReqRecord: GenerateImageReqRecord,
    ): Pair<String?, BufferedImage?> {
        val pair = nano.gen(
            genRequest.text!!,
            genRequest.images,
            genRequest.aspectRatio?.ratio,
            genRequest.system,
            genRequest.temperature,
            genRequest.maxOutputTokens,
            genRequest.topP,
            genRequest.imageSize?.size ?: ImageSize.X1.size,
            genRequest.mediaResolution?.name ?: MediaResolution.AUTO.name
        )
        val usageMetadata = pair.second
        synchronized(imageReqRecord) {
            imageReqRecord.inputTokens += usageMetadata.promptTokenCount().getOrElse { 0 }
            imageReqRecord.outputTokens += usageMetadata.candidatesTokenCount().getOrElse { 0 }
            imageReqRecord.retryCount += 1
        }
        val generateResult = pair.first
        val retry = generateResult.first == null && generateResult.second == null
        if (retry) {
            throw BusinessException("模型返回内容为空")
        }
        return generateResult
    }

    private fun sign(ref: String, uid: String, exp: Long): String {
        val hmac = HMac(HmacAlgorithm.HmacSHA256, previewConfig.secret.toByteArray())
        val data = "$ref|$uid|$exp"
        return Base64.encode(hmac.digest(data))
    }

    private fun verifyToken(ref: String, token: String?): String? {
        if (token.isNullOrBlank()) return null
        val parts = token.split(":")
        if (parts.size != 3) return null
        val uid = parts[0]
        val exp = parts[1].toLongOrNull() ?: return null
        val sig = parts[2]
        if (System.currentTimeMillis() > exp) return null
        val expect = sign(ref, uid, exp)
        return if (expect == sig) uid else null
    }

    override suspend fun preview(ref: String, ip: String, token: String?): String? {
        val tokenUid = verifyToken(ref, token)
        val isTokenVisit = tokenUid != null
        val limit = if (isTokenVisit) 10 else 3
        val count = retrievalImageReqRecordRepository.countByImageRefAndIsTokenVisit(ref, isTokenVisit)
        if (count >= limit) return null
        val imageRef = generateImageReqRefRepository.findByImageRef(ref) ?: return null
        val record = generateImageReqRecordRepository.findById(imageRef.recordId) ?: return null
        if (isTokenVisit) {
            if (record.userCode != tokenUid) return null
        }
        retrievalImageReqRecordRepository.save(
            RetrievalImageReqRecord(imageRef = ref, ip = ip, isTokenVisit = isTokenVisit)
        )
        val presignRequest = PresignRequest.builder(ref, PresignRequest.Operation.DOWNLOAD).build()
        val response = store.presign(presignRequest)
        return response.url.toString()
    }

    override suspend fun linkPage(ref: String): LinkPage? {
        val uid = securityService.authentication.get().name
        val imageRef = generateImageReqRefRepository.findByImageRef(ref) ?: return null
        val record = generateImageReqRecordRepository.findById(imageRef.recordId) ?: return null
        if (record.userCode != uid) return null
        val exp = System.currentTimeMillis() + previewConfig.ttlSeconds * 1000
        val token = sign(ref, uid, exp)
        val src = "/api/generative-image?ref=" + ref + "&pt=" + "$uid:$exp:$token"
        val time = record.createTime?.let {
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it)
        } ?: ""
        val user = record.userCode?.let { code ->
            val openUser = openUserRepository.findByUid(code)
            if (openUser != null) {
                val outletWeb = outletUserRepository.findByOpenUserIdAndUserSource(
                    openUser.id!!,
                    coralsum.common.enums.UserSource.WEB
                )
                outletWeb?.nickName ?: ""
            } else ""
        } ?: ""
        return LinkPage(src = src, time = time, user = user)
    }
}
