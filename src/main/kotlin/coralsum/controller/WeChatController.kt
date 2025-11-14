package coralsum.controller

import coralsum.service.IWeChatService
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/api/wechat")
class WeChatController(
    val service: IWeChatService
) {

    @Get(produces = [MediaType.TEXT_XML], consumes = [MediaType.TEXT_XML])
    fun verify(
        @QueryValue timestamp: String,
        @QueryValue nonce: String,
        @QueryValue signature: String,
        @QueryValue echostr: String?,
    ): String? {
        return service.verify(timestamp, nonce, signature, echostr)
    }

    @ExecuteOn(TaskExecutors.IO)
    @Post(produces = [MediaType.TEXT_XML], consumes = [MediaType.TEXT_XML])
    suspend fun handle(
        @QueryValue timestamp: String,
        @QueryValue nonce: String,
        @QueryValue signature: String,
        @QueryValue echostr: String?,
        @QueryValue("encrypt_type") encryptType: String?,
        @QueryValue("msg_signature") msgSignature: String?,
        request: HttpRequest<ByteArray>
    ): String {
        return service.handle(timestamp, nonce, signature, echostr, encryptType, msgSignature, request)
    }
}