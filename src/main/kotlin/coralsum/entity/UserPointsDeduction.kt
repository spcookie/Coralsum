package coralsum.entity

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import java.math.BigDecimal
import java.time.LocalDateTime

@MappedEntity(value = "user_points_deduction", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class UserPointsDeduction(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    var id: Long? = null,

    var uid: String,

    var recordId: Long?,

    var imageRef: String?,

    var success: Boolean,

    var pointsDeducted: BigDecimal,

    @field:io.micronaut.data.annotation.TypeDef(type = DataType.JSON)
    var costDetail: String? = null,

    @field:DateCreated
    var createTime: LocalDateTime? = null,

    @field:DateUpdated
    var updateTime: LocalDateTime? = null,
)

