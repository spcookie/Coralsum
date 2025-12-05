package coralsum.infrastructure.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(
    value = "generate_image_conf",
    namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class
)
data class GenerateImageConf(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:Relation(value = Relation.Kind.ONE_TO_ONE)
    @field:MappedProperty(value = "open_user_id")
    val openUser: OpenUser? = null,

    @field:TypeDef(type = DataType.JSON)
    val conf: String? = null,

    @field:DateCreated
    val createTime: LocalDateTime? = null,

    @field:DateUpdated
    val updateTime: LocalDateTime? = null,

    @field:CreatedBy
    val createBy: String? = null,

    @field:UpdatedBy
    val updateBy: String? = null,
)