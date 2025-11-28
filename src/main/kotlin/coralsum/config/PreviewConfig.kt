package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("preview")
class PreviewConfig {
    lateinit var secret: String
    var ttlSeconds: Long = 300
}