package coralsum.controller

import coralsum.aop.Debounce
import coralsum.common.dto.Res
import coralsum.common.request.RedeemPointsReq
import coralsum.common.response.EstimateParamsResponse
import coralsum.config.PricingConfig
import coralsum.service.IPointsKeyService
import io.micronaut.core.version.annotation.Version
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid

@Controller("/api/points")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Validated
class PointsController(
    private val pointsKeyService: IPointsKeyService,
    private val pricingConfig: PricingConfig,
) {
    @Post("/redeem")
    @Debounce(name = "points.redeem", windowMillis = 2000, byUid = true)
    suspend fun redeem(authentication: Authentication, request: HttpRequest<*>, @Body @Valid req: RedeemPointsReq): Res<Any> {
        val uid = authentication.name
        val ip = request.remoteAddress?.address?.hostAddress
        val resp = pointsKeyService.redeem(uid, ip, req.key)
        return Res.success(resp)
    }

    @Version("v1")
    @Get("/estimate-params")
    @Operation(summary = "获取估算参数", description = "返回前端用于估算扣减的价格参数")
    suspend fun estimateParams(): Res<EstimateParamsResponse> {
        val p = pricingConfig
        val resp = EstimateParamsResponse(
            usdToCny = p.usdToCny,
            coefficient = p.coefficient,
            flashLiteTokensPerChar = p.flashLite.tokensPerChar,
            imagePreviewTokensPerMb = p.imagePreview.tokensPerMb,
            flashLiteInputUsdPerMTokens = p.flashLite.inputUsdPerMTokens,
            flashLiteOutputUsdPerMTokens = p.flashLite.outputUsdPerMTokens,
            imagePreviewInputUsdPerMTokens = p.imagePreview.inputUsdPerMTokens,
            imagePreviewOutputUsdPerMTokens = p.imagePreview.outputUsdPerMTokens,
            imagePricePerResolutionUsd = mapOf(
                "1K" to p.imagePreview.pricePerImage1K2KUsd,
                "2K" to p.imagePreview.pricePerImage1K2KUsd,
                "4K" to p.imagePreview.pricePerImage4KUsd,
            ),
            estimatedBytesPerImage = mapOf(
                "PNG:1K" to p.estimate.pngBytes1K,
                "PNG:2K" to p.estimate.pngBytes2K,
                "PNG:4K" to p.estimate.pngBytes4K,
                "JPG:1K" to p.estimate.jpgBytes1K,
                "JPG:2K" to p.estimate.jpgBytes2K,
                "JPG:4K" to p.estimate.jpgBytes4K,
            ),
            ossBusyRmbPerGb = p.oss.busyRmbPerGb,
            ossIdleRmbPerGb = p.oss.idleRmbPerGb,
            natRmbPerGb = p.traffic.natRmbPerGb,
            proxyRmbPerGb = p.traffic.proxyRmbPerGb,
            visitMultiplier = p.traffic.visitMultiplier,
            upscaylEnabled = p.upscayl.enabled,
            upscaylChargeByScale = p.upscayl.chargeByScale,
            pointsPerRmb = p.pointsPerRmb,
        )
        return Res.success(resp)
    }
}
