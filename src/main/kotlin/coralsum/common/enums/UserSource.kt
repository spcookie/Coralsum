package coralsum.common.enums

enum class UserSource {
    LOCAL,
    WECHAT,
    WEB;

    companion object {
        fun of(value: String): UserSource {
            return when (value) {
                "LOCAL" -> LOCAL
                "WECHAT" -> WECHAT
                "WEB" -> WEB
                else -> throw IllegalArgumentException("Invalid UserSource value: $value")
            }
        }
    }
}