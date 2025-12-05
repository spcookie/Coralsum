package coralsum.infrastructure.repository

import coralsum.infrastructure.entity.UserPoints
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface UserPointsRepository : CoroutineCrudRepository<UserPoints, Long> {
    @Join(value = "openUser")
    suspend fun findByOpenUserId(openUserId: Long): UserPoints?
}