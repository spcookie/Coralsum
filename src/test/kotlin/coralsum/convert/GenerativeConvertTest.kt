package coralsum.convert

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers

class GenerativeConvertTest {
    private val mapper: GenerativeConvert = Mappers.getMapper(GenerativeConvert::class.java)

    @Test
    fun toRequest_withText_createsGenRequest() {
        val req = mapper.toRequest(
            "hello",
            null, null, null, null, null, null, null, null, null, null, null, null, null
        )
        assertEquals("hello", req.text)
    }
}

