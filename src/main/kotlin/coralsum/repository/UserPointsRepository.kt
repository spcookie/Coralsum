package coralsum.repository

import coralsum.entity.UserPoints
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface UserPointsRepository : CoroutineCrudRepository<UserPoints, Long> {
    suspend fun findByOpenUserId(openUserId: Long): UserPoints?
}