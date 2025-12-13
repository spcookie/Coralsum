package coralsum.service.impl

import cn.hutool.crypto.digest.HMac
import cn.hutool.crypto.digest.HmacAlgorithm
import com.aliyun.oss.HttpMethod
import com.aliyun.oss.OSS
import com.aliyun.oss.model.GeneratePresignedUrlRequest
import com.aliyun.oss.model.ObjectMetadata
import coralsum.common.enums.GenTaskStatue
import coralsum.common.enums.ImageSize
import coralsum.common.enums.MediaResolution
import coralsum.common.enums.ModelType
import coralsum.common.event.GenerativeImageCostEvent
import coralsum.common.request.EstimatePointsReq
import coralsum.component.excption.BusinessException
import coralsum.component.models.NanoBanana
import coralsum.component.models.Upscayl
import coralsum.config.GoogleConfig
import coralsum.config.OssConfig
import coralsum.config.PreviewConfig
import coralsum.infrastructure.cache.GenerateTaskCache
import coralsum.infrastructure.cache.UploadedImageCache
import coralsum.infrastructure.cache.UploadedImageRef
import coralsum.infrastructure.entity.GenerateImageReqRecord
import coralsum.infrastructure.entity.GenerateImageReqRef
import coralsum.infrastructure.entity.RetrievalImageReqRecord
import coralsum.infrastructure.repository.*
import coralsum.service.*
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
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.json.JsonMapper
import io.micronaut.json.tree.JsonNode
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
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.http.HttpClient
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.jvm.optionals.getOrElse
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Singleton
class GenerativeImageImpl(
    val googleConfig: GoogleConfig,
    val securityService: SecurityService,
    val jsonMapper: JsonMapper,
    val generateImageReqRecordRepository: GenerateImageReqRecordRepository,
    val generateImageReqRefRepository: GenerateImageReqRefRepository,
    val retrievalImageReqRecordRepository: RetrievalImageReqRecordRepository,
    val generateTaskCache: GenerateTaskCache,
    val applicationEventPublisher: ApplicationEventPublisher<GenerativeImageCostEvent>,
    val userPointsService: IUserPointsService,
    val openUserRepository: OpenUserRepository,
    val outletUserRepository: OutletUserRepository,
    val previewConfig: PreviewConfig,
    val uploadedImageCache: UploadedImageCache,
    val ossConfig: OssConfig,
    val oss: OSS,
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

    }

    @PostConstruct
    fun init() {
        nano = NanoBanana(googleConfig.geminiApiKey)
        val userDir = System.getProperty("user.dir")
        val executable = resolveUpscaylExecutable(userDir)
        upscayl = Upscayl(executable)

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
                        .connectTimeout(30.seconds.toJavaDuration())
                )
            )
            .maxRetries(2)
            .build()

    }

    @PreDestroy
    fun destroy() {
        nano.close()
    }

    override suspend fun generate(genRequest: GenRequest, request: HttpRequest<*>): GenResult {
        val uid = securityService.authentication.get().name
        validPoint(uid, genRequest)
        val watch = StopWatch.createStarted()
        val sid = genRequest.imageSessionId
        val cachedRefs = if (sid != null) uploadedImageCache.list(uid, sid) ?: emptyList() else emptyList()
        val finalRefs: List<String> = cachedRefs.map { r -> r.uri }
        val effectiveReq = genRequest.copy(text = genRequest.text, imageUrls = finalRefs)
        val conf = jsonMapper.writeValueAsString(effectiveReq.copy(text = null, imageUrls = null))
        val imageReqRecord = GenerateImageReqRecord(
            requestText = effectiveReq.text,
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
                val pairs = doConcurrentGenerate(effectiveReq, imageReqRecord, uid)
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
                        try {
                            upscayl.input(imagePath.toString())
                                .output(upscaledPath.toString())
                                .model(upscaylModel.modelName)
                                .scale(upscaylScale.scale)
                                .format(genRequest.format.ext)
                                .run()
                            targetPath = if (upscaledPath.toFile().exists()) upscaledPath else imagePath
                            if (targetPath == imagePath) {
                                log.warn("upscayl failed, fallback to original")
                            }
                        } catch (e: Exception) {
                            log.warn("upscayl failed: ${e.message}")
                            targetPath = imagePath
                        }
                    } else {
                        targetPath = imagePath
                    }

                    val key = "${tempDir.name}-${index}.${genRequest.format.ext}"
                    refs.add(key)

                    val bytes = fs.read(targetPath) { readByteArray() }

                    sizes.add(bytes.size)

                    val metadata = ObjectMetadata()
                    metadata.contentDisposition = "attachment; filename=aigi.${genRequest.format.ext}"
                    metadata.contentType = when (genRequest.format.ext.lowercase()) {
                        "png" -> "image/png"
                        "jpg", "jpeg" -> "image/jpeg"
                        "webp" -> "image/webp"
                        else -> "application/octet-stream"
                    }
                    oss.putObject(resolveBucket(), key, ByteArrayInputStream(bytes), metadata)
                    "/api/generative-image?ref=${key}"
                }
                watch.stop()
                imageReqRecord.durationMs = watch.duration.toMillis()

                val text = pairs.map { it.first }.firstOrNull { !it.isNullOrBlank() } ?: ""
                val linkImages = refs.map { "/api/generative-image/link?ref=$it" }
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
                        GenerateImageReqRef(
                            recordId = recordId,
                            imageRef = ref,
                            imageSize = sizes.getOrNull(i)
                        )
                    )
                }
                applicationEventPublisher.publishEvent(
                    GenerativeImageCostEvent(
                        uid = imageReqRecord.userCode!!,
                        recordId = recordId,
                        inputTokens = imageReqRecord.inputTokens,
                        thoughtsTokens = imageReqRecord.thoughtsTokens,
                        outputTokens = imageReqRecord.outputTokens,
                        candidateCount = genRequest.candidateCount,
                        imageCount = refs.size,
                        imageSizeCategory = genRequest.imageSize ?: ImageSize.X1,
                        imageSizeBytes = sizes.sum(),
                        imageFormat = genRequest.format,
                        inputCharCount = imageReqRecord.requestText?.length ?: 0,
                        timestampMs = System.currentTimeMillis(),
                        success = refs.isNotEmpty(),
                        upscaylScale = genRequest.upscaylScale?.scale ?: 1,
                        modelType = genRequest.modelType ?: ModelType.BASIC
                    )
                )
                try {
                    fs.list(tempDir).forEach { fs.delete(it) }
                    fs.delete(tempDir)
                } catch (e: Exception) {
                    log.warn("failed to clean temp directory: ${e.message}")
                }
                try {
                    finalRefs.forEach { r -> nano.delete(r) }
                } catch (_: Exception) {
                }
                if (sid != null) uploadedImageCache.clear(uid, sid)
            }
        }
        return genResult
    }

    private suspend fun validPoint(uid: String, genRequest: GenRequest) {
        val openUser = openUserRepository.findByUid(uid)
        if (openUser == null) throw BusinessException("积分不足")
        val estimateReq = getEstimatePointsReq(uid, genRequest)
        if (!userPointsService.hasEnoughPoints(openUser.id!!, estimateReq)) {
            throw BusinessException("积分不足")
        }
    }

    private fun getEstimatePointsReq(
        uid: String,
        genRequest: GenRequest
    ): EstimatePointsReq {
        val cachedInputRefs = uploadedImageCache.list(uid, (genRequest.imageSessionId ?: "")) ?: emptyList()
        val inputBytes = cachedInputRefs.sumOf { it.size }
        val estimateReq = EstimatePointsReq(
            candidateCount = genRequest.candidateCount,
            imageSizeCategory = genRequest.imageSize ?: ImageSize.X1,
            imageSizeBytes = 0,
            imageFormat = genRequest.format,
            inputCharCount = genRequest.text?.length ?: 0,
            inputPreviewBytes = inputBytes,
            timestampMs = System.currentTimeMillis(),
            success = false,
            upscaylScale = genRequest.upscaylScale?.scale ?: 1,
            modelType = genRequest.modelType ?: ModelType.BASIC,
        )
        return estimateReq
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
        } catch (e: Exception) {
            log.error("assessIntent fallback due to: ${e.message}")
            throw BusinessException("服务异常")
        }
    }

    override suspend fun submitGenerateTask(
        genRequest: GenRequest,
        request: HttpRequest<*>,
    ): String? {
        var finalSid: String? = genRequest.imageSessionId
        withContext(Dispatchers.IO) {
            val uid = securityService.authentication.get().name
            validPoint(uid, genRequest)
            val sid = genRequest.imageSessionId ?: uploadedImageCache.createSession(uid)
            generateTaskCache.cacheGenerateTaskStatue(uid, sid, GenTaskStatue.PROCESSING)
            scope.launch(currentCoroutineContext().minusKey(Job).minusKey(CoroutineDispatcher)) {
                try {
                    val result = generate(genRequest, request)
                    generateTaskCache.cacheGenerateTaskResult(uid, sid, result)
                } catch (e: Exception) {
                    log.error("生成失败: {}", e.message, e)
                    generateTaskCache.cacheGenerateTaskStatue(uid, sid, GenTaskStatue.FAILED)
                }
            }
            finalSid = sid
        }
        return finalSid
    }

    override suspend fun getGenerateTaskResult(sid: String): GenTaskResult {
        val uid = securityService.authentication.get().name
        val cacheGenerateTaskResult = generateTaskCache.getGenerateTaskResult(uid, sid)
        val statue = generateTaskCache.getGenerateTaskStatue(uid, sid)
        return if (cacheGenerateTaskResult != null) {
            generateTaskCache.clearAll(uid, sid)
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
        uid: String,
    ): List<Pair<String?, BufferedImage?>> {
        val futures = (0 until genRequest.candidateCount).map {
            CompletableFuture.supplyAsync({ doGenerate(genRequest, imageReqRecord, uid) }, executor)
        }.toList()
        CompletableFuture.allOf(*futures.toTypedArray()).get()
        return futures.map { it.get() }
    }

    @RateLimiter(name = "callGenerateImage")
    @Retryable(attempts = "2", delay = "1s")
    fun doGenerate(
        genRequest: GenRequest,
        imageReqRecord: GenerateImageReqRecord,
        uid: String,
    ): Pair<String?, BufferedImage?> {
        val sid = genRequest.imageSessionId
        val cachedRefs = if (sid != null) uploadedImageCache.list(uid, sid) ?: emptyList() else emptyList()
        val refs: List<String> = if (genRequest.imageUrls != null) {
            genRequest.imageUrls!!
        } else {
            cachedRefs.map { ref -> ref.uri }.toList()
        }
        val pair = nano.gen(
            genRequest.text!!,
            refs,
            genRequest.aspectRatio?.ratio,
            genRequest.system,
            genRequest.temperature,
            genRequest.maxOutputTokens,
            genRequest.topP,
            genRequest.imageSize?.size ?: ImageSize.X1.size,
            genRequest.mediaResolution?.name ?: MediaResolution.AUTO.name,
            genRequest.modelType?.name ?: "BASIC"
        )
        val usageMetadata = pair.second
        synchronized(imageReqRecord) {
            imageReqRecord.inputTokens += usageMetadata.promptTokenCount().getOrElse { 0 }
            imageReqRecord.thoughtsTokens += usageMetadata.thoughtsTokenCount().getOrElse { 0 }
            imageReqRecord.outputTokens += usageMetadata.candidatesTokenCount().getOrElse { 0 }
            imageReqRecord.retryCount += 1
        }
        val generateResult = pair.first
        return generateResult
    }

    suspend fun uploadImage(image: StreamingFileUpload?, sid: String?): String? {
        val uid = securityService.authentication.get().name
        if (image == null) return null
        val finalSid = sid ?: uploadedImageCache.createSession(uid)
        val up = withContext(Dispatchers.IO) {
            val ins = runCatching { image.asInputStream() }.getOrNull() ?: return@withContext null
            val bytes = runCatching { ins.readAllBytes() }.getOrNull() ?: return@withContext null
            runCatching { nano.upload(ByteArrayInputStream(bytes), image.filename) }.getOrNull()
                ?.let { it to bytes.size }
        }
        if (up != null) uploadedImageCache.append(
            uid,
            finalSid,
            UploadedImageRef(up.first.uri, up.first.mimeType, up.second)
        )
        return finalSid
    }

    private fun sign(ref: String, uid: String, exp: Long): String {
        val hmac = HMac(HmacAlgorithm.HmacSHA256, previewConfig.secret.toByteArray())
        val data = "$ref|$uid|$exp"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.digest(data))
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

    private fun resolveUpscaylExecutable(userDir: String): String {
        val libsDir = Path(userDir).resolve("libs")
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        val isMac = osName.contains("mac") || osName.contains("darwin")
        val isLinux = osName.contains("nux") || osName.contains("nix") || osName.contains("linux")
        val candidates = when {
            isWindows -> listOf(
                libsDir.resolve("win").resolve("upscayl-bin.exe"),
                Path(userDir).resolve("upscayl-bin.exe")
            )

            isMac -> listOf(
                libsDir.resolve("mac").resolve("upscayl-bin"),
                Path(userDir).resolve("upscayl-bin")
            )

            isLinux -> listOf(
                libsDir.resolve("linux").resolve("upscayl-bin"),
                Path(userDir).resolve("upscayl-bin")
            )

            else -> listOf(
                libsDir.resolve("linux").resolve("upscayl-bin"),
                Path(userDir).resolve("upscayl-bin")
            )
        }
        val selected = candidates.firstOrNull { it.toFile().exists() } ?: when {
            isWindows -> libsDir.resolve("win").resolve("upscayl-bin.exe")
            isMac -> libsDir.resolve("mac").resolve("upscayl-bin")
            else -> libsDir.resolve("linux").resolve("upscayl-bin")
        }
        return selected.toAbsolutePath().toString()
    }

    override suspend fun preview(ref: String, ip: String, token: String?): String? {
        val tokenUid = verifyToken(ref, token)
        val isTokenVisit = tokenUid != null
        val limit = if (isTokenVisit) 5 else 2
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
        val expiration = Date(System.currentTimeMillis() + previewConfig.ttlSeconds * 1000)
        val req = GeneratePresignedUrlRequest(resolveBucket(), ref, HttpMethod.GET)
        req.expiration = expiration
        val url = oss.generatePresignedUrl(req)
        return url?.toString()
    }

    override suspend fun linkPage(ref: String): LinkPage? {
        val uid = securityService.authentication.get().name
        val imageRef = generateImageReqRefRepository.findByImageRef(ref) ?: return null
        val record = generateImageReqRecordRepository.findById(imageRef.recordId) ?: return null
        if (record.userCode != uid) return null
        val exp = System.currentTimeMillis() + previewConfig.ttlSeconds * 1000
        val token = sign(ref, uid, exp)
        val ptRaw = "$uid:$exp:$token"
        val pt = java.net.URLEncoder.encode(ptRaw, java.nio.charset.StandardCharsets.UTF_8)
        val refEncoded = java.net.URLEncoder.encode(ref, java.nio.charset.StandardCharsets.UTF_8)
        val src = "/api/generative-image/share?ref=$refEncoded&pt=$pt"
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

    private fun resolveBucket(): String {
        val bucket = ossConfig.bucket
        if (bucket.isNullOrBlank()) throw BusinessException("OSS桶未配置")
        return bucket
    }
}
