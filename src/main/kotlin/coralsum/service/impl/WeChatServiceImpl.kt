package coralsum.service.impl

import coralsum.common.dto.Res
import coralsum.common.dto.ResCode
import coralsum.common.enums.UserSource
import coralsum.common.response.GenResultResponse
import coralsum.common.response.IntentAssessmentResponse
import coralsum.config.WeChatConfig
import coralsum.infrastructure.entity.GenerateImageConf
import coralsum.infrastructure.entity.OpenUser
import coralsum.infrastructure.repository.GenerateImageConfRepository
import coralsum.infrastructure.repository.OutletUserRepository
import coralsum.service.IWeChatService
import coralsum.toolkit.logger
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.uri.UriBuilder
import io.micronaut.json.JsonMapper
import io.micronaut.reactor.http.client.ReactorHttpClient
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirst
import me.chanjar.weixin.common.session.WxSession
import me.chanjar.weixin.common.session.WxSessionManager
import me.chanjar.weixin.mp.api.WxMpMessageHandler
import me.chanjar.weixin.mp.api.WxMpMessageInterceptor
import me.chanjar.weixin.mp.api.WxMpMessageRouter
import me.chanjar.weixin.mp.api.WxMpService
import me.chanjar.weixin.mp.api.impl.WxMpServiceOkHttpImpl
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage
import org.apache.commons.lang3.StringUtils
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds


@Singleton
class WeChatServiceImpl(
    val weChatConfig: WeChatConfig,
    val generateImageConfRepository: GenerateImageConfRepository,
    val outletUserRepository: OutletUserRepository,
    val jsonMapper: JsonMapper,
    @Client("http://localhost:\${micronaut.server.port}") val httpClient: ReactorHttpClient,
) : IWeChatService {

    private val log = logger<WeChatServiceImpl>()

    private lateinit var wxMpService: WxMpService

    private lateinit var wxMpMessageRouter: WxMpMessageRouter

    @PostConstruct
    fun init() {
        val wxMpServiceOkHttpImpl = WxMpServiceOkHttpImpl()
        wxMpServiceOkHttpImpl.wxMpConfigStorage = weChatConfig.configStorage
        wxMpService = wxMpServiceOkHttpImpl
        log.info("wxMpService init")
        wxMpMessageRouter = WxMpMessageRouter(wxMpService)
        buildRouter(wxMpMessageRouter)
    }

    private fun buildRouter(router: WxMpMessageRouter) {
        router.rule()
            .async(false)
            .matcher { it.content?.startsWith("/imgset") ?: false }
            .handler(ImgSetHandler(generateImageConfRepository, outletUserRepository, jsonMapper))
            .end()
            .rule()
            .async(false)
            .matcher { it.content?.startsWith("/imgget") ?: false }
            .handler(ImgGetHandler(generateImageConfRepository, outletUserRepository))
            .end()
            .rule()
            .async(false)
            .matcher { it.content?.startsWith("/") ?: false }
            .handler(IllegalCmdHandler())
            .end()
            .rule()
            .async(false)
            .msgType("text")
            .interceptor(ImgSetInterceptor(generateImageConfRepository, outletUserRepository))
            .handler(TextHandler(httpClient))
            .end()
            .rule()
            .async(false)
            .msgType("image")
            .handler(ImageHandler())
            .end()
            .rule()
            .event("subscribe")
            .handler(SubscribeHandler(httpClient))
            .end()// oHG1U6h1EMHlm8e1qEWy0d6uI0wc
    }

    override suspend fun handle(
        timestamp: String,
        nonce: String,
        signature: String,
        echostr: String?,
        encryptType: String?,
        msgSignature: String?,
        request: HttpRequest<ByteArray>,
    ): String {
        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            // 消息签名不正确，说明不是公众平台发过来的消息
            return "非法请求"
        }

        if (StringUtils.isNotBlank(echostr)) {
            // 说明是一个仅仅用来验证的请求，回显echostr
            return echostr!!
        }
        val encryptType = if (StringUtils.isBlank(encryptType)) "raw" else encryptType

        val ctx = mapOf("coroutineContext" to currentCoroutineContext())

        if ("raw" == encryptType) {
            // 明文传输的消息
            val inMessage: WxMpXmlMessage = WxMpXmlMessage.fromXml(request.body.get().inputStream())
            val outMessage = withContext(Dispatchers.IO) {
                wxMpMessageRouter.route(inMessage, ctx)
            }
            if (outMessage == null) {
                return ""
            }
            return outMessage.toXml()
        }
        if ("aes" == encryptType) {
            // 是aes加密的消息
            val inMessage: WxMpXmlMessage = WxMpXmlMessage.fromEncryptedXml(
                request.body.get().inputStream(),
                wxMpService.wxMpConfigStorage,
                timestamp,
                nonce,
                msgSignature
            )
            val outMessage = withContext(Dispatchers.IO) {
                wxMpMessageRouter.route(inMessage, ctx)
            }
            if (outMessage == null) {
                // 为null，说明路由配置有问题，需要注意
                return ""
            }
            return outMessage.toEncryptedXml(wxMpService.wxMpConfigStorage)
        }

        return "不可识别的加密类型"
    }

    override fun verify(
        timestamp: String,
        nonce: String,
        signature: String,
        echostr: String?,
    ): String? {
        return echostr
    }

    private class TextHandler(val httpClient: ReactorHttpClient) : WxMpMessageHandler {

        private val log = logger<TextHandler>()

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("wx-text-handler-"))

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun handle(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): WxMpXmlOutMessage? {
            log.info("接收到微信文本消息：{}", wxMessage)

            val session = sessionManager.getSession(wxMessage.fromUser)

            val isBuilding = session.getAttribute("building")
            val hasResult = session.getAttribute("result")

            if (isBuilding == null && hasResult == null) {
                val res = try {
                    httpClient.retrieve(
                        HttpRequest.POST("/api/generative-image/assess-intent", wxMessage.content)
                            .contentType(MediaType.TEXT_PLAIN)
                            .basicAuth("WECHAT", wxMessage.fromUser),
                        Argument.of(
                            Res::class.java,
                            IntentAssessmentResponse::class.java
                        )
                    ).block()
                } catch (e: Exception) {
                    log.error("请求意图评估服务异常", e)
                    return WxMpXmlOutTextMessage.TEXT().content("服务器出了点小差错，请稍候再试。")
                        .fromUser(wxMessage.toUser)
                        .toUser(wxMessage.fromUser)
                        .build()
                }

                if (res != null && res.code == ResCode.SUCCESS && res.data as IntentAssessmentResponse? != null) {
                    val data = res.data
                    log.info("意图评估结果：{}", data)
                    if (!data.generateIntent) {
                        return WxMpXmlOutTextMessage.TEXT().content(data.guideMessage ?: "")
                            .fromUser(wxMessage.toUser)
                            .toUser(wxMessage.fromUser)
                            .build()
                    }
                } else {
                    return WxMpXmlOutTextMessage.TEXT().content("服务器出了点小差错，请稍候再试。")
                        .fromUser(wxMessage.toUser)
                        .toUser(wxMessage.fromUser)
                        .build()
                }
            }

            if ("取消" == wxMessage.content) {
                val image = session.getAttribute("reference_media_id")
                if (image != null) {
                    session.removeAttribute("reference_media_id")
                    return WxMpXmlOutTextMessage.TEXT().content("已取消参考图片。")
                        .fromUser(wxMessage.toUser)
                        .toUser(wxMessage.fromUser)
                        .build()
                }
                return null
            }

            // 获取全局参数
            val globalParam by lazy {
                session.getAttribute("global_param") as ParamParser? ?: ParamParser("")
            }

            // 获取上次的生成结果
            val result = getLatestResult(session, wxMessage)

            if (result != null) {
                return result
            }

            // 开始生成图片
            scope.launch {
                // 设置开始生成标识
                session.setAttribute("building", true)

                // 获取参考图片 media_id
                val mediaId = session.getAttribute("reference_media_id") as String?
                val mediaFile = try {
                    session.removeAttribute("reference_media_id")
                    if (mediaId != null) {
                        log.info("开始下载参考图片 media_id: {}", mediaId)
                        val media = wxMpService.materialService.mediaDownload(mediaId)
                        if (media != null) {
                            media
                        } else {
                            log.warn("media_id {} 下载临时资源失败", mediaId)
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    log.error("media_id {} 下载临时资源失败", mediaId, e)
                    val generate = Res.fail<Any>("参考图片下载失败。")
                    return@launch session.setAttribute("result", generate)
                }

                val generate = try {
                    val multipartBody = MultipartBody.builder()
                        .addPart("text", wxMessage.content)
                        .apply { applyParams(mediaFile, globalParam) }
                        .build()

                    val argument = Argument.of(
                        Res::class.java,
                        GenResultResponse::class.java
                    )

                    httpClient.retrieve(
                        HttpRequest.POST("/api/generative-image", multipartBody)
                            .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                            .basicAuth("WECHAT", wxMessage.fromUser),
                        argument
                    ).awaitFirst()
                } catch (e: Exception) {
                    log.error("生成失败", e)
                    Res.fail<Any>("生成失败，请稍候重试。")
                }
                session.setAttribute("result", generate)
            }

            return WxMpXmlOutTextMessage.TEXT()
                .content("正在生成中，请等待30秒后，发送任意文本消息获取结果。")
                .fromUser(wxMessage.toUser)
                .toUser(wxMessage.fromUser)
                .build()
        }

        private fun getLatestResult(
            session: WxSession,
            wxMessage: WxMpXmlMessage,
        ): WxMpXmlOutTextMessage? {
            val deferred = scope.async {
                repeat(6) {
                    val building = session.getAttribute("building") as Boolean?
                    if (building != null && building) {
                        val result = session.getAttribute("result") as Res<*>?
                        if (result != null) {
                            session.removeAttribute("building")
                            session.removeAttribute("result")
                            return@async buildMessage(wxMessage, result)
                        }
                    } else {
                        return@async null
                    }
                    delay(0.5.seconds)
                }
                return@async WxMpXmlOutTextMessage.TEXT()
                    .fromUser(wxMessage.toUser)
                    .toUser(wxMessage.fromUser)
                    .content("正在生成中，请稍后发送任意文本消息获取结果。")
                    .build()
            }

            val result = runBlocking {
                deferred.await()
            }
            return result
        }

        private fun MultipartBody.Builder.applyParams(
            mediaFile: File?,
            globalParam: ParamParser,
        ) {
            if (mediaFile != null) {
                addPart("image", "image", mediaFile)
            }
            if (globalParam has ParamKey.CC) {
                val candidateCount: String = ParamKey.CC from globalParam
                addPart("candidateCount", candidateCount)
            }
            if (globalParam has ParamKey.T) {
                val temperature: String = ParamKey.T from globalParam
                addPart("temperature", temperature)
            }
            if (globalParam has ParamKey.TP) {
                val topP: String = ParamKey.TP from globalParam
                addPart("topP", topP)
            }
            if (globalParam has ParamKey.MOT) {
                val maxOutputTokens: String = ParamKey.MOT from globalParam
                addPart("maxOutputTokens", maxOutputTokens)
            }
            if (globalParam has ParamKey.AR) {
                val aspectRatio: String = ParamKey.AR from globalParam
                addPart("aspectRatio", aspectRatio)
            }
            if (globalParam has ParamKey.F) {
                val format: String = ParamKey.F from globalParam
                addPart("format", format)
            }
            if (globalParam has ParamKey.UM) {
                val upscaylModel: String = ParamKey.UM from globalParam
                addPart("upscaylModel", upscaylModel)
            }
            if (globalParam has ParamKey.US) {
                val upscaylScale: String = ParamKey.US from globalParam
                addPart("upscaylScale", upscaylScale)
            }
        }

        private fun buildMessage(
            wxMessage: WxMpXmlMessage,
            result: Res<*>,
        ): WxMpXmlOutTextMessage? = WxMpXmlOutTextMessage.TEXT()
            .fromUser(wxMessage.toUser)
            .toUser(wxMessage.fromUser)
            .content(buildString {
                when {
                    ResCode.SUCCESS == result.code -> {
                        val response = result.data as GenResultResponse?
                        if (response != null) {
                            var hasUrl = false
                            var count = 1
                            for (url in response.images) {
                                if (url != null) {
                                    hasUrl = true
                                    appendLine("$count. $url")
                                    count += 1
                                }
                            }
                            if (hasUrl) {
                                appendLine()
                                appendLine("⚠️ 链接仅有 2 次有效访问次数，请注意保存图片。")
                            }
                        }
                    }

                    ResCode.FAIL == result.code -> appendLine(result.message)

                    else -> appendLine("生成失败")
                }
            })
            .build()
    }

    private class SubscribeHandler(
        val httpClient: ReactorHttpClient,
    ) : WxMpMessageHandler {

        companion object {
            val log = logger<SubscribeHandler>()
        }

        override fun handle(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): WxMpXmlOutMessage? {
            try {
                val uri = UriBuilder
                    .of("/api/user/subscribe")
                    .queryParam("user_source", UserSource.WECHAT)
                    .queryParam("source_code", wxMessage.fromUser)
                    .build()
                httpClient.retrieve(
                    HttpRequest.GET<Any>(uri),
                    Res::class.java
                ).block()
            } catch (e: Exception) {
                log.error("用户关注消息处理失败", e)
            }
            return null
        }
    }

    private class ImageHandler : WxMpMessageHandler {
        override fun handle(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): WxMpXmlOutMessage? {
            val session = sessionManager.getSession(wxMessage.fromUser)
            val refId = session.getAttribute("reference_media_id")
            session.setAttribute("reference_media_id", wxMessage.mediaId)
            return if (refId != null) {
                WxMpXmlOutTextMessage.TEXT()
                    .content("参考️图片已修改，请发送文本描述开始生成图片，或发送“取消”取消当前参考图片。")
                    .fromUser(wxMessage.toUser)
                    .toUser(wxMessage.fromUser)
                    .build()
            } else {
                WxMpXmlOutTextMessage.TEXT()
                    .content("图片已参考，请发送文本描述开始生成图片，或发送“取消”取消当前参考图片。")
                    .fromUser(wxMessage.toUser)
                    .toUser(wxMessage.fromUser)
                    .build()
            }
        }
    }

    private class ImgSetHandler(
        val generateImageConfRepository: GenerateImageConfRepository,
        val outletUserRepository: OutletUserRepository,
        val jsonMapper: JsonMapper,
    ) : WxMpMessageHandler {
        override fun handle(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): WxMpXmlOutMessage {
            val session = sessionManager.getSession(wxMessage.fromUser)
            val cmd = wxMessage.content.removePrefix("/imgset").trim()
            val coroutineContext = context["coroutineContext"] as CoroutineContext
            return runBlocking(coroutineContext) {
                val outletUser =
                    outletUserRepository.findBySourceCodeAndUserSource(wxMessage.fromUser, UserSource.WECHAT)
                if (outletUser == null) {
                    return@runBlocking WxMpXmlOutTextMessage.TEXT().content("请先绑定公众号。")
                        .fromUser(wxMessage.toUser)
                        .toUser(wxMessage.fromUser)
                        .build()
                }
                val openUserId = outletUser.openUser!!.id!!
                val imageConf = generateImageConfRepository.findByOpenUserId(openUserId)
                val globalParam = if (imageConf != null) {
                    ParamParser.fromJson(imageConf.conf!!)
                } else {
                    ParamParser("")
                }
                val (_, newGlobalParam) = globalParam merge ParamParser(cmd)
                val conf = GenerateImageConf(
                    id = imageConf?.id,
                    openUser = OpenUser(openUserId),
                    conf = jsonMapper.writeValueAsString(newGlobalParam.values),
                    updateBy = outletUser.openUser.uid,
                    createBy = imageConf?.createBy ?: outletUser.openUser.uid
                )
                if (conf.id == null) {
                    generateImageConfRepository.save(conf)
                } else {
                    generateImageConfRepository.update(conf)
                }
                session.setAttribute("global_param", newGlobalParam)
                return@runBlocking WxMpXmlOutTextMessage.TEXT().content("已设置生成参数。")
                    .fromUser(wxMessage.toUser)
                    .toUser(wxMessage.fromUser)
                    .build()
            }
        }
    }

    private class ImgGetHandler(
        val generateImageConfRepository: GenerateImageConfRepository,
        val outletUserRepository: OutletUserRepository,
    ) : WxMpMessageHandler {
        override fun handle(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): WxMpXmlOutMessage? {
            val coroutineContext = context["coroutineContext"] as CoroutineContext
            return runBlocking(coroutineContext) {
                val outletUser =
                    outletUserRepository.findBySourceCodeAndUserSource(wxMessage.fromUser, UserSource.WECHAT)
                if (outletUser == null) {
                    return@runBlocking null
                }
                val openUserId = outletUser.openUser!!.id!!
                val imageConf = generateImageConfRepository.findByOpenUserId(openUserId)
                val paramParser = if (imageConf != null) {
                    ParamParser.fromJson(imageConf.conf!!)
                } else {
                    ParamParser.fromJson("")
                }
                val param = paramParser.values.map {
                    "${it.key.code}: ${it.value}"
                }.joinToString("\n")
                return@runBlocking WxMpXmlOutTextMessage.TEXT()
                    .content(
                        buildString {
                            appendLine("当前生成参数：")
                            appendLine(param)
                        }
                    )
                    .fromUser(wxMessage.toUser)
                    .toUser(wxMessage.fromUser)
                    .build()
            }
        }
    }

    private class ImgSetInterceptor(
        val generateImageConfRepository: GenerateImageConfRepository,
        val outletUserRepository: OutletUserRepository,
    ) : WxMpMessageInterceptor {
        override fun intercept(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): Boolean {
            val session = sessionManager.getSession(wxMessage.fromUser)
            val coroutineContext = context["coroutineContext"] as CoroutineContext
            runBlocking(coroutineContext) {
                val outletUser =
                    outletUserRepository.findBySourceCodeAndUserSource(wxMessage.fromUser, UserSource.WECHAT)
                if (outletUser == null) {
                    return@runBlocking
                }
                val imageConf = generateImageConfRepository.findByOpenUserId(outletUser.openUser!!.id!!)
                if (imageConf != null) {
                    session.setAttribute("global_param", ParamParser.fromJson(imageConf.conf!!))
                }
            }
            return true
        }
    }

    private class IllegalCmdHandler : WxMpMessageHandler {
        override fun handle(
            wxMessage: WxMpXmlMessage,
            context: Map<String, Any>,
            wxMpService: WxMpService,
            sessionManager: WxSessionManager,
        ): WxMpXmlOutMessage {
            return WxMpXmlOutTextMessage.TEXT().content("无效的命令。")
                .fromUser(wxMessage.toUser)
                .toUser(wxMessage.fromUser)
                .build()
        }
    }

}

class ParamParser(private val input: String) {

    private lateinit var _values: Map<ParamKey, String?>

    val values: Map<ParamKey, String?> by lazy {
        if (::_values.isInitialized) _values else parse(input)
    }

    companion object {
        fun fromJson(json: String): ParamParser {
            val mapper = JsonMapper.createDefault()
            val map = mapper.readValue(json, Map::class.java)
            val input = map.entries.joinToString { (key, value) -> (key as String).lowercase() + "-" + value }
            return ParamParser(input)
        }
    }

    private constructor(values: Map<ParamKey, String?>) : this("") {
        _values = values
    }

    infix fun has(key: ParamKey): Boolean = values[key] != null

    infix fun merge(updates: ParamParser): Pair<Boolean, ParamParser> {
        val newMap = values.toMutableMap()
        updates.values.forEach { (key, value) ->
            if (value != null) {
                newMap[key] = value
            } else {
                newMap.remove(key)
            }
        }
        val that = ParamParser(newMap)
        return if (this.values == that.values) {
            false to this
        } else {
            true to that
        }
    }

    private fun parse(text: String): Map<ParamKey, String?> {
        val regex = Regex(
            """
                s-(?<s>[a-zA-Z0-9\s]*) |
                cc-(?<cc>[1-4]?) |
                t-(?<t>(?:0(?:\.\d+)?|1(?:\.0+)?)?) |
                tp-(?<tp>(?:1(?:\.\d+)?|2(?:\.0+)?)?) |
                mot-(?<mot>[0-9]{0,5}) |
                ar-(?<ar>\d*:\d*) |
                f-(?<f>png|jpg|jpeg|webp)? |
                um-(?<um>[A-Za-z0-9\-]*) |
                us-(?<us>[1-4]?)
            """.trimIndent(),
            RegexOption.COMMENTS
        )

        val groupNames = ParamKey.entries.map { it.code }.toList()
        val map = mutableMapOf<ParamKey, String?>()
        regex.findAll(text).forEach { match ->
            groupNames.forEach { name ->
                val group = match.groups[name]
                val key = ParamKey.fromCode(name) ?: return@forEach
                group?.value?.let {
                    map[key] = group.value.ifBlank { null }
                }
            }
        }

        return map
    }
}

enum class ParamKey(val code: String) {
    S("s"),           // 自然语言
    CC("cc"),         // 数字（1-4）
    T("t"),           // 小数（0-1）
    TP("tp"),         // 小数（1-2）
    MOT("mot"),       // 数字（1-32768）
    AR("ar"),         // 比例（1:2, 16:9）
    F("f"),           // 图片格式（png, jpg, webp）
    UM("um"),         // 模型名（英文字母、数字、-）
    US("us");         // 数字（2-4）

    companion object {
        fun fromCode(code: String): ParamKey? =
            entries.firstOrNull { it.code == code }
    }
}


inline infix fun <reified T> ParamKey.from(parser: ParamParser): T {
    val value = parser.values[this] ?: throw IllegalArgumentException("参数 $this 未定义")
    val converted = when (T::class) {
        Int::class -> value.toIntOrNull()
        Double::class -> value.toDoubleOrNull()
        String::class -> value
        Pair::class -> {
            val parts = value.split(":")
            if (parts.size == 2) {
                val a = parts[0].toIntOrNull()
                val b = parts[1].toIntOrNull()
                if (a != null && b != null) a to b as T else throw IllegalArgumentException("参数 $this 值错误")
            } else throw IllegalArgumentException("参数 $this 格式错误")
        }

        else -> throw IllegalArgumentException("参数 $this 类型错误")
    } as T
    return converted
}
