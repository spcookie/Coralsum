package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Serdeable
@Schema(description = "发送邮箱验证码请求")
data class SendCodeRequest(
    @field:Schema(description = "邮箱") @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式错误") val email: String,
    @field:Schema(description = "用途: REGISTER/RESET") val purpose: String? = null,
)