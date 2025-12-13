package coralsum.infrastructure.repository

import coralsum.infrastructure.entity.IdeaTemplateTagRel
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface IdeaTemplateTagRelRepository : CoroutineCrudRepository<IdeaTemplateTagRel, Long> {
    @Query("SELECT * FROM idea_template_tag_rel WHERE template_id = :templateId")
    suspend fun findByTemplateId(templateId: Long): List<IdeaTemplateTagRel>

    @Query("DELETE FROM idea_template_tag_rel WHERE template_id = :templateId")
    suspend fun deleteByTemplateId(templateId: Long)
}

