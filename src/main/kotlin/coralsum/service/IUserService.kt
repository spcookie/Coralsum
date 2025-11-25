package coralsum.service

import coralsum.common.enums.UserSource

interface IUserService {
    suspend fun userSubscribe(userSource: UserSource, sourceCode: String)
}