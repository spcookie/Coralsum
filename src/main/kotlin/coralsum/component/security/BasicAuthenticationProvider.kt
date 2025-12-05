package coralsum.component.security

import coralsum.common.enums.UserSource
import coralsum.service.ITurnstileService
import coralsum.infrastructure.repository.OutletUserRepository
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationFailureReason
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.provider.HttpRequestReactiveAuthenticationProvider
import jakarta.inject.Singleton
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

@Singleton
class BasicAuthenticationProvider(
    val outletUserRepository: OutletUserRepository,
    val turnstileService: ITurnstileService,
) : HttpRequestReactiveAuthenticationProvider<Any> {
    override fun authenticate(
        requestContext: HttpRequest<Any>?,
        authenticationRequest: AuthenticationRequest<String, String>,
    ): Publisher<AuthenticationResponse> {
        return Mono.deferContextual { ctx ->
            mono(ReactorContext(ctx)) {
                val tokenHeader = requestContext?.headers?.get("CF-Turnstile-Response")
                    ?: requestContext?.headers?.get("X-Turnstile-Token")
                val remoteIp = requestContext?.headers?.get("CF-Connecting-IP")
                    ?: requestContext?.headers?.get("X-Forwarded-For")
                val tokenOk = turnstileService.validate(tokenHeader, remoteIp)
                if (!tokenOk) {
                    return@mono AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
                }
                val user = if (authenticationRequest.secret.isBlank() || authenticationRequest.identity.isBlank()) {
                    null
                } else {
                    try {
                        UserSource.of(authenticationRequest.identity)
                        outletUserRepository.findBySourceCodeAndUserSource(
                            authenticationRequest.secret,
                            UserSource.of(authenticationRequest.identity)
                        )
                    } catch (_: IllegalArgumentException) {
                        outletUserRepository.findBySourceCodeAndUserSourceAndSecret(
                            authenticationRequest.identity,
                            UserSource.WEB,
                            authenticationRequest.secret
                        )
                    }
                }
                if (user == null) {
                    AuthenticationResponse.failure(AuthenticationFailureReason.USER_NOT_FOUND)
                } else {
                    AuthenticationResponse.success(
                        user.openUser!!.uid,
                        user.openUser.assignRoleList() ?: emptyList(),
                        mapOf(
                            "outlet_user_source" to user.userSource,
                            "outlet_user_source_code" to user.sourceCode
                        )
                    )
                }
            }

        }

    }

}
