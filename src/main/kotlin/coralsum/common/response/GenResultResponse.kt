package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "生成结果")
@Serdeable
data class GenResultResponse(
    val groups: MutableList<GroupResponse> = mutableListOf()
) {

    @Serdeable
    data class GroupResponse(
        var text: String? = null,
        var url: String? = null
    )

}