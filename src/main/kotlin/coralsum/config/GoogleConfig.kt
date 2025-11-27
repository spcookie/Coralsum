package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("google")
class GoogleConfig {
    lateinit var vertxApiKey: String
    lateinit var geminiApiKey: String
}