package com.example.comment

import com.example.article.ArticleId
import com.example.common.persistence.req
import com.example.jooq.public.tables.User
import com.example.jooq.public.tables.references.ARTICLE
import com.example.jooq.public.tables.references.COMMENT
import com.example.jooq.public.tables.references.FOLLOWER
import com.example.jooq.public.tables.references.USER
import com.example.user.ProfileDto
import com.example.user.UserId
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class CommentRepository(
    private val dsl: DSLContext,
) {
    private val author: User = USER.`as`("author")

    fun nextId(): CommentId =
        CommentId(dsl.select(DSL.field("nextval('comment_id_seq')", Long::class.java)).fetchSingle().value1()!!)

    fun insert(comment: Comment) {
        dsl
            .insertInto(COMMENT)
            .set(COMMENT.ID, comment.id.value)
            .set(COMMENT.ARTICLE_ID, comment.articleId.value)
            .set(COMMENT.AUTHOR_ID, comment.authorId.value)
            .set(COMMENT.BODY, comment.body)
            .set(COMMENT.CREATED_AT, comment.createdAt)
            .set(COMMENT.UPDATED_AT, comment.updatedAt)
            .execute()
    }

    fun findById(id: CommentId): Comment? =
        dsl
            .selectFrom(COMMENT)
            .where(COMMENT.ID.eq(id.value))
            .fetchOne()
            ?.let { record ->
                Comment(
                    id = CommentId(record.req(COMMENT.ID)),
                    articleId = ArticleId(record.req(COMMENT.ARTICLE_ID)),
                    authorId = UserId(record.req(COMMENT.AUTHOR_ID)),
                    body = record.req(COMMENT.BODY),
                    createdAt = record.req(COMMENT.CREATED_AT),
                    updatedAt = record.req(COMMENT.UPDATED_AT),
                )
            }

    fun deleteById(id: CommentId) {
        dsl.deleteFrom(COMMENT).where(COMMENT.ID.eq(id.value)).execute()
    }

    fun findDtoById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentDto? =
        dsl
            .select(commentFields(viewerId))
            .from(COMMENT)
            .join(author)
            .on(author.ID.eq(COMMENT.AUTHOR_ID))
            .where(COMMENT.ID.eq(id.value))
            .fetchOne()
            ?.toDto()

    fun findDtosByArticleSlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentDto> =
        dsl
            .select(commentFields(viewerId))
            .from(COMMENT)
            .join(author)
            .on(author.ID.eq(COMMENT.AUTHOR_ID))
            .join(ARTICLE)
            .on(ARTICLE.ID.eq(COMMENT.ARTICLE_ID))
            .where(ARTICLE.SLUG.eq(slug))
            .orderBy(COMMENT.CREATED_AT.desc())
            .fetch()
            .map { it.toDto() }

    private fun commentFields(viewerId: UserId?): List<Field<*>> =
        listOf(
            COMMENT.ID,
            COMMENT.BODY,
            COMMENT.CREATED_AT,
            COMMENT.UPDATED_AT,
            author.USERNAME,
            author.BIO,
            author.IMAGE,
            if (viewerId != null) {
                select(count())
                    .from(FOLLOWER)
                    .where(FOLLOWER.FOLLOWEE_ID.eq(COMMENT.AUTHOR_ID))
                    .and(FOLLOWER.FOLLOWER_ID.eq(viewerId.value))
                    .asField<Int>("following")
            } else {
                DSL.`val`(0).`as`("following")
            },
        )

    private fun Record.toDto(): CommentDto =
        CommentDto(
            id = req(COMMENT.ID),
            body = req(COMMENT.BODY),
            createdAt = req(COMMENT.CREATED_AT),
            updatedAt = req(COMMENT.UPDATED_AT),
            author =
                ProfileDto(
                    username = req(author.USERNAME),
                    bio = get(author.BIO),
                    image = get(author.IMAGE),
                    following = req("following", Int::class.java) > 0,
                ),
        )
}
