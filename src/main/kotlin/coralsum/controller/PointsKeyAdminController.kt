package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.CreatePointsKeyConfigReq
import coralsum.common.request.GeneratePointsKeysReq
import coralsum.common.request.ToggleKeysReq
import coralsum.repository.OpenUserRepository
import coralsum.service.IPointsKeyService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Inject

@Controller("/api/ctl/points-keys")
@Secured(SecurityRule.IS_AUTHENTICATED)
class PointsKeyAdminController(
    private val pointsKeyService: IPointsKeyService,
    private val securityService: SecurityService,
    @Inject private val openUserRepository: OpenUserRepository,
) {
    private suspend fun requireCtl(): String {
        val uid = securityService.authentication.get().name
        val ou = openUserRepository.findByUid(uid) ?: throw IllegalStateException("无权限：用户不存在")
        val perms = ou.assignRoleList() ?: emptyList()
        if (!perms.contains("CTL")) throw IllegalStateException("无权限：需要CTL权限")
        return uid
    }

    @Post("/config")
    suspend fun createConfig(@Body req: CreatePointsKeyConfigReq): Res<Any> {
        val operator = requireCtl()
        val saved = pointsKeyService.createConfig(req, operator)
        return Res.success(saved)
    }

    @Get("/configs")
    suspend fun listConfigs(): Res<Any> {
        requireCtl()
        val list = pointsKeyService.listConfigs()
        return Res.success(list)
    }

    @Post("/generate")
    suspend fun generate(@Body req: GeneratePointsKeysReq): Res<Any> {
        val operator = requireCtl()
        val keys = pointsKeyService.generateKeys(req, operator)
        return Res.success(keys)
    }

    @Get("/keys")
    suspend fun listKeys(): Res<Any> {
        requireCtl()
        val list = pointsKeyService.listKeys()
        return Res.success(list)
    }

    @Post("/toggle")
    suspend fun toggle(@Body req: ToggleKeysReq): Res<Any> {
        requireCtl()
        pointsKeyService.toggleKeys(req)
        return Res.success(message = "操作成功")
    }
}