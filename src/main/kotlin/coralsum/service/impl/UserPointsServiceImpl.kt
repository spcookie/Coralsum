package coralsum.service.impl

import coralsum.common.enums.SubscribeStatus
import coralsum.common.enums.SubscribeType
import coralsum.common.event.GenerativeImageCostEvent
import coralsum.entity.OpenUser
import coralsum.entity.UserPoints
import coralsum.repository.OpenUserRepository
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
) : IUserPointsService {

    companion object {
        private val log = logger<UserPointsServiceImpl>()

        private val INPUT_TOKEN_UNIT = 1.25.toBigDecimal() / 1_000_000.toBigDecimal()
        private val OUTPUT_TOKEN_UNIT = 10.toBigDecimal() / 1_000_000.toBigDecimal()
        private val SCALE_UNIT = 0.02.toBigDecimal()
        private val SIZE_UNIT = 0.005.toBigDecimal()
        private val EXCHANGE_RATE = 7.08.toBigDecimal()
        private val COEFFICIENT = 2.toBigDecimal()
    }

    @Transactional
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

    override suspend fun updateSubscribeStatus(openUserId: Long, status: SubscribeStatus): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.subscribeStatus = status
        if (status == SubscribeStatus.EXPIRED) {
            points.subscribeExpireTime = LocalDateTime.now()
        }
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

    @EventListener
    fun onGenerativeImageCostEvent(event: GenerativeImageCostEvent) {
        val cost = COEFFICIENT * EXCHANGE_RATE *
                (event.inputTokens.toBigDecimal() * INPUT_TOKEN_UNIT
                        + event.outputTokens.toBigDecimal() * OUTPUT_TOKEN_UNIT
                        + SCALE_UNIT * (event.scale - 1).toBigDecimal()
                        + SIZE_UNIT * event.imageSize.toBigDecimal() / 1024.toBigDecimal() / 1024.toBigDecimal())
        runBlocking {
            try {
                val openUser = openUserRepository.findByUid(event.uid)!!
                val userPoints = getOrCreateByOpenUserId(openUser.id!!)

                if (userPoints.subscribePoints >= cost) {
                    userPoints.subscribePoints = userPoints.subscribePoints - cost
                    userPointsRepository.update(userPoints)
                } else {
                    userPoints.subscribePoints = 0.toBigDecimal()
                    val remaining = cost - userPoints.subscribePoints
                    userPoints.permanentPoints = userPoints.permanentPoints - remaining
                    userPointsRepository.update(userPoints)
                }
            } catch (e: Exception) {
                log.error("Failed to update user points", e)
            }
        }
    }

}
