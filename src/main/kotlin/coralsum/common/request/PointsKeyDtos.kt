package coralsum.common.request

import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.*
import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

@Serdeable
data class CreatePointsKeyConfigReq(
    @field:NotBlank(message = "配置名称不能为空")
    val name: String,

    @field:DecimalMin(value = "0", inclusive = true, message = "永久积分需≥0")
    val permanentPoints: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0", inclusive = true, message = "订阅积分需≥0")
    val subscribePoints: BigDecimal = BigDecimal.ZERO,

    val subscribeType: String? = null,
    val periodUnit: String? = null,

    @field:PositiveOrZero(message = "周期数量需≥0")
    val periodCount: Int = 0,

    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val validTo: LocalDateTime? = null,
)

@Serdeable
data class GeneratePointsKeysReq(
    @field:Positive(message = "配置ID需为正数")
    val configId: Long,

    @field:Min(value = 1, message = "生成数量需≥1")
    @field:Max(value = 1000, message = "生成数量不可超过1000")
    val count: Int,
)

@Serdeable
data class ToggleKeysReq(
    @field:NotEmpty(message = "请选择至少一个密钥")
    val ids: List<Long>,
    val enabled: Boolean,
)

@Serdeable
data class RedeemPointsReq(
    val email: String? = null,
    @field:NotBlank(message = "密钥不能为空")
    val key: String,
)

@Serdeable
data class RedeemPointsResp(
    val pointsAdded: Int,
    val points: Int,
)
