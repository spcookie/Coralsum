package coralsum.common.enums

enum class SubscribeStatus {
    UNSUBSCRIBED,
    ACTIVE,
    EXPIRED;

    companion object {
        fun of(value: Int): SubscribeStatus {
            return when (value) {
                0 -> UNSUBSCRIBED
                1 -> ACTIVE
                2 -> EXPIRED
                else -> throw IllegalArgumentException("Invalid SubscribeStatus value: $value")
            }
        }
    }
}