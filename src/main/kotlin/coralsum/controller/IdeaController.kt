package coralsum.controller

import coralsum.common.dto.Res
import com.aliyun.oss.OSS
import com.aliyun.oss.HttpMethod
import com.aliyun.oss.model.GeneratePresignedUrlRequest
import coralsum.config.OssConfig
import coralsum.config.PreviewConfig
import coralsum.component.excption.BusinessException
import coralsum.service.IIdeaService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Validated
@Controller("/api/ideas")
@Tag(name = "创意管理")
@Secured("CTL")
class IdeaController(
    private val service: IIdeaService,
    private val oss: OSS,
    private val ossConfig: OssConfig,
    private val previewConfig: PreviewConfig,
) {

    @Get("/categories")
    @Operation(summary = "分类列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listCategories() = Res.success(service.listCategories())

    @Get("/categories/page")
    @Operation(summary = "分类分页列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listCategoriesPage(
        @QueryValue("page") page: Int?,
        @QueryValue("size") size: Int?
    ) = Res.success(service.listCategoriesPage(page ?: 1, size ?: 10))

    @Get("/categories/tree")
    @Operation(summary = "分类树形列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listCategoryTree() = Res.success(service.listCategoryTree())

    @Serdeable
    @Introspected
    data class CategoryReq(val id: Long?, val name: String, val description: String?, val parentId: Long?)

    @Post("/category")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "创建分类")
    suspend fun createCategory(@Body req: CategoryReq) =
        Res.success(service.createCategory(req.name, req.description, req.parentId))

    @Put("/category")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "更新分类")
    suspend fun updateCategory(@Body req: CategoryReq) =
        Res.success(service.updateCategory(req.id!!, req.name, req.description, req.parentId))

    @Delete("/category/{id}")
    @Operation(summary = "删除分类")
    suspend fun deleteCategory(@PathVariable id: Long) = Res.success(service.deleteCategory(id))

    @Get("/tags")
    @Operation(summary = "标签列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listTags() = Res.success(service.listTags())

    @Get("/tags/page")
    @Operation(summary = "标签分页列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listTagsPage(
        @QueryValue("page") page: Int?,
        @QueryValue("size") size: Int?
    ) = Res.success(service.listTagsPage(page ?: 1, size ?: 10))

    @Serdeable
    @Introspected
    data class TagReq(val id: Long?, val name: String)

    @Post("/tag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "创建标签")
    suspend fun createTag(@Body req: TagReq) = Res.success(service.createTag(req.name))

    @Put("/tag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "更新标签")
    suspend fun updateTag(@Body req: TagReq) = Res.success(service.updateTag(req.id!!, req.name))

    @Delete("/tag/{id}")
    @Operation(summary = "删除标签")
    suspend fun deleteTag(@PathVariable id: Long) = Res.success(service.deleteTag(id))

    @Get("/templates")
    @Operation(summary = "模版列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listTemplates(
        @QueryValue("categoryId") categoryId: Long?,
        @QueryValue("categoryIds") categoryIdsRaw: String?,
        @QueryValue("tagIds") tagIdsRaw: String?,
        @QueryValue("sortBy") sortBy: String?,
        @QueryValue("order") order: String?,
    ) = Res.success(
        service.listTemplates(
            mergeCategoryIds(categoryId, categoryIdsRaw),
            parseIds(tagIdsRaw),
            sortBy,
            order
        )
    )

    @Get("/templates/page")
    @Operation(summary = "模版分页列表")
    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    suspend fun listTemplatesPage(
        @QueryValue("categoryId") categoryId: Long?,
        @QueryValue("categoryIds") categoryIdsRaw: String?,
        @QueryValue("tagIds") tagIdsRaw: String?,
        @QueryValue("sortBy") sortBy: String?,
        @QueryValue("order") order: String?,
        @QueryValue("page") page: Int?,
        @QueryValue("size") size: Int?,
    ) = Res.success(
        service.listTemplatesPage(
            mergeCategoryIds(categoryId, categoryIdsRaw),
            parseIds(tagIdsRaw),
            sortBy,
            order,
            page ?: 1,
            size ?: 10
        )
    )

    @Serdeable
    @Introspected
    data class TemplateReq(
        val id: Long?,
        val name: String,
        val description: String?,
        val promptText: String,
        val categoryId: Long?,
        val tipsNeedImage: Boolean,
        val tipsNeedProModel: Boolean,
        val tipsNeedEditPrompt: Boolean,
        val tagIds: List<Long>
    )

    @Post("/template")
    @Operation(summary = "创建模版")
    suspend fun createTemplate(@Body req: TemplateReq) = Res.success(
        service.createTemplate(
            name = req.name,
            description = req.description,
            promptText = req.promptText,
            categoryId = req.categoryId,
            tipsNeedImage = req.tipsNeedImage,
            tipsNeedProModel = req.tipsNeedProModel,
            tipsNeedEditPrompt = req.tipsNeedEditPrompt,
            tagIds = req.tagIds
        )
    )

    @Put("/template/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "更新模版")
    suspend fun updateTemplate(@PathVariable id: Long, @Body req: TemplateReq) = Res.success(
        service.updateTemplate(
            id = id,
            name = req.name,
            description = req.description,
            promptText = req.promptText,
            categoryId = req.categoryId,
            tipsNeedImage = req.tipsNeedImage,
            tipsNeedProModel = req.tipsNeedProModel,
            tipsNeedEditPrompt = req.tipsNeedEditPrompt,
            tagIds = req.tagIds
        )
    )

    @Delete("/template/{id}")
    @Operation(summary = "删除模版")
    suspend fun deleteTemplate(@PathVariable id: Long) = Res.success(service.deleteTemplate(id))

    @Post("/templates/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "批量导入模版（不含图片）")
    suspend fun importTemplates(@Body items: List<Map<String, Any?>>) = Res.success(service.importTemplates(items))

    @Post("/template/{id}/upload-original")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "上传原图（单张，最多10张，<=1M）")
    suspend fun uploadOriginal(@PathVariable id: Long, image: StreamingFileUpload) = try {
        Res.success(service.uploadOriginalImage(id, image))
    } catch (e: BusinessException) {
        Res.fail(e.message)
    }

    @Post("/template/{id}/upload-effect")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "上传效果图（最多1张，<=1M）")
    suspend fun uploadEffect(@PathVariable id: Long, image: StreamingFileUpload) = try {
        Res.success(service.uploadEffectImage(id, image))
    } catch (e: BusinessException) {
        Res.fail(e.message)
    }

    @Delete("/template/{id}/original")
    @Operation(summary = "删除原图（按key）")
    suspend fun deleteOriginal(@PathVariable id: Long, @QueryValue("key") key: String) =
        Res.success(service.deleteOriginalImage(id, key))

    @Delete("/template/{id}/effect")
    @Operation(summary = "删除效果图")
    suspend fun deleteEffect(@PathVariable id: Long) = Res.success(service.deleteEffectImage(id))

    @Post("/template/{id}/use")
    @Operation(summary = "记录模版使用")
    suspend fun markUsed(@PathVariable id: Long) = Res.success(service.markTemplateUsed(id))

    private fun parseIds(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { it.trim().toLongOrNull() }.toList()
    }

    private fun mergeCategoryIds(single: Long?, raw: String?): List<Long> {
        val list = parseIds(raw).toMutableList()
        if (single != null) list.add(single)
        return list.distinct()
    }

    @Secured(io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS)
    @Get("/image-url")
    @Operation(summary = "获取一次性图片URL", description = "为 IDEA 模版图片（原图/效果图）生成 OSS 预签名一次性链接")
    suspend fun imageUrl(@QueryValue("key") key: String): Res<String?> {
        val expiration = java.util.Date(System.currentTimeMillis() + previewConfig.ttlSeconds * 1000)
        val req = GeneratePresignedUrlRequest(resolveBucket(), key, HttpMethod.GET)
        req.expiration = expiration
        val url = oss.generatePresignedUrl(req)
        return Res.success(url?.toString())
    }

    private fun resolveBucket(): String {
        val bucket = ossConfig.bucket
        if (bucket.isNullOrBlank()) throw coralsum.component.excption.BusinessException("OSS桶未配置")
        return bucket
    }
}
