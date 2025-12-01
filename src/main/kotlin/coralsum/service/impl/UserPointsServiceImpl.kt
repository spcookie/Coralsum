package coralsum.service.impl

import coralsum.common.enums.ImageSize
import coralsum.common.enums.MembershipTier
import coralsum.common.enums.SubscribeType
import coralsum.common.event.GenerativeImageCostEvent
import coralsum.config.PricingConfig
import coralsum.entity.OpenUser
import coralsum.entity.UserPoints
import coralsum.entity.UserPointsDeduction
import coralsum.repository.GenerateImageReqRecordRepository
import coralsum.repository.GenerateImageReqRefRepository
import coralsum.repository.OpenUserRepository
import coralsum.repository.UserPointsDeductionRepository
import coralsum.repository.UserPointsRepository
import coralsum.service.IUserPointsService
import coralsum.toolkit.logger
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.LocalDateTime

@Singleton
class UserPointsServiceImpl(
    private val userPointsRepository: UserPointsRepository,
    private val openUserRepository: OpenUserRepository,
    private val pricingConfig: PricingConfig,
    private val generateImageReqRefRepository: GenerateImageReqRefRepository,
    private val userPointsDeductionRepository: UserPointsDeductionRepository,
) : IUserPointsService {

    companion object {
        private val log = logger<UserPointsServiceImpl>()
    }

    @Transactional(rollbackFor = [Exception::class])
    override suspend fun getOrCreateByOpenUserId(openUserId: Long): UserPoints {
        val existed = userPointsRepository.findByOpenUserId(openUserId)
        if (existed != null) return existed
        return userPointsRepository.save(
            UserPoints(
                openUser = OpenUser(id = openUserId)
            )
        )
    }

    override suspend fun addPermanentPoints(openUserId: Long, delta: Int): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.permanentPoints = points.permanentPoints.add(delta.toBigDecimal()).max(BigDecimal.ZERO)
        return userPointsRepository.save(points)
    }

    override suspend fun addSubscribePoints(openUserId: Long, delta: Int): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.subscribePoints = points.subscribePoints.add(delta.toBigDecimal()).max(BigDecimal.ZERO)
        return userPointsRepository.save(points)
    }

    override suspend fun updateSubscribeType(openUserId: Long, type: SubscribeType?): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.subscribeType = type
        return userPointsRepository.save(points)
    }

    override suspend fun hasEnoughPoints(openUserId: Long): Boolean {
        val userPoints = getOrCreateByOpenUserId(openUserId)
        return userPoints.permanentPoints + userPoints.subscribePoints > BigDecimal.ZERO
    }

    override suspend fun reconcileTier(openUserId: Long): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        val now = LocalDateTime.now()
        val active = points.subscribeExpireTime?.isAfter(now) == true
        val newTier = when {
            active -> MembershipTier.PRO
            !active && points.permanentPoints > BigDecimal.ZERO -> MembershipTier.PLUS
            else -> MembershipTier.FREE
        }
        if (newTier != points.tier) {
            points.tier = newTier
            return userPointsRepository.save(points)
        }
        return points
    }

    @EventListener
    fun onGenerativeImageCostEvent(event: GenerativeImageCostEvent) {
        // 费用计算说明（参考 price.md）：
        // - 评估阶段（Gemini 2.5 Flash-Lite）：输入字符→令牌（2.5/token/char），输出令牌≈输入字符×2×2.5；
        //   输入按 0.10 USD/百万令牌，输出按 0.40 USD/百万令牌，最终按汇率转人民币。
        // - 生成阶段（gemini-3-pro-image-preview）：输入/输出令牌按 2.00/12.00 USD/百万令牌；
        //   图片单价：1K/2K 为 0.134 USD/张，4K 为 0.24 USD/张。
        // - 流量费用：OSS 按时段（闲时 0.25 RMB/GB，忙时 0.50 RMB/GB），内网穿透 1.1 RMB/GB，代理 1.6 RMB/GB。
        // - 积分扣减：总成本 × 系数（默认 1.5）。成功生成时计入全部成本；失败仅计模型调用（评估+生成令牌）。
        runBlocking {
            try {
                val openUser = openUserRepository.findByUid(event.uid) ?: return@runBlocking
                val userPoints = getOrCreateByOpenUserId(openUser.id!!)

                val pricing = pricingConfig
                val usdToCny = pricing.usdToCny.toBigDecimal() // 汇率 USD→CNY
                val coef = pricing.coefficient.toBigDecimal() // 成本抽成系数（积分换算系数）

                // 评估阶段（Flash-Lite）按字符估算令牌与美元成本
                val evalInputChars = event.inputCharCount.toBigDecimal() // 输入字符数
                val evalTokensPerChar = pricing.flashLite.tokensPerChar.toBigDecimal() // 每字符令牌数（2.5）
                val evalInputTokens = evalInputChars * evalTokensPerChar // 输入令牌
                val evalOutputTokens = evalInputChars * (2.toBigDecimal()) * evalTokensPerChar // 输出令牌≈输入×2
                val evalInputUsd = evalInputTokens / 1_000_000.toBigDecimal() * pricing.flashLite.inputUsdPerMTokens.toBigDecimal() // 输入美元成本
                val evalOutputUsd = evalOutputTokens / 1_000_000.toBigDecimal() * pricing.flashLite.outputUsdPerMTokens.toBigDecimal() // 输出美元成本
                val evalRmb = (evalInputUsd + evalOutputUsd) * usdToCny // 评估人民币成本

                // 生成阶段（Image Preview）令牌成本（美元→人民币）
                val previewInputTokens = event.inputTokens.toBigDecimal()
                val previewInputUsd = previewInputTokens / 1_000_000.toBigDecimal() * pricing.imagePreview.inputUsdPerMTokens.toBigDecimal()
                val previewRmbTokens = previewInputUsd * usdToCny

                // 图片单价（按分辨率）
                val perImageUsd = when (event.imageSizeCategory) {
                    ImageSize.X4 -> pricing.imagePreview.pricePerImage4KUsd.toBigDecimal()
                    ImageSize.X1, ImageSize.X2 -> pricing.imagePreview.pricePerImage1K2KUsd.toBigDecimal()
                }
                val imageCount = event.imageCount.toBigDecimal() // 生成图片数量
                val imageUsd = perImageUsd * imageCount // 图片美元成本
                val imageRmb = imageUsd * usdToCny // 图片人民币成本

                // 流量成本（GB 为单位）：OSS（按时段），内网穿透与代理
                val bytes = event.imageSizeBytes.toBigDecimal() // 总字节大小（所有图片）
                val gb = bytes / 1024.toBigDecimal() / 1024.toBigDecimal() / 1024.toBigDecimal() // 字节→GB
                val hour = java.time.Instant.ofEpochMilli(event.timestampMs).atZone(java.time.ZoneId.systemDefault()).hour
                val busy = hour >= pricing.oss.busyStartHour && hour < pricing.oss.busyEndHour // 忙时判定
                val ossRate = (if (busy) pricing.oss.busyRmbPerGb else pricing.oss.idleRmbPerGb).toBigDecimal() // OSS 单价
                val trafficMultiplier = pricing.traffic.visitMultiplier.toBigDecimal()
                val ossRmb = gb * ossRate * trafficMultiplier
                val natRmb = gb * pricing.traffic.natRmbPerGb.toBigDecimal() * trafficMultiplier
                val proxyRmb = gb * pricing.traffic.proxyRmbPerGb.toBigDecimal() * trafficMultiplier

                // 成功：全部成本；失败：仅模型调用（评估+生成令牌）
                val totalCostRmb = if (event.success) evalRmb + previewRmbTokens + imageRmb + ossRmb + natRmb + proxyRmb else evalRmb + previewRmbTokens + imageRmb
                val pointsToDeduct = (totalCostRmb * coef).setScale(4, java.math.RoundingMode.HALF_UP) // 积分扣减

                // 扣减优先使用订阅积分，不足部分再扣永久积分
                var remaining = pointsToDeduct
                if (userPoints.subscribePoints >= remaining) {
                    userPoints.subscribePoints = userPoints.subscribePoints - remaining
                } else {
                    val sub = userPoints.subscribePoints
                    remaining = remaining - sub
                    userPoints.subscribePoints = BigDecimal.ZERO
                    userPoints.permanentPoints = userPoints.permanentPoints - remaining
                }
                userPointsRepository.update(userPoints)

                // 扣减后根据到期时间与剩余积分修正会员等级
                reconcileTier(openUser.id!!)

                // 记录成本明细（JSON），用于审计与对账
                val costDetail = """{
                    "eval_rmb": ${evalRmb},
                    "preview_rmb_tokens": ${previewRmbTokens},
                    "image_rmb": ${imageRmb},
                    "oss_rmb": ${ossRmb},
                    "nat_rmb": ${natRmb},
                    "proxy_rmb": ${proxyRmb},
                    "total_cost_rmb": ${totalCostRmb},
                    "points_to_deduct": ${pointsToDeduct}
                }"""
                val refs = if (event.recordId != null) generateImageReqRefRepository.findAllByRecordId(event.recordId) else emptyList()
                if (refs.isEmpty()) {
                    // 失败或无图片场景：写入一条无 image_ref 的扣减记录
                    userPointsDeductionRepository.save(
                        UserPointsDeduction(
                            uid = event.uid,
                            recordId = event.recordId,
                            imageRef = null,
                            success = event.success,
                            pointsDeducted = pointsToDeduct,
                            costDetail = costDetail
                        )
                    )
                } else {
                    // 成功场景：按图片引用逐条写入扣减记录
                    for (r in refs) {
                        userPointsDeductionRepository.save(
                            UserPointsDeduction(
                                uid = event.uid,
                                recordId = event.recordId,
                                imageRef = r.imageRef,
                                success = event.success,
                                pointsDeducted = pointsToDeduct,
                                costDetail = costDetail
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to update user points", e)
            }
        }
    }

}
