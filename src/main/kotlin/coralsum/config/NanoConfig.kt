package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("nano")
class NanoConfig {
    lateinit var apiKey: String
}