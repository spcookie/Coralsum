package coralsum.convert

import coralsum.common.response.EstimateParamsResponse
import coralsum.config.PricingConfig
import org.mapstruct.*

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
interface PricingConvert {
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "usdToCny", source = "usdToCny")
    @Mapping(target = "coefficient", source = "coefficient")
    @Mapping(target = "pointsPerRmb", source = "pointsPerRmb")
    @Mapping(target = "flashLiteTokensPerChar", source = "flashLite.tokensPerChar")
    @Mapping(target = "flashLiteInputUsdPerMTokens", source = "flashLite.inputUsdPerMTokens")
    @Mapping(target = "flashLiteOutputUsdPerMTokens", source = "flashLite.outputUsdPerMTokens")
    @Mapping(target = "imagePreviewTokensPerMb", source = "imagePreview.tokensPerMb")
    @Mapping(target = "proInputUsdPerMTokens", source = "pro.inputUsdPerMTokens")
    @Mapping(target = "proOutputUsdPerMTokens", source = "pro.outputUsdPerMTokens")
    @Mapping(target = "proPricePerImage1k2kUsd", source = "pro.pricePerImage1k2kUsd")
    @Mapping(target = "proPricePerImage4kUsd", source = "pro.pricePerImage4kUsd")
    @Mapping(target = "basicInputUsdPerMTokens", source = "basic.inputUsdPerMTokens")
    @Mapping(target = "basicOutputUsdPerMTokens", source = "basic.outputUsdPerMTokens")
    @Mapping(target = "basicOutputPricePerImage1kUsd", source = "basic.outputPricePerImage1kUsd")
    @Mapping(target = "ossBusyRmbPerGb", source = "oss.busyRmbPerGb")
    @Mapping(target = "ossIdleRmbPerGb", source = "oss.idleRmbPerGb")
    @Mapping(target = "ossBusyStartHour", source = "oss.busyStartHour")
    @Mapping(target = "ossBusyEndHour", source = "oss.busyEndHour")
    @Mapping(target = "trafficNatRmbPerGb", source = "traffic.natRmbPerGb")
    @Mapping(target = "trafficProxyRmbPerGb", source = "traffic.proxyRmbPerGb")
    @Mapping(target = "trafficVisitMultiplier", source = "traffic.visitMultiplier")
    @Mapping(target = "upscaylEnabled", source = "upscayl.enabled")
    @Mapping(target = "upscaylChargeByScale", source = "upscayl.chargeByScale")
    @Mapping(target = "estimatePngBytes1K", source = "estimate.pngBytes1K")
    @Mapping(target = "estimatePngBytes2K", source = "estimate.pngBytes2K")
    @Mapping(target = "estimatePngBytes4K", source = "estimate.pngBytes4K")
    @Mapping(target = "estimateJpgBytes1K", source = "estimate.jpgBytes1K")
    @Mapping(target = "estimateJpgBytes2K", source = "estimate.jpgBytes2K")
    @Mapping(target = "estimateJpgBytes4K", source = "estimate.jpgBytes4K")
    fun toResponse(cfg: PricingConfig): EstimateParamsResponse
}

