package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.RegisterRequest
import coralsum.common.request.ResetPasswordRequest
import coralsum.common.request.SendCodeRequest
import coralsum.service.IAuthService
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
) {
    @Post("/send-code")
    @Operation(summary = "发送验证码")
    suspend fun sendCode(@Body req: SendCodeRequest): Res<Map<String, Any>> {
        val purpose = (req.purpose ?: "REGISTER").uppercase()
        val ok = authService.sendEmailCode(req.email, purpose)
        return if (ok) Res.success(mapOf("ok" to true)) else Res.fail("发送失败")
    }

    @Post("/register")
    @Operation(summary = "邮箱注册")
    suspend fun register(@Body req: RegisterRequest): Res<Map<String, Any>> {
        val ok = authService.register(req.email, req.password, req.code)
        return if (ok) Res.success(mapOf("ok" to true)) else Res.fail("注册失败或验证码无效")
    }

    @Post("/reset-password")
    @Operation(summary = "重置密码")
    suspend fun resetPassword(@Body req: ResetPasswordRequest): Res<Map<String, Any>> {
        val ok = authService.resetPassword(req.email, req.newPassword, req.code)
        return if (ok) Res.success(mapOf("ok" to true)) else Res.fail("重置失败或验证码无效")
    }
}