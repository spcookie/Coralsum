package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class EstimateParamsResponse(
    val usdToCny: Double,
    val coefficient: Double,
    val pointsPerRmb: Double,
    val flashLiteTokensPerChar: Double,
    val flashLiteInputUsdPerMTokens: Double,
    val flashLiteOutputUsdPerMTokens: Double,
    val imagePreviewTokensPerMb: Double,
    val proInputUsdPerMTokens: Double,
    val proOutputUsdPerMTokens: Double,
    val proPricePerImage1k2kUsd: Double,
    val proPricePerImage4kUsd: Double,
    val basicInputUsdPerMTokens: Double,
    val basicOutputUsdPerMTokens: Double,
    val basicOutputPricePerImage1kUsd: Double,
    val ossBusyRmbPerGb: Double,
    val ossIdleRmbPerGb: Double,
    val ossBusyStartHour: Int,
    val ossBusyEndHour: Int,
    val trafficNatRmbPerGb: Double,
    val trafficProxyRmbPerGb: Double,
    val trafficVisitMultiplier: Double,
    val upscaylEnabled: Boolean,
    val upscaylChargeByScale: Boolean,
    val estimatePngBytes1K: Long,
    val estimatePngBytes2K: Long,
    val estimatePngBytes4K: Long,
    val estimateJpgBytes1K: Long,
    val estimateJpgBytes2K: Long,
    val estimateJpgBytes4K: Long,
)
