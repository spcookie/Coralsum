package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class TurnstileVerifyResponse(
    val success: Boolean,
    val hostname: String? = null,
    val action: String? = null,
    val cdata: String? = null,
    val errorCodes: List<String>? = null,
)

