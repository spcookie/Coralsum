package coralsum.common.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "媒体分辨率")
enum class MediaResolution {
    AUTO,
    LOW,
    MEDIUM,
    HIGH;
}