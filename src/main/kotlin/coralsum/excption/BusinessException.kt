package coralsum.excption

class BusinessException(
    override val message: String?,
    override val cause: Throwable?
) : RuntimeException()