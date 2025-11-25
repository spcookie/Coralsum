package coralsum.repository

import coralsum.entity.RetrievalImageReqRecord
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface RetrievalImageReqRecordRepository : CoroutineCrudRepository<RetrievalImageReqRecord, Long> {
    suspend fun findByImageRef(imageRef: String): List<RetrievalImageReqRecord>

    suspend fun findByIp(ip: String): List<RetrievalImageReqRecord>

    suspend fun countByImageRef(imageRef: String): Long
}