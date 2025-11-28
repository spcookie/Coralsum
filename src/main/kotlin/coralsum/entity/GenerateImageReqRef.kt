package coralsum.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.model.naming.NamingStrategies

@MappedEntity(
    value = "generate_image_req_ref",
    namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class
)
data class GenerateImageReqRef(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    var id: Long? = null,

    var recordId: Long,

    var imageRef: String,

    var imageSize: Int? = null,
)
