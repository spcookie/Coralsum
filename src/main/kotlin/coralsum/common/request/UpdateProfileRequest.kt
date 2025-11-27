package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Schema(description = "更新档案请求")
data class UpdateProfileRequest(
    @field:Schema(description = "昵称", example = "Alice")
    val nickName: String,
)