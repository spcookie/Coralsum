package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.CreatePointsKeyConfigReq
import coralsum.common.request.GeneratePointsKeysReq
import coralsum.common.request.ToggleKeysReq
import coralsum.service.IPointsKeyService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.utils.SecurityService


@Controller("/api/ctl/points-keys")
@Secured("CTL")
class PointsKeyAdminController(
    private val pointsKeyService: IPointsKeyService,
    private val securityService: SecurityService,
) {
    @Post("/config")
    suspend fun createConfig(@Body req: CreatePointsKeyConfigReq): Res<Any> {
        val operator = securityService.authentication.get().name
        val saved = pointsKeyService.createConfig(req, operator)
        return Res.success(saved)
    }

    @Get("/configs")
    suspend fun listConfigs(
        @QueryValue(defaultValue = "1") page: Int,
        @QueryValue(defaultValue = "10") size: Int,
        @QueryValue(defaultValue = "") name: String?,
        @QueryValue(defaultValue = "id") sortBy: String?,
        @QueryValue(defaultValue = "desc") order: String?,
    ): Res<Any> {
        val res = pointsKeyService.listConfigsPaged(page, size, name, sortBy, order)
        return Res.success(res)
    }

    @Post("/generate")
    suspend fun generate(@Body req: GeneratePointsKeysReq): Res<Any> {
        val operator = securityService.authentication.get().name
        val keys = pointsKeyService.generateKeys(req, operator)
        return Res.success(keys)
    }

    @Get("/keys")
    suspend fun listKeys(
        @QueryValue(defaultValue = "1") page: Int,
        @QueryValue(defaultValue = "10") size: Int,
        @QueryValue(defaultValue = "") key: String?,
        @QueryValue(defaultValue = "desc") order: String?,
    ): Res<Any> {
        val res = pointsKeyService.listKeysPaged(page, size, key, order)
        return Res.success(res)
    }

    @Post("/toggle")
    suspend fun toggle(@Body req: ToggleKeysReq): Res<Any> {
        pointsKeyService.toggleKeys(req)
        return Res.success(message = "操作成功")
    }
}
