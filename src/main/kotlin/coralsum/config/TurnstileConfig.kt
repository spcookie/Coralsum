package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("turnstile")
class TurnstileConfig {
    var secret: String? = null
}

