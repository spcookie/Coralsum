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
@MappedEntity(value = "POINTS_KEY_CONFIG", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class PointsKeyConfig(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    @field:MappedProperty("ID")
    val id: Long? = null,

    @field:MappedProperty("NAME")
    val name: String,

    @field:TypeDef(type = DataType.BIGDECIMAL)
    @field:MappedProperty("PERMANENT_POINTS")
    val permanentPoints: BigDecimal = BigDecimal.ZERO,

    @field:TypeDef(type = DataType.BIGDECIMAL)
    @field:MappedProperty("SUBSCRIBE_POINTS")
    val subscribePoints: BigDecimal = BigDecimal.ZERO,

    @field:MappedProperty("SUBSCRIBE_TYPE")
    val subscribeType: String? = null,

    @field:MappedProperty("PERIOD_UNIT")
    val periodUnit: String? = null,
    @field:MappedProperty("PERIOD_COUNT")
    val periodCount: Int = 0,

    @field:MappedProperty("VALID_TO")
    val validTo: LocalDateTime? = null,

    @field:MappedProperty("DISABLED")
    val disabled: Boolean = false,

    @field:DateCreated
    @field:MappedProperty("CREATE_TIME")
    val createTime: LocalDateTime? = null,
    @field:DateUpdated
    @field:MappedProperty("UPDATE_TIME")
    val updateTime: LocalDateTime? = null,
    @field:CreatedBy
    @field:MappedProperty("CREATE_BY")
    val createBy: String? = null,
    @field:UpdatedBy
    @field:MappedProperty("UPDATE_BY")
    val updateBy: String? = null,
)
