package coralsum.convert

import coralsum.service.GenRequest
import org.mapstruct.ObjectFactory

object GenerativeObjectFactory {
    @JvmStatic
    @ObjectFactory
    fun createGenRequest(text: String): GenRequest = GenRequest(text = text)
}
