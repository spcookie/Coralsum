package coralsum.common.dto

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "响应码")
@Serdeable
enum class ResCode(
) {
    SUCCESS,
    FAIL,
    NOT_ACCEPTABLE,
    CONFLICT,
    TOO_MANY_REQUESTS,
    INTERNAL_SERVER_ERROR,
    SERVICE_UNAVAILABLE,
}