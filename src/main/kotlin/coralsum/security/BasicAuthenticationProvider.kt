package coralsum.security

import coralsum.repository.OutletUserRepository
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.provider.HttpRequestReactiveAuthenticationProvider
import jakarta.inject.Singleton
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher

@Singleton
class BasicAuthenticationProvider(
    val outletUserRepository: OutletUserRepository
) : HttpRequestReactiveAuthenticationProvider<Any> {
    override fun authenticate(
        requestContext: HttpRequest<Any>?,
        authenticationRequest: AuthenticationRequest<String, String>
    ): Publisher<AuthenticationResponse> {
        return mono {
            if (authenticationRequest.secret.isBlank() || authenticationRequest.identity.isBlank()) {
                null
            } else {
                outletUserRepository.findBySourceCodeAndUserSource(
                    authenticationRequest.secret,
                    authenticationRequest.identity
                )
            }
        }.map {
            if (it == null) {
                AuthenticationResponse.failure()
            } else {
                AuthenticationResponse.success(
                    it.openUser!!.uid,
                    it.openUser.assignRoleList() ?: emptyList(),
                    mapOf(
                        "outletUser" to it,
                        "openUser" to it.openUser
                    )
                )
            }
        }
    }

}