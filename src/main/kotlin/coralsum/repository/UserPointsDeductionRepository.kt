package coralsum.repository

import coralsum.entity.UserPointsDeduction
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface UserPointsDeductionRepository : CoroutineCrudRepository<UserPointsDeduction, Long> {
    suspend fun findByRecordIdAndImageRef(recordId: Long, imageRef: String): UserPointsDeduction?
    suspend fun findByRecordIdAndImageRefIsNull(recordId: Long): UserPointsDeduction?
}

