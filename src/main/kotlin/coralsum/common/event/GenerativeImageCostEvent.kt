package coralsum.common.event

import coralsum.common.enums.ImageFormat
import coralsum.common.enums.ImageSize
import coralsum.common.enums.ModelType

data class GenerativeImageCostEvent(
    val uid: String,
    val recordId: Long?,
    val inputTokens: Int,
    val thoughtsTokens: Int,
    val outputTokens: Int,
    val candidateCount: Int,
    val imageCount: Int,
    val imageSizeCategory: ImageSize,
    val imageSizeBytes: Int,
    var imageFormat: ImageFormat,
    val inputCharCount: Int,
    val timestampMs: Long,
    val success: Boolean,
    val upscaylScale: Int,
    val modelType: ModelType,
)
