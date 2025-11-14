package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.enums.AspectRatio
import coralsum.common.enums.ImageFormat
import coralsum.common.enums.UpscaylModel
import coralsum.common.enums.UpscaylScale
import coralsum.common.response.GenResultResponse
import coralsum.convert.GenerativeConvert
import coralsum.service.impl.GenRequest
import coralsum.service.impl.GenerativeImageImpl
import io.micronaut.core.version.annotation.Version
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponseFactory
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.validation.constraints.NotEmpty
import java.net.URI

@Validated
@Controller("/api/generative-image")
class GenerativeImageController(
    val service: GenerativeImageImpl,
    val convert: GenerativeConvert
) {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Version("v1")
    @Post(consumes = ["multipart/form-data"])
    suspend fun generate(
        @Part("image") completedFileUpload: CompletedFileUpload?,
        @Part @NotEmpty text: String,
        @Part system: String?,
        @Part aspectRatio: AspectRatio?,
        @Part candidateCount: Int?,
        @Part temperature: Float?,
        @Part topP: Float?,
        @Part maxOutputTokens: Int?,
        @Part format: ImageFormat?,
        @Part upscaylModel: UpscaylModel?,
        @Part upscaylScale: UpscaylScale?,
        request: HttpRequest<*>
    ): Res<GenResultResponse> {
        val result = service.generate(
            GenRequest(
                text,
                completedFileUpload?.bytes,
                candidateCount ?: 1,
                aspectRatio?.ratio ?: AspectRatio.R1_1.ratio,
                system,
                temperature ?: 0.5f,
                maxOutputTokens ?: 32768,
                topP ?: 1f,
                format?.ext ?: ImageFormat.PNG.ext,
                upscaylModel?.modelName ?: UpscaylModel.NMKD_SUPERSCALE_SP_178000_G_X4.modelName,
                upscaylScale?.scale ?: UpscaylScale.X4.scale
            ),
            request
        )
        return Res.success(convert.toResponse(result))
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Version("v1")
    @Get
    suspend fun preview(@QueryValue ref: String): HttpResponse<*> {
        val url = service.preview(ref)
        return HttpResponseFactory.INSTANCE
            .status<Any>(HttpStatus.FOUND)
            .headers { headers -> headers.location(URI.create(url)) }
    }
}