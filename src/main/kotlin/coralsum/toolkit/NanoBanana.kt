package coralsum.toolkit

import cn.hutool.core.io.FileTypeUtil
import com.google.common.collect.Lists
import com.google.genai.Client
import com.google.genai.types.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrElse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class NanoBanana(private val apiKey: String) : Closeable {

    private var client = Client.builder()
        .httpOptions(
            HttpOptions.builder()
//                .baseUrl(String.format("https://%s-aiplatform.googleapis.com", "us-central1"))
//                .baseUrl("https://aiplatform.googleapis.com")
//                .apiVersion("v1alpha")
                .timeout(5.minutes.toInt(DurationUnit.MILLISECONDS))
                .build()
        )
        .vertexAI(false)
        .apiKey(apiKey)
        .build()

    fun gen(
        text: String,
        images: List<InputStream>? = null,
        aspectRatio: String? = null,
        system: String? = null,
        temperature: Float = 1f,
        maxOutputTokens: Int = 32768,
        topP: Float = 1f,
        imageSize: String = "1K",
        mediaResolution: String = "AUTO",
        modelType: String = "BASIC",
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
//                if (images != null) {
//                    when (mediaResolution) {
//                        "AUTO" -> {
//                            mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_UNSPECIFIED)
//                        }
//
//                        "LOW" -> {
//                            mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_LOW)
//                        }
//
//                        "MEDIUM" -> {
//                            mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_MEDIUM)
//                        }
//
//                        "HIGH" -> {
//                            mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_HIGH)
//                        }
//                    }
//                }
            }
            .temperature(temperature)
            .topP(topP)
            .maxOutputTokens(maxOutputTokens)
            .imageConfig(
                ImageConfig.builder()
                    .apply {
                        if (aspectRatio != null) {
                            aspectRatio(aspectRatio)
                        }
                        if (modelType.uppercase() != "BASIC") {
                            imageSize(imageSize)
                        }
                    }
                    .build()
            )
            .apply {
                if (modelType.uppercase() != "BASIC") {
                    tools(
                        Tool.builder()
                            .googleSearch(GoogleSearch.builder().build())
                            .build()
                    )
                }
            }
            .build()
        val uploadedFiles = mutableListOf<File>()
        val contentParts: List<Part> = try {
            val parts = buildList {
                add(Part.builder().text(text).build())
                if (images != null && images.isNotEmpty()) {
                    val futures = images.map { imgStream ->
                        CompletableFuture.supplyAsync {
                            val mimeType = when (val type = FileTypeUtil.getType(imgStream)) {
                                "png" -> "image/png"
                                "jpg", "jpeg" -> "image/jpeg"
                                else -> throw IllegalArgumentException("Unsupported image type: $type")
                            }
                            val file = client.files.upload(
                                imgStream,
                                0,
                                UploadFileConfig.builder()
                                    .mimeType(mimeType)
                                    .build()
                            )
                            file
                        }
                    }
                    val files = futures.map { it.join() }
                    uploadedFiles.addAll(files)
                    files.forEach { f ->
                        add(Part.fromUri(f.name().get(), f.mimeType().get()))
                    }
                }
            }
            parts
        } catch (_: Exception) {
            buildList {
                add(Part.builder().text(text).build())
                images?.forEach { imgStream ->
                    val bytes = try {
                        imgStream.readBytes()
                    } catch (_: Exception) {
                        ByteArray(0)
                    }
                    val mimeType = when (val type = FileTypeUtil.getType(ByteArrayInputStream(bytes))) {
                        "png" -> "image/png"
                        "jpg", "jpeg" -> "image/jpeg"
                        else -> throw IllegalArgumentException("Unsupported image type: $type")
                    }
                    add(
                        Part.builder().inlineData(
                            Blob.builder().data(bytes).mimeType(mimeType).build()
                        ).build()
                    )
                }
            }
        }
        val response: GenerateContentResponse = try {
            val contentObj = Content.fromParts(*contentParts.toTypedArray())
            client.models.generateContent(
                when (modelType.uppercase()) {
                    "BASIC" -> "gemini-2.5-flash-image"
                    else -> "gemini-3-pro-image-preview"
                },
                contentObj,
                contentConfig
            )
        } finally {
            uploadedFiles.forEach { f ->
                try {
                    client.files.delete(f.name().get(), null)
                } catch (_: Exception) {
                }
            }
        }
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
