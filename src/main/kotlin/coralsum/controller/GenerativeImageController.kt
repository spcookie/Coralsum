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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotEmpty
import java.net.URI

@Validated
@Controller("/api/generative-image")
@Tag(name = "生成图片")
class GenerativeImageController(
    val service: GenerativeImageImpl,
    val convert: GenerativeConvert,
    val addressResolver: HttpClientAddressResolver,
) {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post(consumes = ["multipart/form-data"])
    @Operation(
        summary = "生成图片",
        description = "根据文本与可选图片生成结果，支持宽高比、采样、格式与放大设置"
    )
    @ApiResponse(
        responseCode = "200", description = "生成成功", content = [
            Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = GenResultResponse::class))
        ]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA)])
    suspend fun generate(
        @Parameter(description = "参考图片文件") @Part("image") completedFileUpload: CompletedFileUpload?,
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
        request: HttpRequest<*>,
    ): Res<GenResultResponse> {
        val result = service.generate(
            GenRequest(
                text = text,
                image = completedFileUpload?.bytes,
                candidateCount = candidateCount ?: 1,
                aspectRatio = aspectRatio ?: AspectRatio.R1_1,
                system = system,
                temperature = temperature ?: 0.5f,
                maxOutputTokens = maxOutputTokens ?: 32768,
                topP = topP ?: 1f,
                format = format ?: ImageFormat.PNG,
                upscaylModel = upscaylModel ?: UpscaylModel.HIGH_FIDELITY_4X,
                upscaylScale = upscaylScale ?: UpscaylScale.X1,
                imageSize = imageSize ?: ImageSize.X1
            ),
            request
        )
        return Res.success(convert.toResponse(result))
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Version("v1")
    @Get
    @Operation(summary = "预览链接跳转", description = "根据引用标识返回图片URL重定向")
    suspend fun preview(@QueryValue ref: String, request: HttpRequest<*>): HttpResponse<*> {
        val ip = addressResolver.resolve(request)
        val url = service.preview(ref, ip)
        return if (url == null) {
            HttpResponse.notFound<Any>()
        } else {
            HttpResponse.status<Any>(HttpStatus.FOUND)
                .headers { headers -> headers.location(URI.create(url)) }
        }
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
        @Parameter(description = "参考图片文件") @Part("image") completedFileUpload: CompletedFileUpload?,
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
        request: HttpRequest<*>,
    ): Res<Unit> {
        service.submitGenerateTask(
            GenRequest(
                text = text,
                image = completedFileUpload?.bytes,
                candidateCount = candidateCount ?: 1,
                aspectRatio = aspectRatio ?: AspectRatio.R1_1,
                system = system,
                temperature = temperature ?: 0.5f,
                maxOutputTokens = maxOutputTokens ?: 32768,
                topP = topP ?: 1f,
                format = format ?: ImageFormat.PNG,
                upscaylModel = upscaylModel ?: UpscaylModel.HIGH_FIDELITY_4X,
                upscaylScale = upscaylScale ?: UpscaylScale.X1,
                imageSize = imageSize ?: ImageSize.X1
            ),
            request
        )
        return Res.success()
    }

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