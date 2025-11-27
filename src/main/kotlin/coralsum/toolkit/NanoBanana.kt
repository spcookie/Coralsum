package coralsum.toolkit

import cn.hutool.core.io.FileTypeUtil
import com.google.common.collect.Lists
import com.google.genai.Client
import com.google.genai.types.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.Closeable
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrElse

class NanoBanana(private val apiKey: String) : Closeable {

    private var client = Client.builder()
        .httpOptions(
            HttpOptions.builder()
//                .baseUrl(String.format("https://%s-aiplatform.googleapis.com", "us-central1"))
//                .baseUrl("https://aiplatform.googleapis.com")
                .apiVersion("v1")
                .build()
        )
        .vertexAI(true)
        .apiKey(apiKey)
//        .project("1046708816451")
//        .location("global")
        .build()

    fun gen(
        text: String,
        image: ByteArray? = null,
        aspectRatio: String? = null,
        system: String? = null,
        temperature: Float = 1f,
        maxOutputTokens: Int = 32768,
        topP: Float = 1f,
        imageSize: String = "1K",
    ): Pair<Pair<String?, BufferedImage?>, GenerateContentResponseUsageMetadata> {
        val contentConfig = GenerateContentConfig.builder()
            .responseModalities("TEXT", "IMAGE")
            .apply {
                if (system != null) {
                    systemInstruction(
                        Content.builder()
                            .parts(
                                Part.builder()
                                    .text(system)
                                    .build()
                            )
                            .build()
                    )
                }
            }
            .mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_HIGH)
            .temperature(temperature)
            .topP(topP)
            .maxOutputTokens(maxOutputTokens)
            .imageConfig(
                ImageConfig.builder()
                    .apply {
                        if (aspectRatio != null) {
                            aspectRatio(aspectRatio)
                        }
                        imageSize(imageSize)
                    }
                    .build()
            )
            .build()
        val response = client.models.generateContent(
            "gemini-3-pro-image-preview",
            Content.builder()
                .parts(
                    buildList {
                        add(
                            Part.builder().text(text).build()
                        )
                        if (image != null) {
                            add(
                                Part.builder().inlineData(
                                    Blob.builder().data(image)
                                        .mimeType("image/" + FileTypeUtil.getType(image.inputStream())).build()
                                ).build()
                            )
                        }
                    }).role("user").build(), contentConfig
        )
        val result = response.candidates()
            .map { candidates ->
                candidates.stream()
                    .map { candidate ->
                        candidate.content()
                            .flatMap { content -> content.parts() }
                            .getOrElse { Lists.newArrayList() }
                    }
                    .findFirst()
                    .getOrElse { listOf() }
            }
            .map { parts ->
                var text: String? = null
                var image: BufferedImage? = null
                for (part in parts) {
                    if (part.text().isPresent) {
                        text = part.text().get()
                    } else if (part.inlineData().isPresent) {
                        image = ImageIO.read(
                            ByteArrayInputStream(
                                part.inlineData().flatMap { obj -> obj.data() }.get()
                            )
                        )
                    }
                }
                Pair(text, image)
            }
            .getOrElse { Pair(null, null) }
        val usage = response.usageMetadata().get()
        return Pair(result, usage)
    }

    override fun close() {
        client.close()
    }
}