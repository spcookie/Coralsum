package coralsum.service.impl

import cn.hutool.core.util.RandomUtil
import com.fasterxml.jackson.databind.ObjectMapper
import coralsum.common.dto.PageResp
import coralsum.common.enums.MembershipTier
import coralsum.common.enums.SubscribeType
import coralsum.common.request.*
import coralsum.component.excption.BusinessException
import coralsum.infrastructure.entity.PointsKey
import coralsum.infrastructure.entity.PointsKeyConfig
import coralsum.infrastructure.repository.OpenUserRepository
import coralsum.infrastructure.repository.PointsKeyConfigRepository
import coralsum.infrastructure.repository.PointsKeyRepository
import coralsum.service.IPointsKeyService
import coralsum.service.IUserPointsService
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Singleton
class PointsKeyServiceImpl(
    private val pointsKeyConfigRepository: PointsKeyConfigRepository,
    private val pointsKeyRepository: PointsKeyRepository,
    private val openUserRepository: OpenUserRepository,
    private val userPointsService: IUserPointsService,
    private val objectMapper: ObjectMapper,
) : IPointsKeyService {

    override suspend fun createConfig(req: CreatePointsKeyConfigReq, operator: String?): PointsKeyConfig {
        val config = PointsKeyConfig(
            name = req.name,
            permanentPoints = req.permanentPoints,
            subscribePoints = req.subscribePoints,
            subscribeType = req.subscribeType,
            periodUnit = req.periodUnit,
            periodCount = req.periodCount,
            disabled = false,
        )
        return pointsKeyConfigRepository.save(config)
    }

    override suspend fun listConfigs(): List<PointsKeyConfig> {
        return pointsKeyConfigRepository.findAll().toList()
    }

    override suspend fun generateKeys(req: GeneratePointsKeysReq, operator: String?): List<PointsKey> {
        val cfg = pointsKeyConfigRepository.findById(req.configId)
            ?: throw BusinessException("配置不存在")
        if (cfg.disabled) {
            throw BusinessException("配置已禁用，无法生成密钥")
        }
        val cfgJson = objectMapper.writeValueAsString(cfg)
        val result = mutableListOf<PointsKey>()
        val count = req.count.coerceAtMost(1000)
        repeat(count) {
            val keyStr = RandomUtil.randomString(32)
            val entity = PointsKey(
                keyCode = keyStr,
                configId = cfg.id!!,
                configJson = cfgJson,
                enabled = true,
            )
            result += pointsKeyRepository.save(entity)
        }
        return result
    }

    override suspend fun listKeys(): List<PointsKey> {
        return pointsKeyRepository.findAll().toList()
    }

    override suspend fun listConfigsPaged(
        page: Int,
        size: Int,
        name: String?,
        sortBy: String?,
        order: String?,
    ): PageResp<PointsKeyConfig> {
        val p = if (page > 0) page else 1
        val s = if (size > 0) size else 10
        val sortProp = when (sortBy?.lowercase()) {
            "status", "disabled" -> "disabled"
            else -> "id"
        }
        val dir = if ((order ?: "desc").equals("asc", true)) Sort.Order.Direction.ASC else Sort.Order.Direction.DESC
        val pageable = Pageable.from(p - 1, s, Sort.of(Sort.Order(sortProp, dir, false)))
        val pageData = if (!name.isNullOrBlank()) {
            pointsKeyConfigRepository.findByNameContains(name.trim(), pageable)
        } else {
            pointsKeyConfigRepository.findAll(pageable)
        }
        return PageResp(items = pageData.content, page = p, size = s, total = pageData.totalSize)
    }

    override suspend fun listKeysPaged(
        page: Int,
        size: Int,
        key: String?,
        sortBy: String?,
        order: String?,
    ): PageResp<PointsKey> {
        val p = if (page > 0) page else 1
        val s = if (size > 0) size else 10
        val dir = if ((order ?: "desc").equals("asc", true)) Sort.Order.Direction.ASC else Sort.Order.Direction.DESC
        val sp = when (sortBy?.lowercase()) {
            "status" -> null
            "used", "usedtime" -> "usedTime"
            "enabled" -> "enabled"
            else -> "usedTime"
        }
        val pageable = if (sp == null) {
            Pageable.from(
                p - 1, s, Sort.of(
                    Sort.Order("used", dir, false),
                    Sort.Order("enabled", dir, false)
                )
            )
        } else {
            Pageable.from(p - 1, s, Sort.of(Sort.Order(sp, dir, false)))
        }
        val pageData = if (!key.isNullOrBlank()) {
            pointsKeyRepository.findByKeyCodeContains(key.trim(), pageable)
        } else {
            pointsKeyRepository.findAll(pageable)
        }
        return PageResp(items = pageData.content, page = p, size = s, total = pageData.totalSize)
    }

    override suspend fun toggleKeys(req: ToggleKeysReq) {
        if (req.ids.isEmpty()) return
        pointsKeyRepository.batchToggleEnabled(req.ids, req.enabled)
    }

    override suspend fun toggleConfig(req: ToggleConfigReq): PointsKeyConfig {
        val cur = pointsKeyConfigRepository.findById(req.id)
            ?: throw BusinessException("配置不存在")
        pointsKeyConfigRepository.updateDisabled(req.id, req.disabled)
        return pointsKeyConfigRepository.findById(req.id) ?: cur.copy(disabled = req.disabled)
    }

    @Transactional(rollbackFor = [Exception::class])
    override suspend fun redeem(uid: String, ip: String?, keyCode: String): RedeemPointsResp {
        val pk = pointsKeyRepository.findByKeyCode(keyCode) ?: throw IllegalArgumentException("密钥不存在")
        if (!pk.enabled) throw BusinessException("密钥已被禁用")
        if (pk.used) throw BusinessException("密钥已被使用")
        val now = LocalDateTime.now()

        val cfg = pointsKeyConfigRepository.findById(pk.configId)
            ?: throw BusinessException("密钥不存在")

        if (cfg.disabled) {
            throw BusinessException("密钥已禁用")
        }

        val openUser = openUserRepository.findByUid(uid) ?: throw IllegalStateException("用户不存在")
        val openUserId = openUser.id ?: throw BusinessException("用户ID无效")

        val permAdd = cfg.permanentPoints
        val subsAdd = cfg.subscribePoints
        // 累加积分
        if (permAdd > BigDecimal.ZERO) {
            userPointsService.addPermanentPoints(
                openUserId,
                permAdd.setScale(2, RoundingMode.HALF_UP).toInt()
            )
        }
        if (subsAdd > BigDecimal.ZERO) {
            userPointsService.addSubscribePoints(
                openUserId,
                subsAdd.setScale(2, RoundingMode.HALF_UP).toInt()
            )
        }

        // 订阅类型与时长处理
        if (cfg.periodCount > 0 || !cfg.subscribeType.isNullOrBlank()) {
            val type = cfg.subscribeType?.let { SubscribeType.of(it) }
            userPointsService.updateSubscribeType(openUserId, type)
        }

        // 更新会员等级（FREE → PRO/PLUS）
        run {
            val up = userPointsService.getOrCreateByOpenUserId(openUserId)
            val newTier = when {
                cfg.periodCount > 0 -> MembershipTier.PRO
                cfg.periodCount <= 0 && cfg.permanentPoints > BigDecimal.ZERO -> MembershipTier.PLUS
                else -> up.tier
            }
            if (newTier != up.tier) {
                up.tier = newTier
            }
            // 订阅过期时间与时长累加
            if (cfg.periodCount > 0 && !cfg.periodUnit.isNullOrBlank()) {
                val base = up.subscribeExpireTime?.let { if (it.isAfter(now)) it else now } ?: now
                up.subscribeExpireTime = when (cfg.periodUnit.uppercase()) {
                    "DAY" -> base.plusDays(cfg.periodCount.toLong())
                    "MONTH" -> base.plusMonths(cfg.periodCount.toLong())
                    "YEAR" -> base.plusYears(cfg.periodCount.toLong())
                    else -> base
                }
            }
            userPointsService.getOrCreateByOpenUserId(openUserId)
        }

        // 标记密钥使用
        val usedEntity = pk.copy(
            used = true,
            usedTime = now,
            usedIp = ip,
            usedUid = uid,
        )
        pointsKeyRepository.update(usedEntity)

        // 计算返回值（两位小数积分按 *100 整数视图）
        val added = permAdd.add(subsAdd).setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
        val cur = run {
            val up = userPointsService.getOrCreateByOpenUserId(openUserId)
            val total = up.permanentPoints.add(up.subscribePoints)
            total.setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
        }
        return RedeemPointsResp(pointsAdded = added, points = cur)
    }
}
