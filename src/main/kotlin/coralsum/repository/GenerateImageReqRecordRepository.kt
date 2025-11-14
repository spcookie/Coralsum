package coralsum.repository

import coralsum.entity.GenerateImageReqRecord
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface GenerateImageReqRecordRepository : CoroutineCrudRepository<GenerateImageReqRecord, Long> {
    suspend fun findByUserCode(userCode: String): List<GenerateImageReqRecord>
}