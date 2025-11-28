package coralsum.service.impl

import coralsum.service.IEmailService
import jakarta.inject.Singleton

@Singleton
class EmailServiceAliyun : IEmailService {
    override suspend fun sendCode(email: String, code: String, purpose: String) {
        // TODO 使用阿里云邮件/短信SDK实现邮件验证码发送
    }
}