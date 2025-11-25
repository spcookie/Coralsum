package coralsum.common.enums

enum class SubscribeType {
    MONTHLY,
    YEARLY,
    TRIAL;

    companion object {
        fun of(value: String): SubscribeType {
            return when (value.uppercase()) {
                "MONTHLY" -> MONTHLY
                "YEARLY" -> YEARLY
                "TRIAL" -> TRIAL
                else -> throw IllegalArgumentException("Invalid SubscribeType value: $value")
            }
        }
    }
}