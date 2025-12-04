package coralsum.config

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.annotation.Introspected

@Introspected
@ConfigurationProperties("oss")
class OssConfig {
    var endpoint: String? = null
    var accessKeyId: String? = null
    var accessKeySecret: String? = null
    var bucket: String? = null
}
