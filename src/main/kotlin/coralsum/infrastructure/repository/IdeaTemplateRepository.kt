package coralsum.infrastructure.repository

import coralsum.infrastructure.entity.IdeaTemplate
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.H2)
interface IdeaTemplateRepository : CoroutineCrudRepository<IdeaTemplate, Long> {
    suspend fun findByName(name: String): IdeaTemplate?

    @Query("SELECT * FROM idea_template WHERE category_id = :categoryId")
    suspend fun findByCategoryId(categoryId: Long): List<IdeaTemplate>

    @Query(
        "SELECT t.* FROM idea_template t JOIN idea_template_tag_rel r ON t.id = r.template_id WHERE r.tag_id IN (:tagIds)"
    )
    suspend fun findByTagIds(tagIds: List<Long>): List<IdeaTemplate>
}

