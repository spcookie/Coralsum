package coralsum.common.event

data class UserRegisteredEvent(
    val openUserId: Long,
    val uid: String,
)
