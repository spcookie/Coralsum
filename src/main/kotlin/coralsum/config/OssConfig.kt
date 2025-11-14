package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("oss")
class OssConfig {
    lateinit var endpoint: String
}