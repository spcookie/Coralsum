package coralsum.infrastructure.repository

import coralsum.common.enums.UserSource
import coralsum.infrastructure.entity.NickNameBitmap
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface NickNameBitmapRepository : CoroutineCrudRepository<NickNameBitmap, Long> {
    suspend fun findByBaseNameAndUserSource(baseName: String, userSource: UserSource): NickNameBitmap?
}