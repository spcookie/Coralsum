package coralsum.service.impl

import com.aliyun.oss.OSS
import com.aliyun.oss.model.ObjectMetadata
import coralsum.component.excption.BusinessException
import coralsum.config.OssConfig
import coralsum.infrastructure.entity.IdeaCategory
import coralsum.infrastructure.entity.IdeaTag
import coralsum.infrastructure.entity.IdeaTemplate
import coralsum.infrastructure.entity.IdeaTemplateTagRel
import coralsum.infrastructure.repository.IdeaCategoryRepository
import coralsum.infrastructure.repository.IdeaTagRepository
import coralsum.infrastructure.repository.IdeaTemplateRepository
import coralsum.infrastructure.repository.IdeaTemplateTagRelRepository
import coralsum.service.IIdeaService
import coralsum.common.dto.PageResp
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.*

@Singleton
class IdeaServiceImpl(
    private val categoryRepo: IdeaCategoryRepository,
    private val tagRepo: IdeaTagRepository,
    private val templateRepo: IdeaTemplateRepository,
    private val relRepo: IdeaTemplateTagRelRepository,
    private val ossConfig: OssConfig,
    private val oss: OSS,
    private val securityService: SecurityService,
) : IIdeaService {

    override suspend fun listCategories(): List<IdeaCategory> = categoryRepo.findAll().toList()

    override suspend fun listCategoriesPage(page: Int, size: Int): PageResp<IdeaCategory> {
        val all = categoryRepo.findAll().toList()
        return pageOf(all, page, size)
    }

    override suspend fun listCategoryTree(): List<coralsum.service.CategoryNode> {
        val all = categoryRepo.findAll().toList()
        val byParent = mutableMapOf<Long?, MutableList<IdeaCategory>>()
        for (c in all) {
            val pid = c.parentId
            byParent.computeIfAbsent(pid) { mutableListOf() }.add(c)
        }
        fun build(pid: Long?): List<coralsum.service.CategoryNode> {
            val children = byParent[pid] ?: mutableListOf()
            val nodes = mutableListOf<coralsum.service.CategoryNode>()
            for (ch in children.sortedBy { it.name }) {
                val node = coralsum.service.CategoryNode(
                    id = ch.id!!,
                    name = ch.name,
                    children = build(ch.id!!)
                )
                nodes.add(node)
            }
            return nodes
        }
        return build(null)
    }

    override suspend fun createCategory(name: String, description: String?, parentId: Long?): IdeaCategory {
        val exist = categoryRepo.findByName(name)
        if (exist != null) throw BusinessException("分类已存在")
        return categoryRepo.save(IdeaCategory(name = name, description = description, parentId = parentId))
    }

    override suspend fun updateCategory(id: Long, name: String, description: String?, parentId: Long?): IdeaCategory? {
        val po = categoryRepo.findById(id) ?: return null
        po.name = name
        po.description = description
        po.parentId = parentId
        return categoryRepo.update(po)
    }

    override suspend fun deleteCategory(id: Long): Boolean {
        val po = categoryRepo.findById(id) ?: return false
        val all = categoryRepo.findAll().toList()
        val hasChildren = all.any { it.parentId == id }
        if (hasChildren) throw BusinessException("分类存在子类目，无法删除")
        val anyRef = templateRepo.findAll().toList().any { it.categoryId == id }
        if (anyRef) throw BusinessException("分类被模板引用，无法删除")
        categoryRepo.delete(po)
        return true
    }

    override suspend fun listTags(): List<IdeaTag> = tagRepo.findAll().toList()

    override suspend fun listTagsPage(page: Int, size: Int): PageResp<IdeaTag> {
        val all = tagRepo.findAll().toList()
        return pageOf(all, page, size)
    }

    override suspend fun createTag(name: String): IdeaTag {
        val exist = tagRepo.findByName(name)
        if (exist != null) throw BusinessException("标签已存在")
        return tagRepo.save(IdeaTag(name = name))
    }

    override suspend fun updateTag(id: Long, name: String): IdeaTag? {
        val po = tagRepo.findById(id) ?: return null
        po.name = name
        return tagRepo.update(po)
    }

    override suspend fun deleteTag(id: Long): Boolean {
        val po = tagRepo.findById(id) ?: return false
        tagRepo.delete(po)
        return true
    }

    override suspend fun listTemplates(
        categoryIds: List<Long>?,
        tagIds: List<Long>?,
        sortBy: String?,
        order: String?
    ): List<IdeaTemplate> {
        val all = templateRepo.findAll()
        var list = all.toList()
        if (!categoryIds.isNullOrEmpty()) {
            val allCats = categoryRepo.findAll().toList()
            val childrenMap = mutableMapOf<Long?, MutableList<IdeaCategory>>()
            for (c in allCats) {
                childrenMap.computeIfAbsent(c.parentId) { mutableListOf() }.add(c)
            }
            fun collectDescendants(ids: Set<Long>): Set<Long> {
                val result = mutableSetOf<Long>()
                val stack = ArrayDeque<Long>()
                ids.forEach { stack.add(it) }
                while (stack.isNotEmpty()) {
                    val cur = stack.removeFirst()
                    result.add(cur)
                    val kids = childrenMap[cur]?.mapNotNull { it.id } ?: emptyList()
                    for (k in kids) stack.add(k)
                }
                return result
            }

            val base = categoryIds.filterNotNull().toSet()
            val expanded = collectDescendants(base)
            list = list.filter { t -> t.categoryId != null && expanded.contains(t.categoryId!!) }
        }
        if (!tagIds.isNullOrEmpty()) {
            val ids = tagIds.toSet()
            val relMap = mutableMapOf<Long, List<IdeaTemplateTagRel>>()
            for (t in list) {
                val tid = t.id ?: continue
                relMap[tid] = relRepo.findByTemplateId(tid)
            }
            list = list.filter { t ->
                val tid = t.id ?: return@filter false
                val rels = relMap[tid] ?: emptyList()
                rels.any { ids.contains(it.tagId) }
            }
        }
        val desc = (order?.lowercase() ?: "desc") == "desc"
        list = when (sortBy?.lowercase()) {
            "updated" -> if (desc) list.sortedByDescending { it.updateTime } else list.sortedBy { it.updateTime }
            "hot", "usage" -> list.sortedWith(compareByDescending<IdeaTemplate> { it.usageCount }
                .thenByDescending { it.lastUsedTime })

            else -> list.sortedByDescending { it.updateTime }
        }
        val now = LocalDateTime.now()
        val newDays = 7L
        val hotCount = 20
        for (t in list) {
            val ts = t.updateTime ?: t.createTime
            val isNew = if (ts != null) java.time.Duration.between(ts, now).toDays() <= newDays else false
            val isHot = (t.usageCount) >= hotCount
            t.isNew = isNew
            t.isHot = isHot
            val cid = t.categoryId
            if (cid != null) {
                val allCats = runCatching { categoryRepo.findAll().toList() }.getOrNull() ?: emptyList()
                val byId = allCats.associateBy { it.id }
                val names = mutableListOf<String>()
                var cur: IdeaCategory? = byId[cid]
                while (cur != null) {
                    names.add(cur.name)
                    cur = byId[cur.parentId]
                }
                names.reverse()
                t.categoryName = names.lastOrNull()
                t.categoryPath = names
            }
            val tid = t.id
            if (tid != null) {
                val rels = runCatching { relRepo.findByTemplateId(tid) }.getOrNull() ?: emptyList()
                val names = mutableListOf<String>()
                val ids = mutableListOf<Long>()
                for (r in rels) {
                    ids.add(r.tagId)
                    val tag = runCatching { tagRepo.findById(r.tagId) }.getOrNull()
                    tag?.name?.let { names.add(it) }
                }
                t.tagNames = names
                t.tagIds = ids
            }
        }
        return list
    }

    override suspend fun listTemplatesPage(
        categoryIds: List<Long>?,
        tagIds: List<Long>?,
        sortBy: String?,
        order: String?,
        page: Int,
        size: Int
    ): PageResp<IdeaTemplate> {
        val list = listTemplates(categoryIds, tagIds, sortBy, order)
        return pageOf(list, page, size)
    }

    override suspend fun createTemplate(
        name: String,
        description: String?,
        promptText: String,
        categoryId: Long?,
        tipsNeedImage: Boolean,
        tipsNeedProModel: Boolean,
        tipsNeedEditPrompt: Boolean,
        tagIds: List<Long>
    ): IdeaTemplate {
        val t = templateRepo.save(
            IdeaTemplate(
                name = name,
                description = description,
                promptText = promptText,
                categoryId = categoryId,
                tipsNeedImage = tipsNeedImage,
                tipsNeedProModel = tipsNeedProModel,
                tipsNeedEditPrompt = tipsNeedEditPrompt
            )
        )
        for (tid in tagIds.distinct()) {
            relRepo.save(IdeaTemplateTagRel(templateId = t.id!!, tagId = tid))
        }
        return t
    }

    override suspend fun updateTemplate(
        id: Long,
        name: String,
        description: String?,
        promptText: String,
        categoryId: Long?,
        tipsNeedImage: Boolean,
        tipsNeedProModel: Boolean,
        tipsNeedEditPrompt: Boolean,
        tagIds: List<Long>
    ): IdeaTemplate? {
        val po = templateRepo.findById(id) ?: return null
        po.name = name
        po.description = description
        po.promptText = promptText
        po.categoryId = categoryId
        po.tipsNeedImage = tipsNeedImage
        po.tipsNeedProModel = tipsNeedProModel
        po.tipsNeedEditPrompt = tipsNeedEditPrompt
        val saved = templateRepo.update(po)
        relRepo.deleteByTemplateId(id)
        for (tid in tagIds.distinct()) {
            relRepo.save(IdeaTemplateTagRel(templateId = id, tagId = tid))
        }
        return saved
    }

    override suspend fun deleteTemplate(id: Long): Boolean {
        val po = templateRepo.findById(id) ?: return false
        val originals = runCatching { parseJsonArray(po.originalImageRefs) }.getOrNull() ?: mutableListOf()
        for (key in originals) {
            runCatching { oss.deleteObject(resolveBucket(), key) }
        }
        val eff = po.effectImageRef
        if (!eff.isNullOrBlank()) {
            runCatching { oss.deleteObject(resolveBucket(), eff) }
        }
        templateRepo.delete(po)
        relRepo.deleteByTemplateId(id)
        return true
    }

    override suspend fun uploadOriginalImage(templateId: Long, file: StreamingFileUpload): String? {
        val po = templateRepo.findById(templateId) ?: throw BusinessException("idea.error.template_not_found")
        val (uri, size) = withContext(Dispatchers.IO) {
            val ins = runCatching { file.asInputStream() }.getOrNull() ?: return@withContext null
            val bytes = runCatching { ins.readAllBytes() }.getOrNull() ?: return@withContext null
            if (bytes.size > 1_000_000) throw BusinessException("idea.error.image_too_large")
            val ext = guessExt(file.filename)
            val key = "idea/original/${templateId}/${UUID.randomUUID()}.$ext"
            val metadata = ObjectMetadata()
            metadata.contentDisposition = "attachment; filename=${file.filename}"
            metadata.contentType = mimeByExt(ext)
            oss.putObject(resolveBucket(), key, ByteArrayInputStream(bytes), metadata)
            Pair(key, bytes.size)
        } ?: return null
        val list = runCatching { parseJsonArray(po.originalImageRefs) }.getOrNull() ?: mutableListOf<String>()
        if (list.size >= 10) throw BusinessException("idea.error.original_image_limit")
        list.add(uri)
        po.originalImageRefs = toJsonArray(list)
        templateRepo.update(po)
        return uri
    }

    override suspend fun uploadEffectImage(templateId: Long, file: StreamingFileUpload): String? {
        val po = templateRepo.findById(templateId) ?: throw BusinessException("idea.error.template_not_found")
        val uri = withContext(Dispatchers.IO) {
            val ins = runCatching { file.asInputStream() }.getOrNull() ?: return@withContext null
            val bytes = runCatching { ins.readAllBytes() }.getOrNull() ?: return@withContext null
            if (bytes.size > 1_000_000) throw BusinessException("idea.error.image_too_large")
            val ext = guessExt(file.filename)
            val key = "idea/effect/${templateId}/${UUID.randomUUID()}.$ext"
            val metadata = ObjectMetadata()
            metadata.contentDisposition = "attachment; filename=${file.filename}"
            metadata.contentType = mimeByExt(ext)
            oss.putObject(resolveBucket(), key, ByteArrayInputStream(bytes), metadata)
            key
        } ?: return null
        po.effectImageRef = uri
        templateRepo.update(po)
        return uri
    }

    override suspend fun deleteOriginalImage(templateId: Long, key: String): Boolean {
        val po = templateRepo.findById(templateId) ?: return false
        val list = runCatching { parseJsonArray(po.originalImageRefs) }.getOrNull() ?: mutableListOf()
        if (!list.remove(key)) return false
        po.originalImageRefs = toJsonArray(list)
        templateRepo.update(po)
        runCatching { oss.deleteObject(resolveBucket(), key) }
        return true
    }

    override suspend fun deleteEffectImage(templateId: Long): Boolean {
        val po = templateRepo.findById(templateId) ?: return false
        val key = po.effectImageRef ?: return false
        po.effectImageRef = null
        templateRepo.update(po)
        runCatching { oss.deleteObject(resolveBucket(), key) }
        return true
    }

    override suspend fun importTemplates(items: List<Map<String, Any?>>): Int {
        var cnt = 0
        for (it in items) {
            val name = (it["name"] as? String)?.trim().orEmpty()
            val prompt = (it["prompt"] as? String)?.trim().orEmpty()
            if (name.isBlank() || prompt.isBlank()) continue
            val desc = (it["description"] as? String)?.trim()
            val category = (it["category"] as? String)?.trim()
            val tipsNeedImage = it["tipsNeedImage"] as? Boolean ?: false
            val tipsNeedProModel = it["tipsNeedProModel"] as? Boolean ?: false
            val tipsNeedEditPrompt = it["tipsNeedEditPrompt"] as? Boolean ?: false
            val rawTags = (it["tags"] as? List<Any?>) ?: emptyList()
            val tagNames = rawTags.mapNotNull { x -> x?.toString()?.trim() }.filter { s -> s.isNotBlank() }

            val categoryId = if (!category.isNullOrBlank()) {
                val c0 = categoryRepo.findByName(category)
                (c0 ?: categoryRepo.save(IdeaCategory(name = category))).id
            } else null

            val tagIds = mutableListOf<Long>()
            for (tn in tagNames) {
                val t0 = tagRepo.findByName(tn) ?: tagRepo.save(IdeaTag(name = tn))
                t0.id?.let { tagIds.add(it) }
            }

            createTemplate(
                name = name,
                description = desc,
                promptText = prompt,
                categoryId = categoryId,
                tipsNeedImage = tipsNeedImage,
                tipsNeedProModel = tipsNeedProModel,
                tipsNeedEditPrompt = tipsNeedEditPrompt,
                tagIds = tagIds
            )
            cnt++
        }
        return cnt
    }

    override suspend fun markTemplateUsed(id: Long): Boolean {
        val po = templateRepo.findById(id) ?: return false
        po.usageCount = (po.usageCount) + 1
        po.lastUsedTime = LocalDateTime.now()
        templateRepo.update(po)
        return true
    }

    private fun resolveBucket(): String {
        val bucket = ossConfig.bucket
        if (bucket.isNullOrBlank()) throw BusinessException("oss.error.bucket_missing")
        return bucket
    }

    private fun guessExt(filename: String?): String {
        val f = (filename ?: "").lowercase()
        return when {
            f.endsWith(".png") -> "png"
            f.endsWith(".jpg") || f.endsWith(".jpeg") -> "jpg"
            f.endsWith(".webp") -> "webp"
            else -> "png"
        }
    }

    private fun mimeByExt(ext: String): String = when (ext.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }

    private fun parseJsonArray(json: String?): MutableList<String> {
        val raw = (json ?: "").trim()
        if (raw.isBlank()) return mutableListOf()
        return try {
            val arr = io.micronaut.json.JsonMapper.createDefault().readValue(raw, Array<String>::class.java)
            arr?.toMutableList() ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun toJsonArray(list: List<String>): String {
        return try {
            io.micronaut.json.JsonMapper.createDefault().writeValueAsString(list)
        } catch (_: Exception) {
            "[]"
        }
    }

    private fun <T> pageOf(all: List<T>, page: Int, size: Int): PageResp<T> {
        val p = if (page <= 0) 1 else page
        val s = when {
            size <= 0 -> 10
            size > 100 -> 100
            else -> size
        }
        val start = (p - 1) * s
        val items = if (start >= all.size) emptyList() else all.subList(start, kotlin.math.min(start + s, all.size))
        return PageResp(items = items, page = p, size = s, total = all.size.toLong())
    }
}
