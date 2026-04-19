package com.example.infrastructure.persistence.jooq.comment

import com.example.domain.article.ArticleId
import com.example.domain.comment.Comment
import com.example.domain.comment.CommentId
import com.example.domain.comment.CommentRepository
import com.example.domain.user.UserId
import com.example.jooq.public.tables.references.COMMENTS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqCommentRepository(
    private val dsl: DSLContext,
) : CommentRepository {
    override fun nextId(): CommentId =
        CommentId(
            dsl
                .select(DSL.field("nextval('comments_id_seq')", Long::class.java))
                .fetchSingle()
                .value1()!!,
        )

    override fun create(entity: Comment): Comment {
        dsl
            .insertInto(COMMENTS)
            .set(COMMENTS.ID, entity.id.value)
            .set(COMMENTS.ARTICLE_ID, entity.articleId.value)
            .set(COMMENTS.AUTHOR_ID, entity.authorId.value)
            .set(COMMENTS.BODY, entity.body)
            .set(COMMENTS.CREATED_AT, entity.createdAt)
            .set(COMMENTS.UPDATED_AT, entity.updatedAt)
            .execute()

        return entity
    }

    override fun update(entity: Comment): Comment {
        dsl
            .update(COMMENTS)
            .set(COMMENTS.BODY, entity.body)
            .set(COMMENTS.UPDATED_AT, entity.updatedAt)
            .where(COMMENTS.ID.eq(entity.id.value))
            .execute()

        return entity
    }

    override fun findById(id: CommentId): Comment? {
        val record =
            dsl
                .selectFrom(COMMENTS)
                .where(COMMENTS.ID.eq(id.value))
                .fetchOne() ?: return null

        return toComment(record)
    }

    override fun findByArticleId(articleId: ArticleId): List<Comment> =
        dsl
            .selectFrom(COMMENTS)
            .where(COMMENTS.ARTICLE_ID.eq(articleId.value))
            .orderBy(COMMENTS.CREATED_AT.desc())
            .fetch()
            .map { toComment(it) }

    private fun toComment(record: com.example.jooq.public.tables.records.CommentsRecord): Comment =
        Comment(
            id = CommentId(record.id!!),
            articleId = ArticleId(record.articleId!!),
            authorId = UserId(record.authorId!!),
            body = record.body!!,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )

    override fun deleteById(id: CommentId) {
        dsl
            .deleteFrom(COMMENTS)
            .where(COMMENTS.ID.eq(id.value))
            .execute()
    }
}
