package coralsum.service

import coralsum.common.response.ProfileResponse

interface IWebUserService {
    suspend fun profile(uid: String): ProfileResponse?
    suspend fun updateNickName(uid: String, nickName: String): Boolean

    // 更新用户语言偏好
    suspend fun updateLanguage(uid: String, language: String): Boolean
}
