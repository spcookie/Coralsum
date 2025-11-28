package coralsum.repository

import coralsum.entity.GenerateImageReqRef
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface GenerateImageReqRefRepository : CoroutineCrudRepository<GenerateImageReqRef, Long> {
    suspend fun findByImageRef(imageRef: String): GenerateImageReqRef?
    suspend fun findAllByRecordId(recordId: Long): List<GenerateImageReqRef>
    suspend fun deleteAllByRecordId(recordId: Long)
}

