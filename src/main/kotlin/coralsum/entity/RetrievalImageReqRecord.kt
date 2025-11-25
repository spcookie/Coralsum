package coralsum.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(
    value = "retrieval_image_req_record",
    namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class
)
data class RetrievalImageReqRecord(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    val imageRef: String,

    val ip: String? = null,

    @field:DateCreated
    val createTime: LocalDateTime? = null,

    @field:DateUpdated
    val updateTime: LocalDateTime? = null,

    @field:CreatedBy
    val createBy: String? = null,

    @field:UpdatedBy
    val updateBy: String? = null,
)