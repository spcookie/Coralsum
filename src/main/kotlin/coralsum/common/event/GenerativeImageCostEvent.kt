package coralsum.common.event

data class GenerativeImageCostEvent(
    val uid: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val scale: Int,
    val imageSize: Int,
)
