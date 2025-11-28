package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.RedeemPointsReq
import coralsum.service.IPointsKeyService
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule

@Controller("/api/points")
@Secured(SecurityRule.IS_AUTHENTICATED)
class PointsController(
    private val pointsKeyService: IPointsKeyService,
) {
    @Post("/redeem")
    suspend fun redeem(authentication: Authentication, request: HttpRequest<*>, @Body req: RedeemPointsReq): Res<Any> {
        val uid = authentication.name
        val ip = request.remoteAddress?.address?.hostAddress
        val resp = pointsKeyService.redeem(uid, ip, req.key)
        return Res.success(resp)
    }
}