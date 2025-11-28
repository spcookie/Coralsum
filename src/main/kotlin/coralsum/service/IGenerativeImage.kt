package coralsum.service

import coralsum.common.enums.*
import io.micronaut.http.HttpRequest
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

interface IGenerativeImage {

    suspend fun generate(genRequest: GenRequest, request: HttpRequest<*>): GenResult

    suspend fun preview(ref: String, ip: String): String?

    suspend fun assessIntent(userMessage: String): IntentAssessment

    suspend fun submitGenerateTask(genRequest: GenRequest, request: HttpRequest<*>)

    suspend fun getGenerateTaskResult(): GenTaskResult

}

@Schema(description = "生成请求参数")
@Serdeable
data class GenRequest(
    @field:Schema(description = "生成文本") val text: String? = null,
    @field:Schema(description = "参考图片字节列表") var images: List<ByteArray>? = null,
    @field:Schema(description = "候选数量") var candidateCount: Int = 1,
    @field:Schema(description = "宽高比") var aspectRatio: AspectRatio? = null,
    @field:Schema(description = "系统提示") var system: String? = null,
    @field:Schema(description = "温度") var temperature: Float = 1f,
    @field:Schema(description = "最大输出tokens") var maxOutputTokens: Int = 32768,
    @field:Schema(description = "TopP") var topP: Float = 1f,
    @field:Schema(description = "图片格式") var format: ImageFormat = ImageFormat.PNG,
    @field:Schema(description = "Upscayl模型") var upscaylModel: UpscaylModel? = null,
    @field:Schema(description = "Upscayl倍数") var upscaylScale: UpscaylScale? = null,
    @field:Schema(description = "输出分辨率") var imageSize: ImageSize? = ImageSize.X1,
    @field:Schema(description = "媒体分辨率") var mediaResolution: MediaResolution? = MediaResolution.AUTO,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenRequest) return false

        if (candidateCount != other.candidateCount) return false
        if (temperature != other.temperature) return false
        if (maxOutputTokens != other.maxOutputTokens) return false
        if (topP != other.topP) return false
        if (upscaylScale != other.upscaylScale) return false
        if (mediaResolution != other.mediaResolution) return false
        if (text != other.text) return false
        val thisImages = images
        val otherImages = other.images
        if (thisImages == null && otherImages != null) return false
        if (thisImages != null && otherImages == null) return false
        if (thisImages != null && otherImages != null) {
            if (thisImages.size != otherImages.size) return false
            for (i in thisImages.indices) {
                if (!thisImages[i].contentEquals(otherImages[i])) return false
            }
        }
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
        result = 31 * result + (upscaylScale?.hashCode() ?: 0)
        result = 31 * result + (mediaResolution?.hashCode() ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + (
                images?.fold(1) { acc, bytes -> 31 * acc + bytes.contentHashCode() } ?: 0
                )
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
    @field:Schema(description = "输入tokens") val inputTokens: Int,
    @field:Schema(description = "输出tokens") val outputTokens: Int,
    @field:Schema(description = "耗时毫秒") val durationMs: Int,
    @field:Schema(description = "图片链接列表") val images: List<String?>,
    @field:Schema(description = "可选文本") val text: String? = null,
)

//@Schema(description = "意图评估")
@Serdeable
data class IntentAssessment(
    val generateIntent: Boolean,
    val guideMessage: String? = null,
)

@Serdeable
@Schema(description = "生成任务状态")
data class GenTaskResult(
    @field:Schema(description = "任务状态") val status: GenTaskStatue,
    @field:Schema(description = "生成结果") val result: GenResult? = null,
)
