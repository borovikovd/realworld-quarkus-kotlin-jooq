package com.example.comment

import com.example.article.ArticleId
import com.example.common.persistence.decryptAuthorProfile
import com.example.common.persistence.req
import com.example.common.security.CryptoService
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.vault.tables.references.PERSON
import com.example.user.UserId
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

interface CommentRepository {
    fun nextId(): CommentId

    fun insert(comment: Comment)

    fun findById(id: CommentId): Comment?

    fun deleteById(id: CommentId)

    fun findDtoById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentDto?

    fun findDtosByArticleSlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentDto>
}

@ApplicationScoped
class JooqCommentRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : CommentRepository {
    override fun nextId(): CommentId =
        CommentId(dsl.select(DSL.field("nextval('comments_id_seq')", Long::class.java)).fetchSingle().value1()!!)

    override fun insert(comment: Comment) {
        dsl
            .insertInto(COMMENTS)
            .set(COMMENTS.ID, comment.id.value)
            .set(COMMENTS.ARTICLE_ID, comment.articleId.value)
            .set(COMMENTS.AUTHOR_ID, comment.authorId.value)
            .set(COMMENTS.BODY, comment.body)
            .set(COMMENTS.CREATED_AT, comment.createdAt)
            .set(COMMENTS.UPDATED_AT, comment.updatedAt)
            .execute()
    }

    override fun findById(id: CommentId): Comment? =
        dsl
            .selectFrom(COMMENTS)
            .where(COMMENTS.ID.eq(id.value))
            .fetchOne()
            ?.let { record ->
                Comment(
                    id = CommentId(record.req(COMMENTS.ID)),
                    articleId = ArticleId(record.req(COMMENTS.ARTICLE_ID)),
                    authorId = UserId(record.req(COMMENTS.AUTHOR_ID)),
                    body = record.req(COMMENTS.BODY),
                    createdAt = record.req(COMMENTS.CREATED_AT),
                    updatedAt = record.req(COMMENTS.UPDATED_AT),
                )
            }

    override fun deleteById(id: CommentId) {
        dsl.deleteFrom(COMMENTS).where(COMMENTS.ID.eq(id.value)).execute()
    }

    override fun findDtoById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentDto? =
        dsl
            .select(commentFields(viewerId))
            .from(COMMENTS)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(COMMENTS.AUTHOR_ID))
            .where(COMMENTS.ID.eq(id.value))
            .fetchOne()
            ?.toDto()

    override fun findDtosByArticleSlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentDto> =
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
            .map { it.toDto() }

    private fun commentFields(viewerId: UserId?): List<Field<*>> =
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
                    .and(FOLLOWERS.FOLLOWER_ID.eq(viewerId.value))
                    .asField<Int>("following")
            } else {
                DSL.`val`(0).`as`("following")
            },
        )

    private fun Record.toDto(): CommentDto =
        CommentDto(
            id = req(COMMENTS.ID),
            body = req(COMMENTS.BODY),
            createdAt = req(COMMENTS.CREATED_AT),
            updatedAt = req(COMMENTS.UPDATED_AT),
            author = decryptAuthorProfile(crypto, req(COMMENTS.AUTHOR_ID), req("following", Int::class.java) > 0),
        )
}
