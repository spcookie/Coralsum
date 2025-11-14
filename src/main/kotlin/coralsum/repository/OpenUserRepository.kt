package coralsum.repository

import coralsum.entity.OpenUser
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface OpenUserRepository : CoroutineCrudRepository<OpenUser, Long> {
    suspend fun findByUid(uid: String): OpenUser?
}