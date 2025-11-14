package coralsum.common.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "图片格式")
enum class ImageFormat(val ext: String) {
    PNG("png"),
    JPG("jpg"),
    WEBP("webp");

    override fun toString() = ext
}