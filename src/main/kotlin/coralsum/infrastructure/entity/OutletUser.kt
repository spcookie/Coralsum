package coralsum.infrastructure.entity

import coralsum.common.enums.UserSource
import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(value = "outlet_user", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class OutletUser(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    @field:MappedProperty(value = "open_user_id")
    val openUser: OpenUser? = null,

    val userSource: UserSource? = null,

    val nickName: String? = null,

    val nickTag: Int? = null,

    val sourceCode: String? = null,

    val secret: String? = null,

    @field:DateCreated
    val createTime: LocalDateTime? = null,

    @field:DateUpdated
    val updateTime: LocalDateTime? = null,

    @field:CreatedBy
    val createBy: String? = null,

    @field:UpdatedBy
    val updateBy: String? = null,
)
