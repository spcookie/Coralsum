package coralsum.service

interface IEmailService {
    suspend fun sendCode(email: String, code: String, purpose: String)
}