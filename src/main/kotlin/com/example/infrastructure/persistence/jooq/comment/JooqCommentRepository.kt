package com.example.infrastructure.persistence.jooq.comment

import com.example.application.outport.comment.CommentRepository
import com.example.application.outport.security.CryptoService
import com.example.application.readmodel.CommentReadModel
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.aggregate.user.UserId
import com.example.infrastructure.persistence.jooq.shared.decryptAuthorProfile
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqCommentRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
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

    override fun findById(id: CommentId): Comment? {
        val record =
            dsl
                .selectFrom(COMMENTS)
                .where(COMMENTS.ID.eq(id.value))
                .fetchOne() ?: return null

        return toComment(record)
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

    override fun findByArticleId(articleId: ArticleId): List<Comment> =
        dsl
            .selectFrom(COMMENTS)
            .where(COMMENTS.ARTICLE_ID.eq(articleId.value))
            .orderBy(COMMENTS.CREATED_AT.desc())
            .fetch()
            .map { toComment(it) }

    override fun deleteById(id: CommentId) {
        dsl
            .deleteFrom(COMMENTS)
            .where(COMMENTS.ID.eq(id.value))
            .execute()
    }

    override fun findById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel? =
        dsl
            .select(commentFields(viewerId))
            .from(COMMENTS)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(COMMENTS.AUTHOR_ID))
            .where(COMMENTS.ID.eq(id))
            .fetchOne()
            ?.toCommentReadModel()

    override fun findByArticleSlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel> =
        dsl
            .select(commentFields(viewerId))
            .from(COMMENTS)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(COMMENTS.AUTHOR_ID))
            .join(ARTICLES)
            .on(ARTICLES.ID.eq(COMMENTS.ARTICLE_ID))
            .where(ARTICLES.SLUG.eq(slug))
            .orderBy(COMMENTS.CREATED_AT.desc())
            .fetch()
            .map { it.toCommentReadModel() }

    private fun commentFields(viewerId: Long?): List<Field<*>> =
        listOf(
            COMMENTS.ID,
            COMMENTS.BODY,
            COMMENTS.CREATED_AT,
            COMMENTS.UPDATED_AT,
            COMMENTS.AUTHOR_ID,
            PERSON.USERNAME_ENC,
            PERSON.BIO_ENC,
            PERSON.IMAGE_ENC,
            if (viewerId != null) {
                select(count())
                    .from(FOLLOWERS)
                    .where(FOLLOWERS.FOLLOWEE_ID.eq(COMMENTS.AUTHOR_ID))
                    .and(FOLLOWERS.FOLLOWER_ID.eq(viewerId))
                    .asField<Int>("following")
            } else {
                org.jooq.impl.DSL
                    .`val`(0)
                    .`as`("following")
            },
        )

    private fun Record.toCommentReadModel(): CommentReadModel =
        CommentReadModel(
            id = get(COMMENTS.ID)!!,
            body = get(COMMENTS.BODY)!!,
            createdAt = get(COMMENTS.CREATED_AT)!!,
            updatedAt = get(COMMENTS.UPDATED_AT)!!,
            author = decryptAuthorProfile(crypto, get(COMMENTS.AUTHOR_ID), get("following", Int::class.java) > 0),
        )

    private fun toComment(record: com.example.jooq.public.tables.records.CommentsRecord): Comment =
        Comment(
            id = CommentId(record.id!!),
            articleId = ArticleId(record.articleId!!),
            authorId = UserId(record.authorId!!),
            body = record.body!!,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
}
