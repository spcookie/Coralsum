package coralsum.common.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "宽高比")
enum class AspectRatio(val ratio: String) {
    R1_1("1:1"),
    R2_3("2:3"),
    R3_2("3:2"),
    R3_4("3:4"),
    R4_3("4:3"),
    R9_16("9:16"),
    R16_9("16:9"),
    R21_9("21:9");

    override fun toString() = ratio
}