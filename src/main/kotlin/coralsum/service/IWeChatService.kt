package coralsum.service

import io.micronaut.http.HttpRequest

interface IWeChatService {

    suspend fun handle(
        timestamp: String,
        nonce: String,
        signature: String,
        echostr: String?,
        encryptType: String?,
        msgSignature: String?,
        request: HttpRequest<ByteArray>
    ): String

    fun verify(timestamp: String, nonce: String, signature: String, echostr: String?): String?

}