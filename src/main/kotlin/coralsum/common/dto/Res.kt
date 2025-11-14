package coralsum.common.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class Res<T>(
    val code: ResCode,
    val message: String?,
    val data: T? = null
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


