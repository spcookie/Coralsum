package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Positive

@Serdeable
data class RedeemPointsResp(
    val pointsAdded: Int,
    val points: Int,
)

@Serdeable
data class ToggleConfigReq(
    @field:Positive(message = "配置ID需为正数")
    val id: Long,
    val disabled: Boolean,
)
