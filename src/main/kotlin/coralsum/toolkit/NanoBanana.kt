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
                .baseUrl(String.format("https://%s-aiplatform.googleapis.com", "us-central1"))
                .apiVersion("v1")
                .build()
        )
        .vertexAI(true)
        .apiKey(apiKey)
        .build()

    fun gen(
        text: String,
        image: ByteArray? = null,
        candidateCount: Int = 1,
        aspectRatio: String? = null,
        system: String? = null,
        temperature: Float = 1f,
        maxOutputTokens: Int = 32768,
        topP: Float = 1f
    ): Pair<List<Pair<String?, BufferedImage>>, GenerateContentResponseUsageMetadata> {
        val contentConfig = GenerateContentConfig.builder()
            .responseModalities("TEXT", "IMAGE").apply {
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
            .mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_HIGH).apply {
                if (aspectRatio != null) {
                    imageConfig(
                        ImageConfig.builder()
                            .aspectRatio(aspectRatio)
                            .build()
                    )
                }
            }
            .candidateCount(candidateCount)
            .temperature(temperature)
            .topP(topP)
            .maxOutputTokens(maxOutputTokens)
            .build()
        val response = client.models.generateContent(
            "gemini-2.5-flash-image", Content.builder().parts(
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
                    .toList()
            }
            .map { candidates ->
                val group: List<Pair<String?, BufferedImage>> = buildList {
                    for (parts in candidates) {
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
                        add(Pair(text, image!!))
                    }
                }
                group
            }
            .getOrElse { listOf() }
        val usage = response.usageMetadata().get()
        return Pair(result, usage)
    }

    override fun close() {
        client.close()
    }
}