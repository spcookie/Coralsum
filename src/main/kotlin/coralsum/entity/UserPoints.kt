package coralsum.entity

import coralsum.common.enums.MembershipTier
import coralsum.common.enums.SubscribeStatus
import coralsum.common.enums.SubscribeType
import io.micronaut.data.annotation.*
import io.micronaut.data.model.naming.NamingStrategies
import io.micronaut.security.annotation.CreatedBy
import io.micronaut.security.annotation.UpdatedBy
import java.time.LocalDateTime

@MappedEntity(value = "user_points", namingStrategy = NamingStrategies.UnderScoreSeparatedLowerCase::class)
data class UserPoints(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.AUTO)
    var id: Long? = null,

    @field:Relation(value = Relation.Kind.MANY_TO_ONE)
    @field:MappedProperty(value = "open_user_id")
    var openUser: OpenUser? = null,

    var permanentPoints: Int = 0,

    var subscribePoints: Int = 0,

    var subscribeExpireTime: LocalDateTime? = null,

    var subscribeDurationDays: Int = 0,

    var subscribeStatus: SubscribeStatus? = null,

    var subscribeType: SubscribeType? = null,

    var tier: MembershipTier = MembershipTier.FREE,

    @field:DateCreated
    var createTime: LocalDateTime? = null,

    @field:DateUpdated
    var updateTime: LocalDateTime? = null,

    @field:CreatedBy
    var createBy: String? = null,

    @field:UpdatedBy
    var updateBy: String? = null,
)