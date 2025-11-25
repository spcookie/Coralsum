package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "意图评估结果")
@Serdeable
data class IntentAssessmentResponse(
    @Schema(description = "是否为生成/修改图片意图") val generateIntent: Boolean,
    @Schema(description = "引导文案") val guideMessage: String?,
)