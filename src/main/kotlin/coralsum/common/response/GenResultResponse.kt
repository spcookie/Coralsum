package coralsum.common.response

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "生成结果")
@Serdeable
data class GenResultResponse(
    @field:Schema(description = "输入tokens") val inputTokens: Int,
    @field:Schema(description = "输出tokens") val outputTokens: Int,
    @field:Schema(description = "耗时毫秒") val durationMs: Int,
    @field:Schema(description = "图片链接列表") val images: MutableList<String?>,
    @field:Schema(description = "分享页面链接列表") val linkImages: MutableList<String?> = mutableListOf(),
    @field:Schema(description = "可选文本") val text: String? = null,
)