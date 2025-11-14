package coralsum.service

import coralsum.service.impl.GenRequest
import coralsum.service.impl.GenResult
import io.micronaut.http.HttpRequest

interface IGenerativeImage {

    suspend fun generate(genRequest: GenRequest, request: HttpRequest<*>): GenResult

    suspend fun preview(ref: String): String

}