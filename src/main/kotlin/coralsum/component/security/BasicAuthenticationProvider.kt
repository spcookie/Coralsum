package coralsum.component.security

import coralsum.common.enums.UserSource
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
) : HttpRequestReactiveAuthenticationProvider<Any> {
    override fun authenticate(
        requestContext: HttpRequest<Any>?,
        authenticationRequest: AuthenticationRequest<String, String>,
    ): Publisher<AuthenticationResponse> {
        return Mono.deferContextual { ctx ->
            mono(ReactorContext(ctx)) {

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
