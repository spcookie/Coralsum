package coralsum.common.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class ResCode(
) {
    SUCCESS,
    FAIL,
    NOT_FOUND,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_ACCEPTABLE,
    CONFLICT,
    UNSUPPORTED_MEDIA_TYPE,
    UNPROCESSABLE_ENTITY,
    TOO_MANY_REQUESTS,
    INTERNAL_SERVER_ERROR,
    SERVICE_UNAVAILABLE,
}