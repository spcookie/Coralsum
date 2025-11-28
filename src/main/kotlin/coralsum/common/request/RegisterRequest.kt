package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Schema(description = "注册请求")
data class RegisterRequest(
    @field:Schema(description = "邮箱") val email: String,
    @field:Schema(description = "密码") val password: String,
    @field:Schema(description = "验证码") val code: String,
)