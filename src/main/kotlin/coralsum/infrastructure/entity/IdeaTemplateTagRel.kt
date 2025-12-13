package coralsum.infrastructure.entity

import io.micronaut.data.annotation.*
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(
    value = "idea_template_tag_rel",
    namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class
)
@Serdeable
data class IdeaTemplateTagRel(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    var id: Long? = null,

    var templateId: Long,

    var tagId: Long,

    @field:DateCreated
    var createTime: LocalDateTime? = null,

    @field:DateUpdated
    var updateTime: LocalDateTime? = null,

    @field:CreatedBy
    var createBy: String? = null,

    @field:UpdatedBy
    var updateBy: String? = null,
)
