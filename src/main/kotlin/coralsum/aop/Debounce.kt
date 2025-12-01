package coralsum.aop

import io.micronaut.aop.Around

@Around
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Debounce(
    val name: String = "",
    val windowMillis: Long = 2000,
    val byUid: Boolean = true,
)

