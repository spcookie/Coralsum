package coralsum.controller

import coralsum.aop.Debounce
import coralsum.common.dto.Res
import coralsum.common.request.UpdateProfileRequest
import coralsum.common.response.ProfileResponse
import coralsum.service.IWebUserService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
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

    @Put("/profile")
    @Operation(summary = "更新个人档案", description = "修改当前认证用户的档案信息（仅昵称）")
    @ApiResponse(
        responseCode = "200", description = "更新成功", content = [
            Content(schema = Schema(implementation = ProfileResponse::class))
        ]
    )
    @Debounce(name = "web.updateProfile", windowMillis = 2000, byUid = true)
    suspend fun updateProfile(authentication: Authentication, @Body req: UpdateProfileRequest): Res<ProfileResponse> {
        val uid = authentication.name ?: return Res.fail("未登录")
        val ok = webUserService.updateNickName(uid, req.nickName)
        if (!ok) return Res.fail("更新失败")
        val resp = webUserService.profile(uid) ?: return Res.fail("用户不存在")
        return Res.success(resp)
    }
}
