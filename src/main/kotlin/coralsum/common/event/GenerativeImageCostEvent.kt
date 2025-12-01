package coralsum.common.event

import coralsum.common.enums.ImageSize

data class GenerativeImageCostEvent(
    val uid: String,
    val recordId: Long?,
    val inputTokens: Int,
    val outputTokens: Int,
    val candidateCount: Int,
    val imageCount: Int,
    val imageSizeCategory: ImageSize,
    val imageSizeBytes: Int,
    val inputCharCount: Int,
    val timestampMs: Long,
    val success: Boolean,
)
