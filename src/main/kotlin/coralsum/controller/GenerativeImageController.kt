package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.enums.*
import coralsum.common.response.GenResultResponse
import coralsum.common.response.GenTaskResultResponse
import coralsum.common.response.IntentAssessmentResponse
import coralsum.convert.GenerativeConvert
import coralsum.service.GenRequest
import coralsum.service.impl.GenerativeImageImpl
import io.micronaut.core.version.annotation.Version
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.server.util.HttpClientAddressResolver
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import io.micronaut.views.ModelAndView
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.net.URI
import java.net.URL

@Validated
@Controller("/api/generative-image")
@Tag(name = "生成图片")
class GenerativeImageController(
    val service: GenerativeImageImpl,
    val convert: GenerativeConvert,
    val addressResolver: HttpClientAddressResolver,
) {
    private val log = LoggerFactory.getLogger(GenerativeImageController::class.java)

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post(consumes = ["multipart/form-data"])
    @Operation(
        summary = "生成图片",
        description = "根据文本与可选图片（支持多张）生成结果，支持宽高比、采样、格式与放大设置"
    )
    @ApiResponse(
        responseCode = "200", description = "生成成功", content = [
            Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = GenResultResponse::class))
        ]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA)])
    suspend fun generate(
        @Parameter(description = "参考图片文件") @Part("image") images: Publisher<CompletedFileUpload>?,
        @Parameter(description = "生成文本", required = true) @Part @NotEmpty text: String,
        @Parameter(description = "系统提示") @Part system: String?,
        @Parameter(description = "宽高比") @Part aspectRatio: AspectRatio?,
        @Parameter(description = "候选数量") @Part candidateCount: Int?,
        @Parameter(description = "温度") @Part temperature: Float?,
        @Parameter(description = "TopP") @Part topP: Float?,
        @Parameter(description = "最大输出tokens") @Part maxOutputTokens: Int?,
        @Parameter(description = "图片格式") @Part format: ImageFormat?,
        @Parameter(description = "Upscayl模型") @Part upscaylModel: UpscaylModel?,
        @Parameter(description = "Upscayl倍数") @Part upscaylScale: UpscaylScale?,
        @Parameter(description = "图片分辨率") @Part imageSize: ImageSize?,
        @Parameter(description = "媒体分辨率") @Part mediaResolution: MediaResolution?,
        request: HttpRequest<*>,
    ): Res<GenResultResponse> {
        val imgs = images?.let { Flux.from(it).collectList().awaitSingle() }
        log.info("接收图片数量: {}", imgs?.size ?: 0)
        val result = service.generate(
            buildGenRequest(
                text,
                imgs,
                candidateCount,
                aspectRatio,
                system,
                temperature,
                maxOutputTokens,
                topP,
                format,
                upscaylModel,
                upscaylScale,
                imageSize,
                mediaResolution
            ),
            request
        )
        return Res.success(convert.toResponse(result))
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Version("v1")
    @Get
    @Operation(summary = "预览链接跳转", description = "根据引用标识返回图片URL重定向")
    suspend fun preview(
        @QueryValue ref: String,
        @QueryValue("pt") token: String?,
        request: HttpRequest<*>,
    ): HttpResponse<*> {
        val ip = addressResolver.resolve(request)
        val url = service.preview(ref, ip, token)
        return if (url == null) {
            HttpResponse.notFound<Any>()
        } else {
            HttpResponse.status<Any>(HttpStatus.FOUND)
                .headers { headers -> headers.location(URI.create(url)) }
        }
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Get("/link")
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "图片预览页面", description = "根据引用标识返回图片HTML页面")
    suspend fun link(
        @QueryValue ref: String,
        request: HttpRequest<*>,
    ): HttpResponse<ModelAndView<Map<String, Any>>> {
        val page =
            service.linkPage(ref) ?: return HttpResponse.notFound<ModelAndView<Map<String, Any>>>()
        val mv = ModelAndView(
            "image-link",
            mapOf<String, Any>("src" to page.src, "time" to page.time, "user" to page.user)
        )
        return HttpResponse.ok(mv)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Get("/bytes")
    @Operation(summary = "预览图片字节", description = "服务端读取并返回图片字节")
    suspend fun previewBytes(
        @QueryValue ref: String,
        @QueryValue("pt") token: String?,
        request: HttpRequest<*>,
    ): HttpResponse<ByteArray> {
        val ip = addressResolver.resolve(request)
        val url = service.preview(ref, ip, token) ?: return HttpResponse.notFound()
        val bytes = try {
            withContext(Dispatchers.IO) {
                URL(url).openStream().use { it.readBytes() }
            }
        } catch (_: Exception) {
            return HttpResponse.serverError()
        }
        val contentType = when (ref.substringAfterLast('.', "").lowercase()) {
            "png" -> MediaType.IMAGE_PNG
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "webp" -> MediaType.of("image/webp")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
        return HttpResponse.ok(bytes).contentType(contentType)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Version("v1")
    @Post("/assess-intent", consumes = [MediaType.TEXT_PLAIN])
    @Operation(summary = "评估生成意图", description = "基于用户文本判断是否为生成/修改图片意图")
    @ApiResponse(
        responseCode = "200", description = "评估成功", content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = Schema(implementation = IntentAssessmentResponse::class)
            )
        ]
    )
    @RequestBody(content = [Content(mediaType = MediaType.TEXT_PLAIN)])
    suspend fun assessIntent(@Body @NotEmpty text: String): Res<IntentAssessmentResponse> {
        val assessment = service.assessIntent(text)
        return Res.success(
            IntentAssessmentResponse(
                generateIntent = assessment.generateIntent,
                guideMessage = assessment.guideMessage
            )
        )
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post("/submit-task", consumes = ["multipart/form-data"])
    @Operation(summary = "提交生成任务", description = "提交异步图片生成任务")
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA)])
    suspend fun submitGenerateTask(
        @Part("image") images: Publisher<CompletedFileUpload>?,
        @Parameter(description = "生成文本", required = true) @Part @NotEmpty text: String,
        @Parameter(description = "系统提示") @Part system: String?,
        @Parameter(description = "宽高比") @Part aspectRatio: AspectRatio?,
        @Parameter(description = "候选数量") @Part candidateCount: Int?,
        @Parameter(description = "温度") @Part temperature: Float?,
        @Parameter(description = "TopP") @Part topP: Float?,
        @Parameter(description = "最大输出tokens") @Part maxOutputTokens: Int?,
        @Parameter(description = "图片格式") @Part format: ImageFormat?,
        @Parameter(description = "Upscayl模型") @Part upscaylModel: UpscaylModel?,
        @Parameter(description = "Upscayl倍数") @Part upscaylScale: UpscaylScale?,
        @Parameter(description = "图片分辨率") @Part imageSize: ImageSize?,
        @Parameter(description = "媒体分辨率") @Part mediaResolution: MediaResolution?,
        request: HttpRequest<*>,
    ): Res<Unit> {
        val imgs = images?.let { Flux.from(it).collectList().awaitSingle() }
        service.submitGenerateTask(
            buildGenRequest(
                text,
                imgs,
                candidateCount,
                aspectRatio,
                system,
                temperature,
                maxOutputTokens,
                topP,
                format,
                upscaylModel,
                upscaylScale,
                imageSize,
                mediaResolution
            ),
            request
        )
        return Res.success()
    }

    private fun buildGenRequest(
        text: String,
        images: List<CompletedFileUpload>?,
        candidateCount: Int?,
        aspectRatio: AspectRatio?,
        system: String?,
        temperature: Float?,
        maxOutputTokens: Int?,
        topP: Float?,
        format: ImageFormat?,
        upscaylModel: UpscaylModel?,
        upscaylScale: UpscaylScale?,
        imageSize: ImageSize?,
        mediaResolution: MediaResolution?,
    ): GenRequest = GenRequest(
        text = text,
        images = images?.mapNotNull { it.bytes },
        candidateCount = candidateCount ?: 1,
        aspectRatio = aspectRatio ?: AspectRatio.R1_1,
        system = system,
        temperature = temperature ?: 0.5f,
        maxOutputTokens = maxOutputTokens ?: 32768,
        topP = topP ?: 1f,
        format = format ?: ImageFormat.PNG,
        upscaylModel = upscaylModel ?: UpscaylModel.HIGH_FIDELITY_4X,
        upscaylScale = upscaylScale ?: UpscaylScale.X1,
        imageSize = imageSize ?: ImageSize.X1,
        mediaResolution = mediaResolution ?: MediaResolution.AUTO
    )

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Get("/get-task-result")
    @Operation(summary = "查询生成任务结果", description = "获取最近一次生成任务的状态与结果")
    @ApiResponse(
        responseCode = "200", description = "查询成功", content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = Schema(implementation = GenTaskResultResponse::class)
            )
        ]
    )
    suspend fun getGenerateTaskResult(): Res<GenTaskResultResponse> {
        val generateTaskResult = service.getGenerateTaskResult()
        return Res.success(convert.toResponse(generateTaskResult))
    }

}
