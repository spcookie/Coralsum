package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("cloudflare")
class CloudflareConfig {

    lateinit var host: String

}