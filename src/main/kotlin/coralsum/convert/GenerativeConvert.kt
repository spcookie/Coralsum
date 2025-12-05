package coralsum.convert

import coralsum.common.enums.*
import coralsum.common.response.GenResultResponse
import coralsum.common.response.GenTaskResultResponse
import coralsum.common.response.IntentAssessmentResponse
import coralsum.service.GenRequest
import coralsum.service.GenResult
import coralsum.service.GenTaskResult
import coralsum.service.IntentAssessment
import org.mapstruct.*

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
interface GenerativeConvert {
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    fun toResponse(genResult: GenResult): GenResultResponse

    fun toResponse(genResult: GenTaskResult): GenTaskResultResponse

    fun toResponse(assessment: IntentAssessment): IntentAssessmentResponse


    @Mapping(target = "imageSessionId", source = "sid")
    @Mapping(
        target = "modelType",
        expression = "java(modelType != null ? modelType : coralsum.common.enums.ModelType.BASIC)"
    )
    @Mapping(target = "candidateCount", expression = "java(candidateCount != null ? candidateCount : 1)")
    @Mapping(
        target = "aspectRatio",
        expression = "java(aspectRatio != null ? aspectRatio : coralsum.common.enums.AspectRatio.R1_1)"
    )
    @Mapping(target = "system", source = "system")
    @Mapping(target = "temperature", expression = "java(temperature != null ? temperature : 0.5f)")
    @Mapping(target = "maxOutputTokens", expression = "java(maxOutputTokens != null ? maxOutputTokens : 32768)")
    @Mapping(target = "topP", expression = "java(topP != null ? topP : 1f)")
    @Mapping(target = "format", expression = "java(format != null ? format : coralsum.common.enums.ImageFormat.PNG)")
    @Mapping(
        target = "upscaylModel",
        expression = "java(upscaylModel != null ? upscaylModel : coralsum.common.enums.UpscaylModel.HIGH_FIDELITY_4X)"
    )
    @Mapping(
        target = "upscaylScale",
        expression = "java(upscaylScale != null ? upscaylScale : coralsum.common.enums.UpscaylScale.X1)"
    )
    @Mapping(
        target = "imageSize",
        expression = "java(imageSize != null ? imageSize : coralsum.common.enums.ImageSize.X1)"
    )
    @Mapping(
        target = "mediaResolution",
        expression = "java(mediaResolution != null ? mediaResolution : coralsum.common.enums.MediaResolution.AUTO)"
    )
    fun toRequest(
        text: String,
        sid: String?,
        modelType: ModelType?,
        candidateCount: Int?,
        aspectRatio: AspectRatio?,
        system: String?,
        temperature: Float?,
        maxOutputTokens: Int?,
        topP: Float?,
        format: ImageFormat?,
        upscaylModel: UpscaylModel?,
        upscaylScale: UpscaylScale?,
        imageSize: ImageSize?,
        mediaResolution: MediaResolution?,
    ): GenRequest

    @org.mapstruct.ObjectFactory
    fun createGenRequest(text: String): GenRequest = GenRequest(text = text)
}
