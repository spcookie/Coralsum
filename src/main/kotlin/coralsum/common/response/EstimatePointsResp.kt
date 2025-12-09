package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal

@Serdeable
data class EstimatePointsResp(
    val evalRmb: BigDecimal,
    val previewRmbTokens: BigDecimal,
    val imageRmb: BigDecimal,
    val ossRmb: BigDecimal,
    val natRmb: BigDecimal,
    val proxyRmb: BigDecimal,
    val totalCostRmb: BigDecimal,
    val pointsToDeduct: Int,
)

