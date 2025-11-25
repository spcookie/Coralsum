package coralsum.controller

import coralsum.service.IWeChatService
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/wechat")
@Tag(name = "微信")
class WeChatController(
    val service: IWeChatService
) {

    @Get(produces = [MediaType.TEXT_XML], consumes = [MediaType.TEXT_XML])
    @Operation(summary = "微信服务器验证", description = "根据微信签名参数进行服务器URL验证")
    fun verify(
        @Parameter(description = "时间戳") @QueryValue timestamp: String,
        @Parameter(description = "随机数") @QueryValue nonce: String,
        @Parameter(description = "签名") @QueryValue signature: String,
        @Parameter(description = "回显字符串") @QueryValue echostr: String?,
    ): String? {
        return service.verify(timestamp, nonce, signature, echostr)
    }

    @Post(produces = [MediaType.TEXT_XML], consumes = [MediaType.TEXT_XML])
    @Operation(summary = "处理微信消息", description = "接收并处理微信服务器推送的消息或事件")
    @RequestBody(content = [Content(mediaType = MediaType.TEXT_XML)])
    suspend fun handle(
        @Parameter(description = "时间戳") @QueryValue timestamp: String,
        @Parameter(description = "随机数") @QueryValue nonce: String,
        @Parameter(description = "签名") @QueryValue signature: String,
        @Parameter(description = "回显字符串") @QueryValue echostr: String?,
        @Parameter(description = "加密方式") @QueryValue("encrypt_type") encryptType: String?,
        @Parameter(description = "消息签名") @QueryValue("msg_signature") msgSignature: String?,
        request: HttpRequest<ByteArray>,
    ): String {
        return service.handle(timestamp, nonce, signature, echostr, encryptType, msgSignature, request)
    }
}