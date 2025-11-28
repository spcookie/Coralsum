package coralsum.common.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PageResp<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
)