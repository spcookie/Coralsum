package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class EstimateParamsResponse(
    val usdToCny: Double,
    val coefficient: Double,
    val flashLiteTokensPerChar: Double,
    val imagePreviewTokensPerMb: Double,
    val flashLiteInputUsdPerMTokens: Double,
    val flashLiteOutputUsdPerMTokens: Double,
    val imagePreviewInputUsdPerMTokens: Double,
    val imagePreviewOutputUsdPerMTokens: Double,
    val imagePricePerResolutionUsd: Map<String, Double>,
    val estimatedBytesPerImage: Map<String, Long>,
    val ossBusyRmbPerGb: Double,
    val ossIdleRmbPerGb: Double,
    val natRmbPerGb: Double,
    val proxyRmbPerGb: Double,
    val visitMultiplier: Double,
    val upscaylEnabled: Boolean,
    val upscaylChargeByScale: Boolean,
    val pointsPerRmb: Double,
)
