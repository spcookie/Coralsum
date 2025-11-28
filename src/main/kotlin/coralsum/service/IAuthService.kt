package coralsum.service

interface IAuthService {
    suspend fun sendEmailCode(email: String, purpose: String): Boolean
    suspend fun register(email: String, password: String, code: String): Boolean
    suspend fun resetPassword(email: String, newPassword: String, code: String): Boolean
    suspend fun changePassword(email: String, oldPassword: String?, newPassword: String, code: String): Boolean
}