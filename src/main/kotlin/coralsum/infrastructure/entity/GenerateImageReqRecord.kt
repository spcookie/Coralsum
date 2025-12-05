package coralsum.infrastructure.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(
    value = "generate_image_req_record",
    namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class
)
data class GenerateImageReqRecord(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    var id: Long? = null,

    var userCode: String? = null,

    var requestText: String? = null,

    @field:TypeDef(type = DataType.BYTE_ARRAY)
    var requestImage: ByteArray? = null,

    @field:TypeDef(type = DataType.JSON)
    var requestConfig: String? = null,

    var retryCount: Int = 0,

    var inputTokens: Int = 0,

    var thoughtsTokens: Int = 0,

    var outputTokens: Int = 0,

    var durationMs: Long = 0,

    @field:DateCreated
    var createTime: LocalDateTime? = null,

    @field:DateUpdated
    var updateTime: LocalDateTime? = null,

    @field:CreatedBy
    var createBy: String? = null,

    @field:UpdatedBy
    var updateBy: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenerateImageReqRecord) return false

        if (id != other.id) return false
        if (retryCount != other.retryCount) return false
        if (inputTokens != other.inputTokens) return false
        if (thoughtsTokens != other.thoughtsTokens) return false
        if (outputTokens != other.outputTokens) return false
        if (durationMs != other.durationMs) return false
        if (userCode != other.userCode) return false
        if (requestText != other.requestText) return false
        if (!requestImage.contentEquals(other.requestImage)) return false
        if (requestConfig != other.requestConfig) return false
        if (createTime != other.createTime) return false
        if (updateTime != other.updateTime) return false
        if (createBy != other.createBy) return false
        if (updateBy != other.updateBy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + retryCount
        result = 31 * result + inputTokens
        result = 31 * result + thoughtsTokens
        result = 31 * result + outputTokens
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + (userCode?.hashCode() ?: 0)
        result = 31 * result + (requestText?.hashCode() ?: 0)
        result = 31 * result + (requestImage?.contentHashCode() ?: 0)
        result = 31 * result + (requestConfig?.hashCode() ?: 0)
        result = 31 * result + (createTime?.hashCode() ?: 0)
        result = 31 * result + (updateTime?.hashCode() ?: 0)
        result = 31 * result + (createBy?.hashCode() ?: 0)
        result = 31 * result + (updateBy?.hashCode() ?: 0)
        return result
    }

}
