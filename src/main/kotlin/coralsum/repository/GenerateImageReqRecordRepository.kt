package coralsum.repository

import coralsum.entity.GenerateImageReqRecord
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

@R2dbcRepository(dialect = Dialect.H2)
interface GenerateImageReqRecordRepository : CoroutineCrudRepository<GenerateImageReqRecord, Long> {
    suspend fun findByUserCode(userCode: String): List<GenerateImageReqRecord>

    @Query("SELECT * FROM generate_image_req_record WHERE image_ref IS NOT NULL AND create_time < :cutoff")
    suspend fun findExpiredBefore(cutoff: LocalDateTime): List<GenerateImageReqRecord>

    suspend fun findByImageRef(imageRef: String): GenerateImageReqRecord?
}