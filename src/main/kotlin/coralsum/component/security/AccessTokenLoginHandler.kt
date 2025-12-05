package coralsum.component.security

import coralsum.common.dto.Res
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.token.bearer.AccessRefreshTokenLoginHandler
import io.micronaut.security.token.generator.AccessRefreshTokenGenerator
import jakarta.inject.Singleton

@Replaces(AccessRefreshTokenLoginHandler::class)
@Singleton
class AccessTokenLoginHandler(
    accessRefreshTokenGenerator: AccessRefreshTokenGenerator,
) : AccessRefreshTokenLoginHandler(accessRefreshTokenGenerator) {
    override fun loginFailed(
        authenticationFailed: AuthenticationResponse,
        request: HttpRequest<*>,
    ): MutableHttpResponse<*> {
        return HttpResponse.ok(Res.fail<Any>("用户不存在或密码错误"))
    }
}