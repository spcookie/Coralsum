package coralsum.service

import io.micronaut.http.multipart.StreamingFileUpload
import coralsum.infrastructure.entity.IdeaCategory
import coralsum.infrastructure.entity.IdeaTag
import coralsum.infrastructure.entity.IdeaTemplate
import coralsum.common.dto.PageResp
import io.micronaut.serde.annotation.Serdeable

interface IIdeaService {
    suspend fun listCategories(): List<IdeaCategory>
    suspend fun listCategoryTree(): List<CategoryNode>
    suspend fun createCategory(name: String, description: String?, parentId: Long?): IdeaCategory
    suspend fun updateCategory(id: Long, name: String, description: String?, parentId: Long?): IdeaCategory?
    suspend fun deleteCategory(id: Long): Boolean
    suspend fun listCategoriesPage(page: Int, size: Int): PageResp<IdeaCategory>

    suspend fun listTags(): List<IdeaTag>
    suspend fun createTag(name: String): IdeaTag
    suspend fun updateTag(id: Long, name: String): IdeaTag?
    suspend fun deleteTag(id: Long): Boolean
    suspend fun listTagsPage(page: Int, size: Int): PageResp<IdeaTag>

    suspend fun listTemplates(
        categoryIds: List<Long>?,
        tagIds: List<Long>?,
        sortBy: String?,
        order: String?
    ): List<IdeaTemplate>

    suspend fun listTemplatesPage(
        categoryIds: List<Long>?,
        tagIds: List<Long>?,
        sortBy: String?,
        order: String?,
        page: Int,
        size: Int
    ): PageResp<IdeaTemplate>

    suspend fun createTemplate(
        name: String,
        description: String?,
        promptText: String,
        categoryId: Long?,
        tipsNeedImage: Boolean,
        tipsNeedProModel: Boolean,
        tipsNeedEditPrompt: Boolean,
        tagIds: List<Long>
    ): IdeaTemplate

    suspend fun updateTemplate(
        id: Long,
        name: String,
        description: String?,
        promptText: String,
        categoryId: Long?,
        tipsNeedImage: Boolean,
        tipsNeedProModel: Boolean,
        tipsNeedEditPrompt: Boolean,
        tagIds: List<Long>
    ): IdeaTemplate?

    suspend fun deleteTemplate(id: Long): Boolean

    suspend fun uploadOriginalImage(templateId: Long, file: StreamingFileUpload): String?
    suspend fun uploadEffectImage(templateId: Long, file: StreamingFileUpload): String?

    suspend fun deleteOriginalImage(templateId: Long, key: String): Boolean
    suspend fun deleteEffectImage(templateId: Long): Boolean

    suspend fun importTemplates(items: List<Map<String, Any?>>): Int

    suspend fun markTemplateUsed(id: Long): Boolean
}

@Serdeable
data class CategoryNode(
    val id: Long,
    val name: String,
    val children: List<CategoryNode> = emptyList()
)
