package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Schema(description = "发送邮箱验证码请求")
data class SendCodeRequest(
    @field:Schema(description = "邮箱") val email: String,
    @field:Schema(description = "用途: REGISTER/RESET") val purpose: String? = null,
)