package coralsum.common.response

import coralsum.common.enums.MembershipTier
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户档案")
@Serdeable
data class ProfileResponse(
    @Schema(description = "用户UID") val uid: String,
    @Schema(description = "权限列表") val permissions: List<String>,
    @Schema(description = "昵称") val nickName: String,
    @Schema(description = "昵称编号") val nickTag: Int? = null,
    @Schema(description = "来源标识码") val sourceCode: String,
    @Schema(description = "永久积分") val permanentPoints: Int,
    @Schema(description = "订阅积分") val subscribePoints: Int,
    @Schema(description = "会员层级") val tier: MembershipTier,
)
