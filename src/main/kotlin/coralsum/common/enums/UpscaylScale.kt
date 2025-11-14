package coralsum.common.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Upscayl Scale")
enum class UpscaylScale(val scale: Int) {
    X2(2),
    X3(3),
    X4(4);

    override fun toString() = scale.toString()
}