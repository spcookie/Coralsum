package coralsum.service

import coralsum.common.enums.GenTaskStatue
import io.micronaut.http.HttpRequest
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

interface IGenerativeImage {

    suspend fun generate(genRequest: GenRequest, request: HttpRequest<*>): GenResult

    suspend fun preview(ref: String, ip: String): String?

    /**
     * 使用 Gemini 判断用户消息是否为生成或修改图片相关的意图。
     */
    suspend fun assessIntent(userMessage: String): IntentAssessment

    suspend fun submitGenerateTask(genRequest: GenRequest, request: HttpRequest<*>)

    suspend fun getGenerateTaskResult(): GenTaskResult

}

@Schema(description = "生成请求参数")
@Serdeable
data class GenRequest(
    @Schema(description = "生成文本") val text: String? = null,
    @Schema(description = "参考图片字节") var image: ByteArray? = null,
    @Schema(description = "候选数量") var candidateCount: Int = 1,
    @Schema(description = "宽高比") var aspectRatio: String? = null,
    @Schema(description = "系统提示") var system: String? = null,
    @Schema(description = "温度") var temperature: Float = 1f,
    @Schema(description = "最大输出tokens") var maxOutputTokens: Int = 32768,
    @Schema(description = "TopP") var topP: Float = 1f,
    @Schema(description = "图片格式") var format: String = "png",
    @Schema(description = "Upscayl模型") var upscaylModel: String? = null,
    @Schema(description = "Upscayl倍数") var upscaylScale: Int? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenRequest) return false

        if (candidateCount != other.candidateCount) return false
        if (temperature != other.temperature) return false
        if (maxOutputTokens != other.maxOutputTokens) return false
        if (topP != other.topP) return false
        if (upscaylScale != other.upscaylScale) return false
        if (text != other.text) return false
        if (!image.contentEquals(other.image)) return false
        if (aspectRatio != other.aspectRatio) return false
        if (system != other.system) return false
        if (format != other.format) return false
        if (upscaylModel != other.upscaylModel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = candidateCount
        result = 31 * result + temperature.hashCode()
        result = 31 * result + maxOutputTokens
        result = 31 * result + topP.hashCode()
        result = 31 * result + (upscaylScale ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (aspectRatio?.hashCode() ?: 0)
        result = 31 * result + (system?.hashCode() ?: 0)
        result = 31 * result + format.hashCode()
        result = 31 * result + (upscaylModel?.hashCode() ?: 0)
        return result
    }

}

@Serdeable
@Schema(description = "生成结果")
data class GenResult(
    @Schema(description = "输入tokens") val inputTokens: Int,
    @Schema(description = "输出tokens") val outputTokens: Int,
    @Schema(description = "耗时毫秒") val durationMs: Int,
    @Schema(description = "图片链接列表") val images: List<String?>,
    @Schema(description = "可选文本") val text: String? = null,
)

@Schema(description = "意图评估")
@Serdeable
data class IntentAssessment(
    @Schema(description = "是否生成意图") val generateIntent: Boolean,
    @Schema(description = "引导文案") val guideMessage: String? = null,
)

@Serdeable
@Schema(description = "生成任务状态")
data class GenTaskResult(
    @Schema(description = "任务状态") val status: GenTaskStatue,
    @Schema(description = "生成结果") val result: GenResult? = null,
)