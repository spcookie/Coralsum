package coralsum.service.impl

import coralsum.common.enums.SubscribeStatus
import coralsum.common.enums.SubscribeType
import coralsum.entity.OpenUser
import coralsum.entity.UserPoints
import coralsum.repository.UserPointsRepository
import coralsum.service.IUserPointsService
import jakarta.inject.Singleton
import java.time.LocalDateTime

@Singleton
class UserPointsServiceImpl(
    private val userPointsRepository: UserPointsRepository,
) : IUserPointsService {
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
        points.permanentPoints = (points.permanentPoints + delta).coerceAtLeast(0)
        return userPointsRepository.save(points)
    }

    override suspend fun addSubscribePoints(openUserId: Long, delta: Int): UserPoints {
        val points = getOrCreateByOpenUserId(openUserId)
        points.subscribePoints = (points.subscribePoints + delta).coerceAtLeast(0)
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
}