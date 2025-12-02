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
            pointsPerRmb = p.pointsPerRmb,
            flashLiteTokensPerChar = p.flashLite.tokensPerChar,
            flashLiteInputUsdPerMTokens = p.flashLite.inputUsdPerMTokens,
            flashLiteOutputUsdPerMTokens = p.flashLite.outputUsdPerMTokens,
            imagePreviewTokensPerMb = p.imagePreview.tokensPerMb,
            proInputUsdPerMTokens = p.pro.inputUsdPerMTokens,
            proOutputUsdPerMTokens = p.pro.outputUsdPerMTokens,
            proPricePerImage1k2kUsd = p.pro.pricePerImage1k2kUsd,
            proPricePerImage4kUsd = p.pro.pricePerImage4kUsd,
            basicInputUsdPerMTokens = p.basic.inputUsdPerMTokens,
            basicOutputUsdPerMTokens = p.basic.outputUsdPerMTokens,
            basicOutputPricePerImage1kUsd = p.basic.outputPricePerImage1kUsd,
            ossBusyRmbPerGb = p.oss.busyRmbPerGb,
            ossIdleRmbPerGb = p.oss.idleRmbPerGb,
            ossBusyStartHour = p.oss.busyStartHour,
            ossBusyEndHour = p.oss.busyEndHour,
            trafficNatRmbPerGb = p.traffic.natRmbPerGb,
            trafficProxyRmbPerGb = p.traffic.proxyRmbPerGb,
            trafficVisitMultiplier = p.traffic.visitMultiplier,
            upscaylEnabled = p.upscayl.enabled,
            upscaylChargeByScale = p.upscayl.chargeByScale,
            estimatePngBytes1K = p.estimate.pngBytes1K,
            estimatePngBytes2K = p.estimate.pngBytes2K,
            estimatePngBytes4K = p.estimate.pngBytes4K,
            estimateJpgBytes1K = p.estimate.jpgBytes1K,
            estimateJpgBytes2K = p.estimate.jpgBytes2K,
            estimateJpgBytes4K = p.estimate.jpgBytes4K,
        )
        return Res.success(resp)
    }
}
