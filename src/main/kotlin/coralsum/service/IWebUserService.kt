package coralsum.service

import coralsum.common.response.ProfileResponse

interface IWebUserService {
    suspend fun profile(uid: String): ProfileResponse?
    suspend fun updateNickName(uid: String, nickName: String): Boolean
}