package coralsum.infrastructure.entity

import io.micronaut.data.annotation.*
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.data.model.DataType
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(
    value = "idea_template",
    namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class
)
@Serdeable
data class IdeaTemplate(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    var id: Long? = null,

    var name: String,

    var description: String? = null,

    var promptText: String,

    var categoryId: Long? = null,

    var tipsNeedImage: Boolean = false,

    var tipsNeedProModel: Boolean = false,

    var tipsNeedEditPrompt: Boolean = false,

    @field:TypeDef(type = DataType.JSON)
    var originalImageRefs: String? = null,

    var effectImageRef: String? = null,

    var usageCount: Int = 0,

    var lastUsedTime: LocalDateTime? = null,

    @field:DateCreated
    var createTime: LocalDateTime? = null,

    @field:DateUpdated
    var updateTime: LocalDateTime? = null,

    @field:CreatedBy
    var createBy: String? = null,

    @field:UpdatedBy
    var updateBy: String? = null,
) {
    @Transient
    var isNew: Boolean? = null

    @Transient
    var isHot: Boolean? = null

    @Transient
    var categoryName: String? = null

    @Transient
    var tagNames: List<String>? = null

    @Transient
    var tagIds: List<Long>? = null

    @Transient
    var categoryPath: List<String>? = null
}
