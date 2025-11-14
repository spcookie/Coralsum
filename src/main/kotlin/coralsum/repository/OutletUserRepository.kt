package coralsum.repository

import coralsum.entity.OutletUser
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface OutletUserRepository : CoroutineCrudRepository<OutletUser, Long> {
    @Join(value = "openUser")
    suspend fun findBySourceCodeAndUserSource(sourceCode: String, userSource: String): OutletUser?
}