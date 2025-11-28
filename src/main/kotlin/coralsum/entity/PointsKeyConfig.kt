package coralsum.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serdeable
@MappedEntity(value = "points_key_config", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class PointsKeyConfig(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    val name: String,

    @field:TypeDef(type = DataType.BIGDECIMAL)
    val permanentPoints: BigDecimal = BigDecimal.ZERO,

    @field:TypeDef(type = DataType.BIGDECIMAL)
    val subscribePoints: BigDecimal = BigDecimal.ZERO,

    val subscribeType: String? = null,

    val subscribeDurationDays: Int = 0,

    val validFrom: LocalDateTime? = null,
    val validTo: LocalDateTime? = null,

    val disabled: Boolean = false,

    @field:DateCreated
    val createTime: LocalDateTime? = null,
    @field:DateUpdated
    val updateTime: LocalDateTime? = null,
    @field:CreatedBy
    val createBy: String? = null,
    @field:UpdatedBy
    val updateBy: String? = null,
)