package coralsum.convert

import coralsum.common.response.GenResultResponse
import coralsum.common.response.GenTaskResultResponse
import coralsum.service.GenResult
import coralsum.service.GenTaskResult
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
interface GenerativeConvert {
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    fun toResponse(genResult: GenResult): GenResultResponse

    fun toResponse(genResult: GenTaskResult): GenTaskResultResponse
}