package coralsum.infrastructure.entity

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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NickNameBitmap) return false

        if (id != other.id) return false
        if (version != other.version) return false
        if (userSource != other.userSource) return false
        if (baseName != other.baseName) return false
        if (!bitmap.contentEquals(other.bitmap)) return false
        if (createTime != other.createTime) return false
        if (updateTime != other.updateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + userSource.hashCode()
        result = 31 * result + baseName.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        result = 31 * result + (createTime?.hashCode() ?: 0)
        result = 31 * result + (updateTime?.hashCode() ?: 0)
        return result
    }
}