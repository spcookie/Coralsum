package coralsum.service.impl

import coralsum.common.enums.ImageSize
import coralsum.common.request.EstimatePointsReq
import coralsum.common.response.EstimatePointsResp
import coralsum.config.PricingConfig
import coralsum.service.IPointsEstimateService
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

@Singleton
class PointsEstimateServiceImpl(
    private val pricingConfig: PricingConfig,
) : IPointsEstimateService {

    companion object {
        private const val COST_SCALE = 18
        private val COST_ROUND = RoundingMode.HALF_UP
    }

    private fun BigDecimal.divSafe(divisor: BigDecimal): BigDecimal = divide(divisor, COST_SCALE, COST_ROUND)

    override suspend fun estimate(req: EstimatePointsReq): EstimatePointsResp {
        val pricing = pricingConfig
        val usdToCny = pricing.usdToCny.toBigDecimal()
        val coefficient = pricing.coefficient.toBigDecimal()
        val pointsPerRmb = pricing.pointsPerRmb.toBigDecimal()

        // 评估阶段：文本令牌按输入/输出百万令牌单价计费
        val chars = req.inputCharCount.toBigDecimal()
        val tokensPerChar = pricing.flashLite.tokensPerChar.toBigDecimal()
        val inputTextTokens = chars.multiply(tokensPerChar)
        val evalInputUsd = inputTextTokens
            .divSafe(BigDecimal("1000000"))
            .multiply(pricing.flashLite.inputUsdPerMTokens.toBigDecimal())
        val evalOutputUsd = inputTextTokens
            .multiply(BigDecimal("2"))
            .divSafe(BigDecimal("1000000"))
            .multiply(pricing.flashLite.outputUsdPerMTokens.toBigDecimal())
        val evalRmb = (evalInputUsd + evalOutputUsd).multiply(usdToCny)

        // 生成阶段：预览输入令牌 = 文本令牌 + 输入图片体积转换的令牌
        val mb = BigDecimal(req.inputPreviewBytes).divSafe(BigDecimal("1024")).divSafe(BigDecimal("1024"))
            .divSafe(BigDecimal("1024"))
        val imageTokens = mb.multiply(pricing.imagePreview.tokensPerMb.toBigDecimal())
        val previewInputTokens = inputTextTokens + imageTokens
        val inUsdPerM = if (req.modelType.name == "BASIC")
            pricing.basic.inputUsdPerMTokens.toBigDecimal()
        else
            pricing.pro.inputUsdPerMTokens.toBigDecimal()
        val previewUsd = previewInputTokens
            .divSafe(BigDecimal("1000000"))
            .multiply(inUsdPerM)
        val previewRmbTokens = previewUsd.multiply(usdToCny)

        // 图片单价（按分辨率与模型）
        val perImageUsd = if (req.modelType.name == "BASIC") {
            pricing.basic.outputPricePerImage1kUsd.toBigDecimal()
        } else {
            when (req.imageSizeCategory) {
                ImageSize.X1, ImageSize.X2 -> pricing.pro.pricePerImage1k2kUsd.toBigDecimal()
                else -> pricing.pro.pricePerImage4kUsd.toBigDecimal()
            }
        }
        val images = (if (req.candidateCount > 0) req.candidateCount else 1).toBigDecimal()
        val imageRmb = perImageUsd.multiply(usdToCny).multiply(images)

        // 流量评估：按分辨率与格式估算图片体积，乘访问倍数与候选数量
        val bytesPerImage = when (req.imageFormat.name) {
            "PNG" -> when (req.imageSizeCategory) {
                ImageSize.X1 -> pricing.estimate.pngBytes1K.toBigDecimal()
                ImageSize.X2 -> pricing.estimate.pngBytes2K.toBigDecimal()
                else -> pricing.estimate.pngBytes4K.toBigDecimal()
            }

            else -> when (req.imageSizeCategory) {
                ImageSize.X1 -> pricing.estimate.jpgBytes1K.toBigDecimal()
                ImageSize.X2 -> pricing.estimate.jpgBytes2K.toBigDecimal()
                else -> pricing.estimate.jpgBytes4K.toBigDecimal()
            }
        }
        val visitMultiplier = pricing.traffic.visitMultiplier.toBigDecimal()
        val totalBytes = bytesPerImage.multiply(images).multiply(visitMultiplier)
        val gb = totalBytes
            .divSafe(BigDecimal("1024"))
            .divSafe(BigDecimal("1024"))
            .divSafe(BigDecimal("1024"))
        val hour = Instant.ofEpochMilli(req.timestampMs).atZone(ZoneId.systemDefault()).hour
        val busy = hour in pricing.oss.busyStartHour until pricing.oss.busyEndHour
        val ossRate = (if (busy) pricing.oss.busyRmbPerGb else pricing.oss.idleRmbPerGb).toBigDecimal()
        val ossRmb = gb.multiply(ossRate)
        val natRmb = gb.multiply(pricing.traffic.natRmbPerGb.toBigDecimal())
        val proxyRmb = gb.multiply(pricing.traffic.proxyRmbPerGb.toBigDecimal())

        var total = evalRmb + previewRmbTokens + imageRmb + ossRmb + natRmb + proxyRmb
        val scale = req.upscaylScale
        if (pricing.upscayl.enabled && pricing.upscayl.chargeByScale && scale > 1) {
            total = total.multiply(scale.toBigDecimal())
        }

        val points = total
            .multiply(coefficient)
            .multiply(pointsPerRmb)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

        return EstimatePointsResp(
            evalRmb = evalRmb,
            previewRmbTokens = previewRmbTokens,
            imageRmb = imageRmb,
            ossRmb = ossRmb,
            natRmb = natRmb,
            proxyRmb = proxyRmb,
            totalCostRmb = total,
            pointsToDeduct = points,
        )
    }
}
