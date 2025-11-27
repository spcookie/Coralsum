package coralsum.service.impl

import coralsum.common.enums.MembershipTier
import coralsum.common.enums.UserSource
import coralsum.common.response.ProfileResponse
import coralsum.repository.OpenUserRepository
import coralsum.repository.OutletUserRepository
import coralsum.repository.UserPointsRepository
import coralsum.service.IWebUserService
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode

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
            permanentPoints = ((userPoints?.permanentPoints ?: BigDecimal.ZERO).setScale(
                2,
                RoundingMode.HALF_UP
            ) * 100.toBigDecimal()).toInt(),
            subscribePoints = ((userPoints?.subscribePoints ?: BigDecimal.ZERO).setScale(
                2,
                RoundingMode.HALF_UP
            ) * 100.toBigDecimal()).toInt(),
            tier = userPoints?.tier ?: MembershipTier.FREE
        )
    }

    override suspend fun updateNickName(uid: String, nickName: String): Boolean {
        val openUser = openUserRepository.findByUid(uid) ?: return false
        val outletWeb =
            outletUserRepository.findByOpenUserIdAndUserSource(openUser.id!!, UserSource.WEB) ?: return false
        val sanitized = nickName.trim()
        if (sanitized.isEmpty()) return false
        val updated = outletWeb.copy(nickName = sanitized)
        outletUserRepository.save(updated)
        return true
    }
}
