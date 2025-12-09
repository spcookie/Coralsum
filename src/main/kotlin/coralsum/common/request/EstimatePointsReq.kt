package coralsum.common.request

import coralsum.common.enums.ImageFormat
import coralsum.common.enums.ImageSize
import coralsum.common.enums.ModelType
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

@Serdeable
data class EstimatePointsReq(
    @field:PositiveOrZero val inputTokens: Int,
    @field:PositiveOrZero val thoughtsTokens: Int = 0,
    @field:PositiveOrZero val outputTokens: Int,
    @field:PositiveOrZero val candidateCount: Int = 1,
    @field:PositiveOrZero val imageCount: Int = 1,
    val imageSizeCategory: ImageSize,
    @field:PositiveOrZero val imageSizeBytes: Int = 0,
    val imageFormat: ImageFormat = ImageFormat.PNG,
    @field:PositiveOrZero val inputCharCount: Int,
    /** 前端用于评估输入图片体积（Bytes，总和） */
    @field:PositiveOrZero val inputPreviewBytes: Int = 0,
    @field:Positive val timestampMs: Long,
    val success: Boolean,
    @field:Min(1) @field:Max(8) val upscaylScale: Int = 1,
    val modelType: ModelType,
)
