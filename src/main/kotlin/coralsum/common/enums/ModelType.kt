package coralsum.common.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "模型类型")
enum class ModelType(val id: String) {
    BASIC("BASIC"),
    PRO("PRO");

    override fun toString() = id
}

