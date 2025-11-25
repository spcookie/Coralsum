package coralsum.common.dto

import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "统一响应包装")
@Serdeable
data class Res<T>(
    @Schema(description = "响应码") val code: ResCode,
    @Schema(description = "提示信息") val message: String?,
    @Schema(description = "业务数据") val data: T? = null,
) {
    companion object {
        fun <E> success(data: E? = null, message: String? = null): Res<E> {
            return Res(ResCode.SUCCESS, message ?: "", data)
        }

        fun <E> fail(message: String? = null, data: E? = null): Res<E> {
            return Res(ResCode.FAIL, message ?: "", data)
        }
    }
}


