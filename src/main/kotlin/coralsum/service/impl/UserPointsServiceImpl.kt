package coralsum.service.impl

import coralsum.common.enums.MembershipTier
import coralsum.common.enums.SubscribeType
import coralsum.common.event.GenerativeImageCostEvent
import coralsum.config.PricingConfig
import coralsum.entity.OpenUser
import coralsum.entity.UserPoints
import coralsum.entity.UserPointsDeduction
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
        return userPointsRepository.update(points)
    }

    override suspend fun addSubscribePoints(openUserId: Long, delta: Int): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.subscribePoints = points.subscribePoints.add(delta.toBigDecimal()).max(BigDecimal.ZERO)
        return userPointsRepository.update(points)
    }

    override suspend fun updateSubscribeType(openUserId: Long, type: SubscribeType?): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.subscribeType = type
        return userPointsRepository.update(points)
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
            return userPointsRepository.update(points)
        }
        return points
    }

    @EventListener
    fun onGenerativeImageCostEvent(event: GenerativeImageCostEvent) {
        runBlocking {
            try {
                val openUser = openUserRepository.findByUid(event.uid) ?: return@runBlocking
                val userPoints = getOrCreateByOpenUserId(openUser.id!!)

                val pricing = pricingConfig
                val usdToCny = pricing.usdToCny.toBigDecimal() // 汇率 USD→CNY
                val coef = pricing.coefficient.toBigDecimal() // 成本抽成系数（积分换算系数）

                // 评估阶段成本（按模型类型）
                val evalInputChars = event.inputCharCount.toBigDecimal()
                val evalTokensPerChar = pricing.flashLite.tokensPerChar.toBigDecimal()
                val evalInputTokens = evalInputChars * evalTokensPerChar
                val evalOutputTokens = evalInputChars * (2.toBigDecimal()) * evalTokensPerChar
                val evalInputUsd =
                    evalInputTokens / 1_000_000.toBigDecimal() * pricing.flashLite.inputUsdPerMTokens.toBigDecimal()
                val evalOutputUsd =
                    evalOutputTokens / 1_000_000.toBigDecimal() * pricing.flashLite.outputUsdPerMTokens.toBigDecimal()
                val evalRmb = (evalInputUsd + evalOutputUsd) * usdToCny

                // 生成阶段 输入 令牌成本（美元→人民币）
                val previewInputTokens = event.inputTokens.toBigDecimal()
                val inputUsdPerMTokens = if (event.modelType.name == "BASIC") {
                    pricing.basic.inputUsdPerMTokens.toBigDecimal()
                } else {
                    pricing.pro.inputUsdPerMTokens.toBigDecimal()
                }
                val previewInputUsd = previewInputTokens / 1_000_000.toBigDecimal() * inputUsdPerMTokens
                val previewRmbTokens = previewInputUsd * usdToCny

                // 图片输出令牌单价
                val outputUsdPerMTokens = if (event.modelType.name == "BASIC") {
                    pricing.basic.outputUsdPerMTokens.toBigDecimal()
                } else {
                    pricing.pro.outputUsdPerMTokens.toBigDecimal()
                }

                // 图片输出成本
                val outTokens = event.outputTokens.toBigDecimal()
                val outUsd = outTokens / 1_000_000.toBigDecimal() * outputUsdPerMTokens
                val imageRmb = outUsd * usdToCny

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
                val baseCostRmb =
                    if (event.success) evalRmb + previewRmbTokens + imageRmb + ossRmb + natRmb + proxyRmb else evalRmb + previewRmbTokens
                val upExtra =
                    if (event.success && pricing.upscayl.enabled && pricing.upscayl.chargeByScale && event.upscaylScale > 1) baseCostRmb * (event.upscaylScale - 1).toBigDecimal() else BigDecimal.ZERO
                val totalCostRmb = baseCostRmb + upExtra
                val pointsToDeduct = (totalCostRmb * coef * pricing.pointsPerRmb.toBigDecimal()).setScale(
                    0,
                    java.math.RoundingMode.HALF_UP
                ) // 积分扣减

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
                reconcileTier(openUser.id)

                // 记录成本明细（JSON），用于审计与对账
                val costDetail = """{
                    "eval_rmb": ${evalRmb},
                    "preview_rmb_tokens": ${previewRmbTokens},
                    "image_rmb": ${imageRmb},
                    "oss_rmb": ${ossRmb},
                    "nat_rmb": ${natRmb},
                    "proxy_rmb": ${proxyRmb},
                    "upscayl_extra_rmb": ${upExtra},
                    "upscayl_scale": ${event.upscaylScale},
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
