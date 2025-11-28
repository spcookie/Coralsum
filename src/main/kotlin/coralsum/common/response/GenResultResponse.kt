package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "生成结果")
@Serdeable
data class GenResultResponse(
    @Schema(description = "输入tokens") val inputTokens: Int,
    @Schema(description = "输出tokens") val outputTokens: Int,
    @Schema(description = "耗时毫秒") val durationMs: Int,
    @Schema(description = "图片链接列表") val images: MutableList<String?>,
    @Schema(description = "分享页面链接列表") val linkImages: MutableList<String?> = mutableListOf(),
    @Schema(description = "可选文本") val text: String? = null,
)