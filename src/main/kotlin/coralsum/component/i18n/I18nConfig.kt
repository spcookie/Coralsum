package coralsum.component.i18n

import io.micronaut.context.MessageSource
import io.micronaut.context.annotation.Factory
import io.micronaut.context.i18n.ResourceBundleMessageSource
import jakarta.inject.Singleton

@Factory
class I18nConfig {
    // 注册 MessageSource，基于 ResourceBundle，指向 i18n/messages，使用英文为默认兜底
    @Singleton
    fun messageSource(): MessageSource {
        return ResourceBundleMessageSource("i18n.messages")
    }
}

