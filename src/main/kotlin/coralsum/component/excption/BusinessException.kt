package coralsum.component.excption

class BusinessException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException()