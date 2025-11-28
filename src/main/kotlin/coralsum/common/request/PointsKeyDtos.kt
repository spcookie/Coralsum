package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serdeable
data class CreatePointsKeyConfigReq(
    val name: String,
    val permanentPoints: BigDecimal = BigDecimal.ZERO,
    val subscribePoints: BigDecimal = BigDecimal.ZERO,
    val subscribeType: String? = null,
    val subscribeDurationDays: Int = 0,
    val validFrom: LocalDateTime? = null,
    val validTo: LocalDateTime? = null,
)

@Serdeable
data class GeneratePointsKeysReq(
    val configId: Long,
    val count: Int,
)

@Serdeable
data class ToggleKeysReq(
    val ids: List<Long>,
    val enabled: Boolean,
)

@Serdeable
data class RedeemPointsReq(
    val email: String? = null,
    val key: String,
)

@Serdeable
data class RedeemPointsResp(
    val pointsAdded: Int,
    val points: Int,
)