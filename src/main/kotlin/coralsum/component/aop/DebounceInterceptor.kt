package coralsum.component.aop

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
@InterceptorBean(Debounce::class)
class DebounceInterceptor(
    private val securityService: SecurityService,
) : MethodInterceptor<Any, Any> {
    private val registry = ConcurrentHashMap<String, Long>()

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val ann = context.getAnnotation(Debounce::class.java)
        val window = ann.longValue("windowMillis").orElse(2000L)
        val byUid = ann.booleanValue("byUid").orElse(true)
        val name = ann.stringValue("name").orElse("")
        val uid = if (byUid && securityService.authentication.isPresent) {
            securityService.authentication.get().name
        } else ""
        val methodKey = name.ifBlank { "${context.declaringType.name}.${context.methodName}" }
        val key = buildString {
            append(methodKey)
            if (uid.isNotBlank()) append(":").append(uid)
        }
        val now = System.currentTimeMillis()
        val updated = registry.compute(key) { _, last ->
            if (last == null || now - last > window) now else last
        }
        if (updated != now) {
            throw HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试")
        }
        return context.proceed()
    }
}
