package coralsum.service

import coralsum.common.response.ProfileResponse

interface IWebUserService {
    suspend fun profile(uid: String): ProfileResponse?
}