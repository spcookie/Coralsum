package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Schema(description = "重置密码请求")
data class ResetPasswordRequest(
    @field:Schema(description = "邮箱") val email: String,
    @field:Schema(description = "新密码") val newPassword: String,
    @field:Schema(description = "验证码") val code: String,
)