package coralsum.infrastructure.repository

import coralsum.infrastructure.entity.GenerateImageReqRecord
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

@R2dbcRepository(dialect = Dialect.H2)
interface GenerateImageReqRecordRepository : CoroutineCrudRepository<GenerateImageReqRecord, Long> {
    suspend fun findByUserCode(userCode: String): List<GenerateImageReqRecord>

    @Query("SELECT * FROM generate_image_req_record WHERE create_time < :cutoff AND id IN (SELECT record_id FROM generate_image_req_ref)")
    suspend fun findExpiredBefore(cutoff: LocalDateTime): List<GenerateImageReqRecord>
}
