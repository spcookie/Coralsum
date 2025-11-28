package coralsum.entity

import coralsum.common.enums.UserSource
import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import java.time.LocalDateTime

@MappedEntity(value = "nick_name_bitmap", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class NickNameBitmap(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    val id: Long? = null,

    val userSource: UserSource,

    val baseName: String,

    val bitmap: ByteArray,

    @field:Version
    val version: Long? = null,

    @field:DateCreated
    val createTime: LocalDateTime? = null,

    @field:DateUpdated
    val updateTime: LocalDateTime? = null,
)