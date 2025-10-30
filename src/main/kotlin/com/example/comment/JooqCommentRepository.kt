package com.example.comment

import com.example.jooq.public.tables.references.COMMENTS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jooq.DSLContext

@ApplicationScoped
class JooqCommentRepository : CommentRepository {
    @Inject
    lateinit var dsl: DSLContext

    override fun create(entity: Comment): Comment {
        require(entity.id == null) { "Cannot create entity with existing ID" }

        val id =
            dsl
                .insertInto(COMMENTS)
                .set(COMMENTS.ARTICLE_ID, entity.articleId)
                .set(COMMENTS.AUTHOR_ID, entity.authorId)
                .set(COMMENTS.BODY, entity.body)
                .set(COMMENTS.CREATED_AT, entity.createdAt)
                .set(COMMENTS.UPDATED_AT, entity.updatedAt)
                .returningResult(COMMENTS.ID)
                .fetchOne()
                ?.value1() ?: error("Failed to insert comment")

        return entity.withId(id)
    }

    override fun update(entity: Comment): Comment {
        requireNotNull(entity.id) { "Cannot update entity without ID" }

        dsl
            .update(COMMENTS)
            .set(COMMENTS.BODY, entity.body)
            .set(COMMENTS.UPDATED_AT, entity.updatedAt)
            .where(COMMENTS.ID.eq(entity.id))
            .execute()

        return entity
    }

    override fun findById(id: Long): Comment? {
        val record =
            dsl
                .selectFrom(COMMENTS)
                .where(COMMENTS.ID.eq(id))
                .fetchOne() ?: return null

        return Comment(
            id = record.id,
            articleId = record.articleId!!,
            authorId = record.authorId!!,
            body = record.body!!,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
    }

    override fun findByArticleId(articleId: Long): List<Comment> =
        dsl
            .selectFrom(COMMENTS)
            .where(COMMENTS.ARTICLE_ID.eq(articleId))
            .orderBy(COMMENTS.CREATED_AT.desc())
            .fetch()
            .map {
                Comment(
                    id = it.id,
                    articleId = it.articleId!!,
                    authorId = it.authorId!!,
                    body = it.body!!,
                    createdAt = it.createdAt!!,
                    updatedAt = it.updatedAt!!,
                )
            }

    override fun deleteById(id: Long) {
        dsl
            .deleteFrom(COMMENTS)
            .where(COMMENTS.ID.eq(id))
            .execute()
    }
}
