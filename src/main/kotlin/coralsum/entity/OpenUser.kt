package coralsum.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(value = "open_user", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class OpenUser(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    val uid: String,

    @field:Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "openUser")
    val outletUsers: MutableList<OutletUser>? = null,

    val assignRole: String? = null,

    @field:DateCreated
    val createTime: LocalDateTime? = null,

    @field:DateUpdated
    val updateTime: LocalDateTime? = null,

    @field:CreatedBy
    val createBy: String? = null,

    @field:UpdatedBy
    val updateBy: String? = null
) {
    fun assignRoleList() = assignRole?.split(",")?.map { it.trim() }
}