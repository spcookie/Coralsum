package coralsum.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime

@Serdeable
@MappedEntity(value = "POINTS_KEY", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class PointsKey(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    @field:MappedProperty("ID")
    val id: Long? = null,

    @field:MappedProperty("KEY_CODE")
    val keyCode: String,

    @field:MappedProperty("CONFIG_ID")
    val configId: Long,

    @field:TypeDef(type = DataType.JSON)
    @field:MappedProperty("CONFIG_JSON")
    val configJson: String? = null,

    @field:MappedProperty("USED")
    val used: Boolean = false,
    @field:MappedProperty("USED_TIME")
    val usedTime: LocalDateTime? = null,
    @field:MappedProperty("USED_IP")
    val usedIp: String? = null,
    @field:MappedProperty("USED_UID")
    val usedUid: String? = null,

    @field:MappedProperty("ENABLED")
    val enabled: Boolean = true,

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
