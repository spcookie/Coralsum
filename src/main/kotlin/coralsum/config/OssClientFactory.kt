package coralsum.config

import com.aliyun.oss.OSS
import com.aliyun.oss.OSSClientBuilder
import coralsum.component.excption.BusinessException
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class OssClientFactory(
    private val ossConfig: OssConfig,
) {

    @Singleton
    fun ossClient(): OSS {
        val endpoint = ossConfig.endpoint
            ?: System.getProperty("oss.endpoint")
            ?: System.getenv("OSS_ENDPOINT")
        val accessKeyId = ossConfig.accessKeyId
            ?: System.getProperty("OSS_ACCESS_KEY_ID")
            ?: System.getenv("OSS_ACCESS_KEY_ID")
        val accessKeySecret = ossConfig.accessKeySecret
            ?: System.getProperty("OSS_ACCESS_KEY_SECRET")
            ?: System.getenv("OSS_ACCESS_KEY_SECRET")
        if (endpoint.isNullOrBlank() || accessKeyId.isNullOrBlank() || accessKeySecret.isNullOrBlank()) {
            throw BusinessException("OSS配置缺失")
        }
        return OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)
    }

}
