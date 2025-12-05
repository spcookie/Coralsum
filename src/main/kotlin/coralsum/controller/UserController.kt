package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.enums.UserSource
import coralsum.common.request.ChangePasswordRequest
import coralsum.component.aop.Debounce
import coralsum.service.IAuthService
import coralsum.service.IUserService
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/user")
@Tag(name = "用户")
class UserController(
    val userService: IUserService,
    val authService: IAuthService,
) {
    @Get("/subscribe")
    @Operation(summary = "登记订阅来源", description = "记录用户来源渠道与标识码")
    suspend fun userSubscribe(
        @Parameter(description = "用户来源枚举") @QueryValue("user_source") userSource: UserSource,
        @Parameter(description = "来源标识码") @QueryValue("source_code") sourceCode: String,
    ): Res<Unit> {
        userService.userSubscribe(userSource, sourceCode)
        return Res.success()
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Post("/change-password")
    @Operation(summary = "修改密码", description = "基于邮箱验证码修改或重置密码")
    @Debounce(name = "user.changePassword", windowMillis = 2000, byUid = false)
    suspend fun changePassword(@Body req: ChangePasswordRequest): Res<Map<String, Any>> {
        val ok = authService.changePassword(req.email, req.oldPassword, req.newPassword, req.code)
        return if (ok) Res.success(mapOf("ok" to true)) else Res.fail("修改失败或验证码无效")
    }
}
