package coralsum.component.turnstile

import coralsum.component.annotation.Turnstile
import io.micronaut.web.router.Router
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class TurnstileProtectedRegistry(
    private val router: Router,
) {
    private val protectedPaths = ConcurrentHashMap.newKeySet<String>()

    @PostConstruct
    fun init() {
        router.uriRoutes().forEach { route ->
            val meta = route.annotationMetadata
            if (meta.hasAnnotation(Turnstile::class.java)) {
                protectedPaths.add(route.uriMatchTemplate.toString())
            }
        }
    }

    fun isProtectedPath(path: String): Boolean {
        if (protectedPaths.isEmpty()) return false
        return protectedPaths.any { template ->
            when {
                template == path -> true
                template.endsWith("/**") && path.startsWith(template.removeSuffix("/**")) -> true
                template.contains("/{") -> {
                    val prefix = template.substringBefore("/{")
                    path.startsWith(prefix)
                }

                else -> false
            }
        }
    }
}
