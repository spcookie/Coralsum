package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Schema(description = "更新语言偏好请求")
data class UpdateLanguageRequest(
    @field:Schema(description = "语言代码", example = "zh-CN")
    val language: String,
)

