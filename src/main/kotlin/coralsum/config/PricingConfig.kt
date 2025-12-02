package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("pricing")
class PricingConfig {
    /**
     * 美元兑人民币汇率（用于将模型与图片费用从 USD 转为 RMB）
     */
    var usdToCny: Double = 7.01

    /**
     * 积分抽成/换算系数；最终扣减积分 = 成本(RMB) * coefficient
     */
    var coefficient: Double = 1.5

    /**
     * 每 1 元人民币折算为多少积分；用于将人民币成本转换为平台积分
     */
    var pointsPerRmb: Double = 100.0

    /**
     * Gemini 2.5 Flash-Lite 评估环节的价格参数（按令牌计费）
     */
    val flashLite = FlashLite()

    /**
     * gemini-3-pro-image-preview 生成环节的价格参数（令牌 + 单张图片价格）
     */
    val imagePreview = ImagePreview()

    val pro = Pro()

    /**
     * BASIC 模型的简化计费（输入统一价格 + 每张1K输出图片价格）
     */
    val basic = Basic()

    /**
     * OSS 出网费用配置（按时间段区分单价）
     */
    val oss = Oss()

    /**
     * 内网穿透与代理流量费用配置，以及访问倍数（考虑图片被多次访问）
     */
    val traffic = Traffic()
    val upscayl = Upscayl()

    /**
     * Flash-Lite 评估价格：将字符转令牌，并按输入/输出的百万令牌单价计费
     */
    class FlashLite {
        var tokensPerChar: Double = 2.5
        var inputUsdPerMTokens: Double = 0.10
        var outputUsdPerMTokens: Double = 0.40
    }

    /**
     * Image Preview 生成价格：按输入/输出令牌计费，且每张图片按分辨率收取固定单价
     */
    class ImagePreview {
        var tokensPerMb: Double = 2500.0
    }

    /**
     * BASIC 模型：输入统一价格（文本/图片）与 1K 输出每张图片固定价格（USD）
     */
    class Basic {
        var inputUsdPerMTokens: Double = 0.30
        var outputPricePerImage1kUsd: Double = 0.039
        var outputUsdPerMTokens: Double = 30.0
    }

    class Pro {
        var inputUsdPerMTokens: Double = 2.0
        var outputUsdPerMTokens: Double = 12.0
        var pricePerImage1k2kUsd: Double = 0.134
        var pricePerImage4kUsd: Double = 0.24
    }

    /**
     * OSS 出网费用：按 GB 计费，支持闲时/忙时单价与忙时区间
     */
    class Oss {
        /** 闲时单价（RMB/GB），默认 0.25 */
        var idleRmbPerGb: Double = 0.25
        /** 忙时单价（RMB/GB），默认 0.50 */
        var busyRmbPerGb: Double = 0.50
        /** 忙时起始小时（0-24），默认 8 */
        var busyStartHour: Int = 8
        /** 忙时结束小时（0-24），默认 24 */
        var busyEndHour: Int = 24
    }

    /**
     * 内网穿透/代理流量费用：按 GB 计费；visitMultiplier 为访问倍数
     */
    class Traffic {
        /** 内网穿透单价（RMB/GB），默认 1.1 */
        var natRmbPerGb: Double = 1.1
        /** 代理流量单价（RMB/GB），默认 1.6 */
        var proxyRmbPerGb: Double = 1.6
        /** 图片访问倍数（用于放大流量成本），默认 5 */
        var visitMultiplier: Double = 5.0
    }

    /**
     * 估算参数：按分辨率与格式估计每张输出图片的体积（Bytes）
     */
    val estimate = Estimate()

    class Estimate {
        /** PNG 格式每张图片估计大小（Bytes） */
        var pngBytes1K: Long = 800_000
        var pngBytes2K: Long = 1_600_000
        var pngBytes4K: Long = 3_500_000
        /** JPG 格式每张图片估计大小（Bytes） */
        var jpgBytes1K: Long = 500_000
        var jpgBytes2K: Long = 1_100_000
        var jpgBytes4K: Long = 2_200_000
    }

    class Upscayl {
        var enabled: Boolean = true
        var chargeByScale: Boolean = true
    }
}
