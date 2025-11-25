package coralsum.common.response

import coralsum.common.enums.GenTaskStatue
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "生成任务结果")
@Serdeable
data class GenTaskResultResponse(
    @Schema(description = "任务状态") val status: GenTaskStatue,
    @Schema(description = "生成结果") val result: GenResultResponse? = null,
)
