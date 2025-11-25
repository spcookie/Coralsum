package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties
import kotlin.properties.Delegates

@ConfigurationProperties("nano")
class NanoConfig {
    lateinit var apiKey: String
    lateinit var proxyHost: String
    var proxyPort by Delegates.notNull<Int>()
}