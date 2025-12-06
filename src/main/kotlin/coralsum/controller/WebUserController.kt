package coralsum.controller

import coralsum.common.dto.Res
import coralsum.common.request.UpdateLanguageRequest
import coralsum.common.request.UpdateProfileRequest
import coralsum.common.response.ProfileResponse
import coralsum.component.annotation.Debounce
import coralsum.service.IWebUserService
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api/web")
@Tag(name = "Web用户")
class WebUserController(
    private val webUserService: IWebUserService,
    private val lms: LocalizedMessageSource,
) {
    @Get("/profile")
    @Operation(summary = "获取个人档案", description = "查询当前认证用户的档案信息")
    @ApiResponse(
        responseCode = "200", description = "查询成功", content = [
            Content(schema = Schema(implementation = ProfileResponse::class))
        ]
    )
    suspend fun getProfile(authentication: Authentication): Res<ProfileResponse> {
        val uid = authentication.name ?: run {
            val msg = lms.getMessage("error.unauthorized").orElse("Unauthorized")
            return Res.fail(msg)
        }
        val resp = webUserService.profile(uid) ?: run {
            val msg = lms.getMessage("error.not_found").orElse("Not found")
            return Res.fail(msg)
        }
        return Res.success(resp)
    }

    @Put("/profile")
    @Operation(summary = "更新个人档案", description = "修改当前认证用户的档案信息（仅昵称）")
    @ApiResponse(
        responseCode = "200", description = "更新成功", content = [
            Content(schema = Schema(implementation = ProfileResponse::class))
        ]
    )
    @Debounce(name = "web.updateProfile", windowMillis = 2000, byUid = true)
    suspend fun updateProfile(authentication: Authentication, @Body req: UpdateProfileRequest): Res<ProfileResponse> {
        val uid = authentication.name ?: run {
            val msg = lms.getMessage("error.unauthorized").orElse("Unauthorized")
            return Res.fail(msg)
        }
        val ok = webUserService.updateNickName(uid, req.nickName)
        if (!ok) {
            val msg = lms.getMessage("common.error").orElse("Error")
            return Res.fail(msg)
        }
        val resp = webUserService.profile(uid) ?: run {
            val msg = lms.getMessage("error.not_found").orElse("Not found")
            return Res.fail(msg)
        }
        return Res.success(resp)
    }

    @Put("/profile/language")
    @Operation(summary = "更新语言偏好", description = "修改当前认证用户的语言设置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @Debounce(name = "web.updateLanguage", windowMillis = 2000, byUid = true)
    suspend fun updateLanguage(
        authentication: Authentication,
        @Body req: UpdateLanguageRequest,
    ): Res<Map<String, Any>> {
        val uid = authentication.name ?: run {
            val msg = lms.getMessage("error.unauthorized").orElse("Unauthorized")
            return Res.fail(msg)
        }
        val ok = webUserService.updateLanguage(uid, req.language)
        return if (ok) {
            val msg = lms.getMessage("user.profile.language.updated").orElse("Updated")
            Res.success(mapOf("message" to msg))
        } else {
            val msg = lms.getMessage("common.error").orElse("Error")
            Res.fail(msg)
        }
    }
}
