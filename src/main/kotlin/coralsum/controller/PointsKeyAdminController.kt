package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.CreatePointsKeyConfigReq
import coralsum.common.request.GeneratePointsKeysReq
import coralsum.common.request.ToggleConfigReq
import coralsum.common.request.ToggleKeysReq
import coralsum.component.annotation.Debounce
import coralsum.service.IPointsKeyService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.utils.SecurityService
import io.micronaut.validation.Validated
import jakarta.validation.Valid


@Controller("/api/ctl/points-keys")
@Secured("CTL")
@Validated
class PointsKeyAdminController(
    private val pointsKeyService: IPointsKeyService,
    private val securityService: SecurityService,
) {
    @Post("/config")
    @Debounce(name = "pointsKey.createConfig", windowMillis = 2000, byUid = true)
    suspend fun createConfig(@Body @Valid req: CreatePointsKeyConfigReq): Res<Any> {
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
    @Debounce(name = "pointsKey.generate", windowMillis = 2000, byUid = true)
    suspend fun generate(@Body @Valid req: GeneratePointsKeysReq): Res<Any> {
        val operator = securityService.authentication.get().name
        val keys = pointsKeyService.generateKeys(req, operator)
        return Res.success(keys)
    }

    @Get("/keys")
    suspend fun listKeys(
        @QueryValue(defaultValue = "1") page: Int,
        @QueryValue(defaultValue = "10") size: Int,
        @QueryValue(defaultValue = "") key: String?,
        @QueryValue(defaultValue = "used") sortBy: String?,
        @QueryValue(defaultValue = "desc") order: String?,
    ): Res<Any> {
        val res = pointsKeyService.listKeysPaged(page, size, key, sortBy, order)
        return Res.success(res)
    }

    @Post("/toggle")
    @Debounce(name = "pointsKey.toggle", windowMillis = 2000, byUid = true)
    suspend fun toggle(@Body @Valid req: ToggleKeysReq): Res<Any> {
        pointsKeyService.toggleKeys(req)
        return Res.success(message = "操作成功")
    }

    @Post("/config/toggle")
    @Debounce(name = "pointsKey.toggleConfig", windowMillis = 2000, byUid = true)
    suspend fun toggleConfig(@Body @Valid req: ToggleConfigReq): Res<Any> {
        val updated = pointsKeyService.toggleConfig(req)
        return Res.success(updated)
    }
}
