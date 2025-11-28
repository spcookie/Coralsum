package coralsum.repository

import coralsum.entity.EmailVerificationCode
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface EmailVerificationCodeRepository : CoroutineCrudRepository<EmailVerificationCode, Long> {
    suspend fun findFirstByEmailAndPurposeAndUsedFalseOrderByExpireTimeDesc(
        email: String,
        purpose: String,
    ): EmailVerificationCode?
}