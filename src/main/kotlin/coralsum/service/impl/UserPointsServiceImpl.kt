package coralsum.service.impl

import coralsum.common.enums.ImageSize
import coralsum.common.enums.MembershipTier
import coralsum.common.enums.SubscribeType
import coralsum.common.event.GenerativeImageCostEvent
import coralsum.common.event.UserRegisteredEvent
import coralsum.common.request.EstimatePointsReq
import coralsum.config.PricingConfig
import coralsum.infrastructure.entity.OpenUser
import coralsum.infrastructure.entity.UserPoints
import coralsum.infrastructure.entity.UserPointsDeduction
import coralsum.infrastructure.repository.GenerateImageReqRefRepository
import coralsum.infrastructure.repository.OpenUserRepository
import coralsum.infrastructure.repository.UserPointsDeductionRepository
import coralsum.infrastructure.repository.UserPointsRepository
import coralsum.service.IPointsEstimateService
import coralsum.service.IUserPointsService
import coralsum.toolkit.logger
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Singleton
class UserPointsServiceImpl(
    private val userPointsRepository: UserPointsRepository,
    private val openUserRepository: OpenUserRepository,
    private val pricingConfig: PricingConfig,
    private val generateImageReqRefRepository: GenerateImageReqRefRepository,
    private val userPointsDeductionRepository: UserPointsDeductionRepository,
    private val pointsEstimateService: IPointsEstimateService,
) : IUserPointsService {

    companion object {
        private val log = logger<UserPointsServiceImpl>()
        private const val COST_SCALE = 18
        private val COST_ROUND = RoundingMode.HALF_UP
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

    override suspend fun addGiftPoints(
        openUserId: Long,
        delta: Int,
        periodUnit: String?,
        periodCount: Int?
    ): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.giftPoints = points.giftPoints.add(delta.toBigDecimal()).max(BigDecimal.ZERO)
        val now = LocalDateTime.now()
        val base = points.giftExpireTime?.let { if (it.isAfter(now)) it else now } ?: now
        val unit = periodUnit?.uppercase()
        val cnt = (periodCount ?: 0).coerceAtLeast(0)
        points.giftExpireTime = when {
            unit == null || cnt <= 0 -> base.plusDays(30)
            unit == "DAY" -> base.plusDays(cnt.toLong())
            unit == "MONTH" -> base.plusMonths(cnt.toLong())
            unit == "YEAR" -> base.plusYears(cnt.toLong())
            else -> base.plusDays(30)
        }
        return userPointsRepository.update(points)
    }

    override suspend fun hasEnoughPoints(openUserId: Long): Boolean {
        val userPoints = getOrCreateByOpenUserId(openUserId)
        val now = LocalDateTime.now()
        val giftActive = userPoints.giftExpireTime?.isAfter(now) == true
        val gift = if (giftActive) userPoints.giftPoints else BigDecimal.ZERO
        return userPoints.permanentPoints + userPoints.subscribePoints + gift > BigDecimal.ZERO
    }

    override suspend fun hasEnoughPoints(openUserId: Long, estimate: EstimatePointsReq): Boolean {
        val points = getOrCreateByOpenUserId(openUserId)
        val now = LocalDateTime.now()
        val giftActive = points.giftExpireTime?.isAfter(now) == true
        val giftAvail = if (giftActive) points.giftPoints else BigDecimal.ZERO
        val subscribeActive = points.subscribeExpireTime?.isAfter(now) == true
        val subscribeAvail = if (subscribeActive) points.subscribePoints else BigDecimal.ZERO
        val available = points.permanentPoints + subscribeAvail + giftAvail
        val est = pointsEstimateService.estimate(estimate)
        val remaining = available.multiply(pricingConfig.pointsPerRmb.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
        return remaining >= est.pointsToDeduct.toBigDecimal()

    }

    override suspend fun reconcileTier(openUserId: Long): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        val now = LocalDateTime.now()
        val active = points.subscribeExpireTime?.isAfter(now) == true
        val giftActive = points.giftExpireTime?.isAfter(now) == true
        val giftVal = if (giftActive) points.giftPoints else BigDecimal.ZERO
        val giftOnly =
            (giftVal > BigDecimal.ZERO) && (points.permanentPoints + points.subscribePoints == BigDecimal.ZERO)
        val nonGiftHas = (points.permanentPoints + points.subscribePoints) > BigDecimal.ZERO
        val newTier = when {
            active -> MembershipTier.PRO
            giftOnly || (!nonGiftHas) -> MembershipTier.FREE
            else -> MembershipTier.PLUS
        }
        if (newTier != points.tier) {
            points.tier = newTier
            return userPointsRepository.update(points)
        }
        return points
    }

    fun BigDecimal.divSafe(divisor: BigDecimal): BigDecimal = divide(divisor, COST_SCALE, COST_ROUND)

    @EventListener
    fun onGenerativeImageCostEvent(event: GenerativeImageCostEvent) {
        runBlocking {
            try {
                val openUser = openUserRepository.findByUid(event.uid) ?: return@runBlocking
                val userPoints = getOrCreateByOpenUserId(openUser.id!!)

                val pricing = pricingConfig

                val usdToCny = pricing.usdToCny.toBigDecimal()
                val coef = pricing.coefficient.toBigDecimal()

                // ========= 评估阶段 =========

                val evalInputChars = event.inputCharCount.toBigDecimal()
                val evalTokensPerChar = pricing.flashLite.tokensPerChar.toBigDecimal()

                val evalInputTokens = evalInputChars.divSafe(evalTokensPerChar)
                val evalOutputTokens = evalInputTokens.multiply(BigDecimal("2"))

                val evalInputUsd = evalInputTokens
                    .divSafe(BigDecimal("1000000"))
                    .multiply(pricing.flashLite.inputUsdPerMTokens.toBigDecimal())

                val evalOutputUsd = evalOutputTokens
                    .divSafe(BigDecimal("1000000"))
                    .multiply(pricing.flashLite.outputUsdPerMTokens.toBigDecimal())

                val evalRmb = (evalInputUsd + evalOutputUsd).multiply(usdToCny)

                // ========= 生成阶段 =========

                val previewInputTokens = event.inputTokens.toBigDecimal()

                val inputUsdPerMTokens = if (event.modelType.name == "BASIC")
                    pricing.basic.inputUsdPerMTokens.toBigDecimal()
                else
                    pricing.pro.inputUsdPerMTokens.toBigDecimal()

                val previewInputUsd = previewInputTokens
                    .divSafe(BigDecimal("1000000"))
                    .multiply(inputUsdPerMTokens)

                val previewRmbTokens = previewInputUsd.multiply(usdToCny)

                // ========= 输出阶段 =========

                val thoughtsTokens = event.outputTokens.toBigDecimal()

                val thoughtsUsdPerMTokens = if (event.modelType.name != "BASIC") {
                    pricing.pro.thoughtsUsdPerMTokens.toBigDecimal()
                } else {
                    BigDecimal.ZERO
                }

                val thoughtsUsd = thoughtsUsdPerMTokens
                    .divSafe(BigDecimal("1000000"))
                    .multiply(thoughtsTokens)

                val thoughtsRmb = thoughtsUsd.multiply(usdToCny)

                val outputUsdPerMTokens = if (event.modelType.name == "BASIC")
                    pricing.basic.outputUsdPerMTokens.toBigDecimal()
                else
                    pricing.pro.outputUsdPerMTokens.toBigDecimal()

                val outTokens = event.outputTokens.toBigDecimal()

                val outUsd = outTokens
                    .divSafe(BigDecimal("1000000"))
                    .multiply(outputUsdPerMTokens)

                val imageRmb = outUsd.multiply(usdToCny)

                // ========= 图片估算 =========

                val estimateOutUsd = if (event.modelType.name == "BASIC") {
                    pricing.basic.outputPricePerImage1kUsd
                } else {
                    when (event.imageSizeCategory) {
                        ImageSize.X1, ImageSize.X2 -> pricing.pro.pricePerImage1k2kUsd
                        else -> pricing.pro.pricePerImage4kUsd
                    }
                }.toBigDecimal()

                val estimateImageRmb = estimateOutUsd.multiply(usdToCny)

                // ========= 流量成本 =========

                val bytes = event.imageSizeBytes.toBigDecimal()

                val gb = bytes
                    .divSafe(BigDecimal("1024"))
                    .divSafe(BigDecimal("1024"))
                    .divSafe(BigDecimal("1024"))

                val hour = Instant.ofEpochMilli(event.timestampMs)
                    .atZone(ZoneId.systemDefault())
                    .hour

                val busy = hour in pricing.oss.busyStartHour until pricing.oss.busyEndHour

                val ossRate = (if (busy)
                    pricing.oss.busyRmbPerGb
                else
                    pricing.oss.idleRmbPerGb).toBigDecimal()

                val trafficMultiplier = pricing.traffic.visitMultiplier.toBigDecimal()

                val ossRmb = gb.multiply(ossRate).multiply(trafficMultiplier)
                val natRmb = gb.multiply(pricing.traffic.natRmbPerGb.toBigDecimal()).multiply(trafficMultiplier)
                val proxyRmb = gb.multiply(pricing.traffic.proxyRmbPerGb.toBigDecimal()).multiply(trafficMultiplier)

                // ========= 总成本 =========

                val baseCostRmb = if (event.success) {
                    evalRmb + previewRmbTokens + thoughtsRmb + imageRmb + ossRmb + natRmb + proxyRmb
                } else {
                    evalRmb + previewRmbTokens + estimateImageRmb
                }

                val upExtra =
                    if (event.success && pricing.upscayl.enabled && pricing.upscayl.chargeByScale && event.upscaylScale > 1)
                        baseCostRmb.multiply((event.upscaylScale - 1).toBigDecimal())
                    else BigDecimal.ZERO

                val totalCostRmb = baseCostRmb + upExtra

                val pointsToDeduct = totalCostRmb
                    .multiply(coef)
                    .setScale(2, RoundingMode.HALF_UP)

                // ========= 扣减逻辑 =========

                var remaining = pointsToDeduct
                val giftActive = userPoints.giftExpireTime?.isAfter(LocalDateTime.now()) == true
                val giftAvail = if (giftActive) userPoints.giftPoints else BigDecimal.ZERO
                if (giftAvail >= remaining) {
                    userPoints.giftPoints -= remaining
                } else {
                    remaining -= giftAvail
                    if (giftActive) userPoints.giftPoints = BigDecimal.ZERO
                    if (userPoints.subscribePoints >= remaining) {
                        userPoints.subscribePoints -= remaining
                    } else {
                        remaining -= userPoints.subscribePoints
                        userPoints.subscribePoints = BigDecimal.ZERO
                        userPoints.permanentPoints -= remaining
                    }
                }

                userPointsRepository.update(userPoints)
                reconcileTier(openUser.id)

                // ========= 记录 =========
                val costDetail = """
                        {
                          "eval_rmb": $evalRmb,
                          "preview_rmb_tokens": $previewRmbTokens,
                          "image_rmb": $imageRmb,
                          "oss_rmb": $ossRmb,
                          "nat_rmb": $natRmb,
                          "proxy_rmb": $proxyRmb,
                          "upscayl_extra_rmb": $upExtra,
                          "upscayl_scale": ${event.upscaylScale},
                          "total_cost_rmb": $totalCostRmb,
                          "points_to_deduct": $pointsToDeduct
                        }
                        """.trimIndent()

                val refs = if (event.recordId != null)
                    generateImageReqRefRepository.findAllByRecordId(event.recordId)
                else emptyList()

                if (refs.isEmpty()) {
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

    @EventListener
    fun onUserRegistered(event: UserRegisteredEvent) {
        runBlocking {
            try {
                val points = getOrCreateByOpenUserId(event.openUserId)
                points.tier = MembershipTier.FREE
                points.giftPoints = points.giftPoints.add(BigDecimal(0.5)).max(BigDecimal.ZERO)
                points.giftExpireTime = LocalDateTime.now().plusDays(30)
                userPointsRepository.update(points)
            } catch (e: Exception) {
                log.error("Failed to grant initial points", e)
            }
        }
    }


}
