package coralsum.common.response

data class TurnstileVerifyResponse(
    val success: Boolean,
    val hostname: String? = null,
    val action: String? = null,
    val cdata: String? = null,
    val error_codes: List<String>? = null,
)

