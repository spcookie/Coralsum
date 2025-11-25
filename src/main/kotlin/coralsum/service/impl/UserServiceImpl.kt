package coralsum.service.impl

import cn.hutool.core.util.IdUtil
import coralsum.common.enums.UserSource
import coralsum.entity.OpenUser
import coralsum.entity.OutletUser
import coralsum.repository.OpenUserRepository
import coralsum.repository.OutletUserRepository
import coralsum.service.IUserService
import jakarta.inject.Singleton

@Singleton
class UserServiceImpl(
    val outletUserRepository: OutletUserRepository,
    val openUserRepository: OpenUserRepository,
) : IUserService {
    override suspend fun userSubscribe(userSource: UserSource, sourceCode: String) {
        val outletUser = outletUserRepository.findBySourceCodeAndUserSource(
            sourceCode,
            UserSource.WECHAT
        )
        if (outletUser == null) {
            val openUser = OpenUser(
                uid = IdUtil.getSnowflakeNextIdStr(),
                assignRole = "FREE"
            )
            val savedOpenUser = openUserRepository.save(openUser)
            outletUserRepository.save(
                OutletUser(
                    openUser = savedOpenUser,
                    userSource = UserSource.WECHAT,
                    sourceCode = sourceCode
                )
            )
        }
    }
}