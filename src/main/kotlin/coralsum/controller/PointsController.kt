package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.RedeemPointsReq
import coralsum.common.response.EstimateParamsResponse
import coralsum.common.response.RedeemPointsResp
import coralsum.component.annotation.Debounce
import coralsum.config.PricingConfig
import coralsum.convert.PricingConvert
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
    private val pricingConvert: PricingConvert,
    private val pointsEstimateService: coralsum.service.IPointsEstimateService,
) {
    @Post("/redeem")
    @Debounce(name = "points.redeem", windowMillis = 2000, byUid = true)
    suspend fun redeem(
        authentication: Authentication,
        request: HttpRequest<*>,
        @Body @Valid req: RedeemPointsReq,
    ): Res<RedeemPointsResp> {
        val uid = authentication.name
        val ip = request.remoteAddress?.address?.hostAddress
        val resp = pointsKeyService.redeem(uid, ip, req.key)
        return Res.success(resp)
    }

    @Version("v1")
    @Get("/estimate-params")
    @Operation(summary = "获取估算参数", description = "返回前端用于估算扣减的价格参数")
    suspend fun estimateParams(): Res<EstimateParamsResponse> {
        val resp = pricingConvert.toResponse(pricingConfig)
        return Res.success(resp)
    }

    @Version("v1")
    @Post("/estimate")
    @Operation(summary = "积分扣减预估", description = "根据输入参数返回预估成本与积分扣减")
    suspend fun estimate(@Body @Valid req: coralsum.common.request.EstimatePointsReq): Res<coralsum.common.response.EstimatePointsResp> {
        val resp = pointsEstimateService.estimate(req)
        return Res.success(resp)
    }
}
