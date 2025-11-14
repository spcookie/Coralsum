package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl
import kotlin.properties.Delegates

@ConfigurationProperties("wechat")
class WeChatConfig {

    lateinit var appId: String

    lateinit var secret: String

    lateinit var token: String

    lateinit var aesKey: String

    lateinit var staticIpProxyHost: String

    var staticIpProxyPort by Delegates.notNull<Int>()

    lateinit var staticIpProxyUser: String

    lateinit var staticIpProxyPassword: String

    val configStorage by lazy {
        WxMpDefaultConfigImpl().apply {
            appId = this@WeChatConfig.appId
            secret = this@WeChatConfig.secret
            token = this@WeChatConfig.token
            aesKey = this@WeChatConfig.aesKey
            httpProxyHost = staticIpProxyHost
            httpProxyPort = staticIpProxyPort
            httpProxyUsername = staticIpProxyUser
            httpProxyPassword = staticIpProxyPassword
        }
    }

}