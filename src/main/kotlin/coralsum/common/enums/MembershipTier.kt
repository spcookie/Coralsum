package coralsum.common.enums

enum class MembershipTier {
    FREE,
    PLUS,
    PRO;

    companion object {
        fun of(value: String): MembershipTier {
            return when (value.uppercase()) {
                "FREE" -> FREE
                "PLUS" -> PLUS
                "PRO" -> PRO
                else -> throw IllegalArgumentException("Invalid MembershipTier value: $value")
            }
        }
    }
}