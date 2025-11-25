package coralsum.service.impl

import coralsum.common.enums.UserSource
import coralsum.common.response.ProfileResponse
import coralsum.repository.OpenUserRepository
import coralsum.repository.OutletUserRepository
import coralsum.repository.UserPointsRepository
import coralsum.service.IWebUserService
import jakarta.inject.Singleton

@Singleton
class WebUserServiceImpl(
    private val openUserRepository: OpenUserRepository,
    private val outletUserRepository: OutletUserRepository,
    private val userPointsRepository: UserPointsRepository,
) : IWebUserService {
    override suspend fun profile(uid: String): ProfileResponse? {
        val openUser = openUserRepository.findByUid(uid) ?: return null
        val outletWeb = outletUserRepository.findByOpenUserIdAndUserSource(openUser.id!!, UserSource.WEB)
        val userPoints = userPointsRepository.findByOpenUserId(openUser.id)
        return ProfileResponse(
            nickName = outletWeb?.nickName ?: "",
            sourceCode = outletWeb?.sourceCode ?: "",
            permanentPoints = userPoints?.permanentPoints ?: 0,
            subscribePoints = userPoints?.subscribePoints ?: 0,
            tier = userPoints?.tier ?: coralsum.common.enums.MembershipTier.FREE
        )
    }
}