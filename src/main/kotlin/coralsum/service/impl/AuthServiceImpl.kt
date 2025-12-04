package coralsum.service.impl

import cn.hutool.core.util.IdUtil
import coralsum.common.enums.UserSource
import coralsum.entity.EmailVerificationCode
import coralsum.entity.OpenUser
import coralsum.entity.OutletUser
import coralsum.repository.EmailVerificationCodeRepository
import coralsum.repository.OpenUserRepository
import coralsum.repository.OutletUserRepository
import coralsum.service.IAuthService
import coralsum.service.IEmailService
import jakarta.inject.Singleton
import java.security.SecureRandom
import java.time.LocalDateTime

@Singleton
class AuthServiceImpl(
    private val emailRepo: EmailVerificationCodeRepository,
    private val emailService: IEmailService,
    private val openUserRepository: OpenUserRepository,
    private val outletUserRepository: OutletUserRepository,
) : IAuthService {
    companion object {
        private val secureRandom: SecureRandom = SecureRandom()
    }
    override suspend fun sendEmailCode(email: String, purpose: String): Boolean {
        val latest = emailRepo.findFirstByEmailAndPurposeAndUsedFalseOrderByExpireTimeDesc(email, purpose)
        if (latest != null) {
            val sentAt = latest.expireTime.minusMinutes(10)
            val withinCooldown = sentAt.isAfter(LocalDateTime.now().minusSeconds(60))
            if (withinCooldown) {
                return false
            }
        }
        val code = "%06d".format(secureRandom.nextInt(1_000_000))
        val rec = EmailVerificationCode(
            email = email,
            code = code,
            purpose = purpose,
            expireTime = LocalDateTime.now().plusMinutes(10),
        )
        emailRepo.save(rec)
        emailService.sendCode(email, code, purpose)
        return true
    }

    override suspend fun register(email: String, password: String, code: String): Boolean {
        val latest = emailRepo.findFirstByEmailAndPurposeAndUsedFalseOrderByExpireTimeDesc(email, "REGISTER")
        if (latest == null || latest.expireTime.isBefore(LocalDateTime.now()) || latest.code != code) return false
        val exists = outletUserRepository.findBySourceCodeAndUserSource(email, UserSource.WEB)
        if (exists != null && !exists.secret.isNullOrBlank()) return false
        val openUser = OpenUser(uid = IdUtil.getSnowflakeNextIdStr(), assignRole = "FREE")
        val savedOpen = openUserRepository.save(openUser)
        outletUserRepository.save(
            OutletUser(
                openUser = savedOpen,
                userSource = UserSource.WEB,
                sourceCode = email,
                secret = password,
            )
        )
        return true
    }

    override suspend fun resetPassword(email: String, newPassword: String, code: String): Boolean {
        val latest = emailRepo.findFirstByEmailAndPurposeAndUsedFalseOrderByExpireTimeDesc(email, "RESET")
        if (latest == null || latest.expireTime.isBefore(LocalDateTime.now()) || latest.code != code) return false
        val user = outletUserRepository.findBySourceCodeAndUserSource(email, UserSource.WEB) ?: return false
        val updated = user.copy(secret = newPassword)
        outletUserRepository.update(updated)
        return true
    }

    override suspend fun changePassword(
        email: String,
        oldPassword: String?,
        newPassword: String,
        code: String,
    ): Boolean {
        val latest = emailRepo.findFirstByEmailAndPurposeAndUsedFalseOrderByExpireTimeDesc(email, "RESET")
        if (latest == null || latest.expireTime.isBefore(LocalDateTime.now()) || latest.code != code) return false
        val user = outletUserRepository.findBySourceCodeAndUserSource(email, UserSource.WEB) ?: return false
        if (!user.secret.isNullOrBlank() && !user.secret.equals(oldPassword ?: "")) return false
        val updated = user.copy(secret = newPassword)
        outletUserRepository.update(updated)
        return true
    }
}
