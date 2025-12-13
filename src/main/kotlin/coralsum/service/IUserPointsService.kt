package coralsum.service

import coralsum.common.enums.SubscribeType
import coralsum.infrastructure.entity.UserPoints

interface IUserPointsService {
    suspend fun getOrCreateByOpenUserId(openUserId: Long): UserPoints

    suspend fun addPermanentPoints(openUserId: Long, delta: Int): UserPoints

    suspend fun addSubscribePoints(openUserId: Long, delta: Int): UserPoints

    suspend fun updateSubscribeType(openUserId: Long, type: SubscribeType?): UserPoints

    suspend fun addGiftPoints(
        openUserId: Long,
        delta: Int,
        periodUnit: String? = null,
        periodCount: Int? = null
    ): UserPoints

    suspend fun hasEnoughPoints(openUserId: Long): Boolean

    suspend fun hasEnoughPoints(openUserId: Long, estimate: coralsum.common.request.EstimatePointsReq): Boolean

    suspend fun reconcileTier(openUserId: Long): UserPoints
}
