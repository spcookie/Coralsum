package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.ChangePasswordRequest
import coralsum.common.request.RegisterRequest
import coralsum.common.request.ResetPasswordRequest
import coralsum.common.request.SendCodeRequest
import coralsum.component.annotation.Debounce
import coralsum.component.annotation.Turnstile
import coralsum.service.IAuthService
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/api/auth")
@Tag(name = "认证")
class AuthController(
    private val authService: IAuthService,
    private val lms: LocalizedMessageSource,
) {
    @Post("/send-code")
    @Operation(summary = "发送验证码")
    @Debounce(name = "auth.sendCode", windowMillis = 60000, byUid = true)
    suspend fun sendCode(@Body req: SendCodeRequest): Res<Map<String, Any>> {
        val purpose = (req.purpose ?: "REGISTER").uppercase()
        val ok = authService.sendEmailCode(req.email, purpose)
        if (ok) return Res.success(mapOf("ok" to true))
        val msg = lms.getMessage("auth.send_code.failed").orElse("Error")
        return Res.fail(msg)
    }

    @Post("/register")
    @Operation(summary = "邮箱注册")
    @Debounce(name = "auth.register", windowMillis = 2000, byUid = true)
    @Turnstile
    suspend fun register(@Body req: RegisterRequest): Res<Map<String, Any>> {
        val ok = authService.register(req.email, req.password, req.code)
        if (ok) return Res.success(mapOf("ok" to true))
        val msg = lms.getMessage("auth.register.failed").orElse("Error")
        return Res.fail(msg)
    }

    @Post("/reset-password")
    @Operation(summary = "重置密码")
    @Debounce(name = "auth.resetPassword", windowMillis = 2000, byUid = true)
    @Turnstile
    suspend fun resetPassword(@Body req: ResetPasswordRequest): Res<Map<String, Any>> {
        val ok = authService.resetPassword(req.email, req.newPassword, req.code)
        if (ok) return Res.success(mapOf("ok" to true))
        val msg = lms.getMessage("auth.reset_password.failed").orElse("Error")
        return Res.fail(msg)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Post("/change-password")
    @Operation(summary = "修改密码", description = "基于邮箱验证码修改或重置密码")
    @Debounce(name = "user.changePassword", windowMillis = 2000, byUid = false)
    @Turnstile
    suspend fun changePassword(@Body req: ChangePasswordRequest): Res<Map<String, Any>> {
        val ok = authService.changePassword(req.email, req.oldPassword, req.newPassword, req.code)
        return if (ok) Res.success(mapOf("ok" to true)) else Res.fail("修改失败或验证码无效")
    }
}
