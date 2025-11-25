package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.response.ProfileResponse
import coralsum.service.IWebUserService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api/web")
@Tag(name = "Web用户")
class WebUserController(
    private val webUserService: IWebUserService,
) {
    @Get("/profile")
    @Operation(summary = "获取个人档案", description = "查询当前认证用户的档案信息")
    @ApiResponse(
        responseCode = "200", description = "查询成功", content = [
            Content(schema = Schema(implementation = ProfileResponse::class))
        ]
    )
    suspend fun getProfile(authentication: Authentication): Res<ProfileResponse> {
        val uid = authentication.name ?: return Res.fail("未登录")
        val resp = webUserService.profile(uid) ?: return Res.fail("用户不存在")
        return Res.success(resp)
    }
}