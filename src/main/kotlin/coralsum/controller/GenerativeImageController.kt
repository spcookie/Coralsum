package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.enums.*
import coralsum.common.response.GenResultResponse
import coralsum.common.response.GenTaskResultResponse
import coralsum.common.response.IntentAssessmentResponse
import coralsum.component.aop.Debounce
import coralsum.convert.GenerativeConvert
import coralsum.infrastructure.repository.OpenUserRepository
import coralsum.infrastructure.repository.OutletUserRepository
import coralsum.service.impl.GenerativeImageImpl
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.core.version.annotation.Version
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.http.server.util.HttpClientAddressResolver
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import io.micronaut.views.ModelAndView
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URL
import java.util.*

@Validated
@Controller("/api/generative-image")
@Tag(name = "生成图片")
class GenerativeImageController(
    val service: GenerativeImageImpl,
    val convert: GenerativeConvert,
    val addressResolver: HttpClientAddressResolver,
    val openUserRepository: OpenUserRepository,
    val outletUserRepository: OutletUserRepository,
    val lms: LocalizedMessageSource,
) {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post(consumes = ["multipart/form-data"])
    @Operation(
        summary = "生成图片",
        description = "根据文本与可选图片（支持多张）生成结果，支持宽高比、采样、格式与放大设置"
    )
    @Debounce(name = "gi.generate", windowMillis = 3000, byUid = true)
    suspend fun generate(
        @Parameter(description = "图片上传标识") @Part("sid") sid: String?,
        @Parameter(description = "生成文本", required = true) @Part @NotEmpty text: String,
        @Parameter(description = "系统提示") @Part system: String?,
        @Parameter(description = "宽高比") @Part aspectRatio: AspectRatio?,
        @Parameter(description = "模型类型") @Part modelType: ModelType?,
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
        val genReq = convert.toRequest(
            text,
            sid,
            modelType,
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
        )
        val result = service.generate(genReq, request)
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
                .headers { headers ->
                    headers.location(URI.create(url))
                    headers.add("Cache-Control", "no-store, no-cache, must-revalidate")
                    headers.add("Pragma", "no-cache")
                    headers.add("Expires", "0")
                }
        }
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Get("/link")
    @Operation(summary = "获取分享直链", description = "返回带签名的预览直链，指向 /share 接口")
    suspend fun link(
        @QueryValue ref: String,
        @QueryValue("darkMode") darkMode: Boolean?,
    ): HttpResponse<String> {
        val page = service.linkPage(ref) ?: return HttpResponse.notFound()
        val url = if (darkMode == true) page.src + "&darkMode=true" else page.src
        return HttpResponse.ok(url)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Version("v1")
    @Get("/share")
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "预览图片页面", description = "读取图片字节并在页面中以Base64内嵌展示")
    suspend fun share(
        @QueryValue ref: String,
        @QueryValue("pt") token: String,
        @QueryValue("darkMode") darkMode: Boolean?,
        request: HttpRequest<*>,
    ): HttpResponse<ModelAndView<Map<String, Any>>> {
        val ip = addressResolver.resolve(request)
        val tokenNorm = token.replace(' ', '+')
        val url =
            service.preview(ref, ip, tokenNorm) ?: throw HttpStatusException(
                HttpStatus.NOT_FOUND,
                "资源不存在或无权访问"
            )
        val bytes = try {
            withContext(Dispatchers.IO) {
                URL(url).openStream().use { it.readBytes() }
            }
        } catch (_: Exception) {
            return HttpResponse.serverError()
        }
        val mime = when (ref.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
        val base64 = Base64.getEncoder().encodeToString(bytes)
        val dataUrl = "data:${mime};base64,${base64}"
        val parts = tokenNorm.split(":")
        val uid = parts.getOrNull(0) ?: ""
        val exp = parts.getOrNull(1)?.toLongOrNull()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val time = exp?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                .format(formatter)
        } ?: ""
        val user = if (uid.isNotBlank()) {
            val openUser = openUserRepository.findByUid(uid)
            if (openUser != null) {
                val outletWeb = outletUserRepository.findByOpenUserIdAndUserSource(
                    openUser.id!!,
                    UserSource.WEB
                )
                outletWeb?.nickName ?: ""
            } else ""
        } else ""
        val title = lms.getMessage("image.preview.title").orElse("Image Preview")
        val mv = ModelAndView(
            "image-link",
            mapOf<String, Any>(
                "src" to dataUrl,
                "time" to time,
                "user" to user,
                "dark" to (darkMode == true),
                "title" to title,
                "lang" to request.getLocale().orElse(java.util.Locale.ENGLISH).toLanguageTag()
            )
        )
        return HttpResponse.ok(mv).headers { headers ->
            headers.add("Cache-Control", "no-store")
        }
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post("/assess-intent", consumes = [MediaType.TEXT_PLAIN])
    @Operation(summary = "评估生成意图", description = "基于用户文本判断是否为生成/修改图片意图")
    @Debounce(name = "gi.assessIntent", windowMillis = 1500, byUid = true)
    suspend fun assessIntent(@Body @NotEmpty text: String): Res<IntentAssessmentResponse> {
        val assessment = service.assessIntent(text)
        return Res.success(convert.toResponse(assessment))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post("/submit-task", consumes = ["multipart/form-data"])
    @Operation(summary = "提交生成任务", description = "提交异步图片生成任务")
    @Debounce(name = "gi.submitTask", windowMillis = 3000, byUid = true)
    suspend fun submitGenerateTask(
        @Parameter(description = "图片上传标识") @Part("sid") sid: String?,
        @Parameter(description = "生成文本", required = true) @Part @NotEmpty text: String,
        @Parameter(description = "系统提示") @Part system: String?,
        @Parameter(description = "宽高比") @Part aspectRatio: AspectRatio?,
        @Parameter(description = "模型类型") @Part modelType: ModelType?,
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
        val genReq = convert.toRequest(
            text,
            sid,
            modelType,
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
        )
        service.submitGenerateTask(genReq, request)
        return Res.success()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post("/upload", consumes = [MediaType.MULTIPART_FORM_DATA])
    @Operation(
        summary = "上传参考图片",
        description = "单文件上传，并缓存，返回会话ID"
    )
    @Debounce(name = "gi.upload", windowMillis = 3000, byUid = true)
    suspend fun uploadImage(image: StreamingFileUpload, @Part("sid") sid: String?): Res<String?> {
        val sessionId = service.uploadImage(image, sid)
        return Res.success(sessionId)
    }


    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Get("/get-task-result")
    @Operation(summary = "查询生成任务结果", description = "获取最近一次生成任务的状态与结果")
    suspend fun getGenerateTaskResult(): Res<GenTaskResultResponse> {
        val generateTaskResult = service.getGenerateTaskResult()
        return Res.success(convert.toResponse(generateTaskResult))
    }

}
