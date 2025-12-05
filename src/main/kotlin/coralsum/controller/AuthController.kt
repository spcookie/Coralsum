package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.RegisterRequest
import coralsum.common.request.ResetPasswordRequest
import coralsum.common.request.SendCodeRequest
import coralsum.component.aop.Debounce
import coralsum.service.IAuthService
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.http.HttpRequest
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
    private val turnstileService: coralsum.service.ITurnstileService,
) {
    @Post("/send-code")
    @Operation(summary = "发送验证码")
    @Debounce(name = "auth.sendCode", windowMillis = 60000, byUid = false)
    suspend fun sendCode(@Body req: SendCodeRequest): Res<Map<String, Any>> {
        val purpose = (req.purpose ?: "REGISTER").uppercase()
        val ok = authService.sendEmailCode(req.email, purpose)
        if (ok) return Res.success(mapOf("ok" to true))
        val msg = lms.getMessage("auth.send_code.failed").orElse("Error")
        return Res.fail(msg)
    }

    @Post("/register")
    @Operation(summary = "邮箱注册")
    @Debounce(name = "auth.register", windowMillis = 2000, byUid = false)
    suspend fun register(@Body req: RegisterRequest, request: HttpRequest<Any>): Res<Map<String, Any>> {
        val token = request.headers.get("CF-Turnstile-Response") ?: request.headers.get("X-Turnstile-Token")
        val remoteIp = request.headers.get("CF-Connecting-IP") ?: request.headers.get("X-Forwarded-For")
        val passed = turnstileService.validate(token, remoteIp)
        if (!passed) {
            val msg = lms.getMessage("auth.turnstile.invalid").orElse("Invalid verification")
            return Res.fail(msg)
        }
        val ok = authService.register(req.email, req.password, req.code)
        if (ok) return Res.success(mapOf("ok" to true))
        val msg = lms.getMessage("auth.register.failed").orElse("Error")
        return Res.fail(msg)
    }

    @Post("/reset-password")
    @Operation(summary = "重置密码")
    @Debounce(name = "auth.resetPassword", windowMillis = 2000, byUid = false)
    suspend fun resetPassword(@Body req: ResetPasswordRequest, request: HttpRequest<Any>): Res<Map<String, Any>> {
        val token = request.headers.get("CF-Turnstile-Response") ?: request.headers.get("X-Turnstile-Token")
        val remoteIp = request.headers.get("CF-Connecting-IP") ?: request.headers.get("X-Forwarded-For")
        val passed = turnstileService.validate(token, remoteIp)
        if (!passed) {
            val msg = lms.getMessage("auth.turnstile.invalid").orElse("Invalid verification")
            return Res.fail(msg)
        }
        val ok = authService.resetPassword(req.email, req.newPassword, req.code)
        if (ok) return Res.success(mapOf("ok" to true))
        val msg = lms.getMessage("auth.reset_password.failed").orElse("Error")
        return Res.fail(msg)
    }
}
