package coralsum.infrastructure.repository

import coralsum.infrastructure.entity.GenerateImageConf
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface GenerateImageConfRepository : CoroutineCrudRepository<GenerateImageConf, Long> {
    suspend fun findByOpenUserId(openUserId: Long): GenerateImageConf?
}