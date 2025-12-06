package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.enums.UserSource
import coralsum.service.IUserService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/user")
@Tag(name = "用户")
class UserController(
    val userService: IUserService,
) {
    @Get("/subscribe")
    @Operation(summary = "登记订阅来源", description = "记录用户来源渠道与标识码")
    suspend fun userSubscribe(
        @Parameter(description = "用户来源枚举") @QueryValue("user_source") userSource: UserSource,
        @Parameter(description = "来源标识码") @QueryValue("source_code") sourceCode: String,
    ): Res<Unit> {
        userService.userSubscribe(userSource, sourceCode)
        return Res.success()
    }

}
