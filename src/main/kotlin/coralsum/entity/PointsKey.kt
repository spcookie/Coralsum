package coralsum.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime

@Serdeable
@MappedEntity(value = "points_key", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class PointsKey(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    @field:MappedProperty("key_code")
    val keyCode: String,

    @field:MappedProperty("config_id")
    val configId: Long,

    @field:TypeDef(type = DataType.JSON)
    @field:MappedProperty("config_json")
    val configJson: String? = null,

    val used: Boolean = false,
    val usedTime: LocalDateTime? = null,
    val usedIp: String? = null,
    val usedUid: String? = null,

    val enabled: Boolean = true,
    val expireTime: LocalDateTime? = null,

    @field:DateCreated
    val createTime: LocalDateTime? = null,
    @field:DateUpdated
    val updateTime: LocalDateTime? = null,
    @field:CreatedBy
    val createBy: String? = null,
    @field:UpdatedBy
    val updateBy: String? = null,
)