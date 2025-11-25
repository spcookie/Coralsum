package coralsum.controller

import coralsum.common.dto.Res
import coralsum.excption.BusinessException
import coralsum.toolkit.logger
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error

@Controller
class BaseController {

    private val logger = logger<BaseController>()

    @Error(BusinessException::class, global = true)
    fun globalError(e: BusinessException): HttpResponse<Res<Any>> {
        logger.error(e.message, e)
        return HttpResponse.serverError<Res<Any>>()
            .body(Res.fail(e.message))
    }
}