package coralsum.infrastructure.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(value = "email_verification_code", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class EmailVerificationCode(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    val email: String,

    val code: String,

    val purpose: String,

    val expireTime: LocalDateTime,

    val used: Boolean = false,

    @field:DateCreated
    val createTime: LocalDateTime? = null,

    @field:DateUpdated
    val updateTime: LocalDateTime? = null,

    @field:CreatedBy
    val createBy: String? = null,

    @field:UpdatedBy
    val updateBy: String? = null,
)