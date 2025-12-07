package coralsum.service.impl

import coralsum.service.IEmailService
import coralsum.toolkit.logger
import io.micronaut.email.Email
import io.micronaut.email.EmailSender
import io.micronaut.email.MultipartBody
import jakarta.inject.Singleton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Singleton
class EmailServiceImpl(
    private val emailSender: EmailSender<*, *>,
    private val templateEngine: TemplateEngine,
) : IEmailService {

    companion object {
        private val log = logger<EmailServiceImpl>()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun sendCode(email: String, code: String, purpose: String) {
        GlobalScope.launch {
            val ctx = Context().apply {
                setVariable("brand", "Coralsum")
                setVariable("code", code)
                setVariable("purpose", purpose)
            }
            val html = templateEngine.process("email-code", ctx)
            val plain =
                "${"Coralsum"} ${(if (purpose == "REGISTER") "注册" else "重置密码")} 验证码：$code（10分钟内有效）"

            val mail = Email.builder()
                .to(email)
                .subject("${"Coralsum"} ${(if (purpose == "REGISTER") "注册" else "重置密码")} 验证码")
                .body(MultipartBody(html, plain))

            emailSender.send(mail)
        }
    }
}
