package coralsum.repository

import coralsum.entity.PointsKeyConfig
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@Repository
@R2dbcRepository(dialect = Dialect.H2)
interface PointsKeyConfigRepository : CoroutineCrudRepository<PointsKeyConfig, Long> {
    suspend fun findByName(name: String): PointsKeyConfig?

    suspend fun findAll(pageable: Pageable): Page<PointsKeyConfig>
    suspend fun findByNameContains(name: String, pageable: Pageable): Page<PointsKeyConfig>

    @Query("UPDATE POINTS_KEY_CONFIG SET DISABLED = :disabled WHERE ID = :id")
    suspend fun updateDisabled(id: Long, disabled: Boolean)
}
