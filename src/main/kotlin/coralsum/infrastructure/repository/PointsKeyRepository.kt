package coralsum.infrastructure.repository

import coralsum.infrastructure.entity.PointsKey
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@Repository
@R2dbcRepository(dialect = Dialect.H2)
interface PointsKeyRepository : CoroutineCrudRepository<PointsKey, Long> {
    suspend fun findByKeyCode(keyCode: String): PointsKey?
    suspend fun findAll(pageable: Pageable): Page<PointsKey>
    suspend fun findByKeyCodeContains(keyCode: String, pageable: Pageable): Page<PointsKey>

    @Query("UPDATE POINTS_KEY SET enabled = :enabled WHERE id IN (:ids)")
    suspend fun batchToggleEnabled(ids: List<Long>, enabled: Boolean)
}
