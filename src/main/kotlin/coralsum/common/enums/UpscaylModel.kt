package coralsum.common.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Upscayl Model")
enum class UpscaylModel(val modelName: String) {
    LSDIR_X4("4xLSDIR"),
    LSDIR_COMPACT_C3_X4("4xLSDIRCompactC3"),
    REAL_ESRGAN_GENERAL_WDN_X4_V3("RealESRGAN_General_WDN_x4_v3"),
    NOMOS8K_SC_X4("4xNomos8kSC"),
    NMKD_SIAX_200K_X4("4x_NMKD-Siax_200k"),
    UNKNOWN_201("unknown-2.0.1"),
    ANIME_V3_X2("realesr-animevideov3-x2"),
    UNISCALE_RESTORE("uniscale_restore"),
    REAL_ESRGAN_GENERAL_X4_V3("RealESRGAN_General_x4_v3"),
    ANIME_V3_X3("realesr-animevideov3-x3"),
    ANIME_V3_X4("realesr-animevideov3-x4"),
    LSDIR_PLUS_C_X4("4xLSDIRplusC"),
    NMKD_SUPERSCALE_SP_178000_G_X4("4x_NMKD-Superscale-SP_178000_G"),
    HFA2K_X4("4xHFA2k"),
    UPSCAYL_STANDARD_4X("upscayl-standard-4x"),
    UPSCAYL_LITE_4X("upscayl-lite-4x"),
    DIGITAL_ART_4X("digital-art-4x"),
    HIGH_FIDELITY_4X("high-fidelity-4x"),
    REMACRI_4X("remacri-4x"),
    ULTRAMIX_BALANCED_4X("ultramix-balanced-4x"),
    ULTRASHARP_4X("ultrasharp-4x");

    override fun toString() = modelName
}