package com.example.infrastructure.persistence.jooq.comment

import com.example.application.comment.CommentReadService
import com.example.application.comment.CommentSummary
import com.example.application.profile.ProfileSummary
import com.example.domain.shared.NotFoundException
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import com.example.shared.architecture.ReadService
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ReadService
class JooqCommentReadService(
    private val dsl: DSLContext,
) : CommentReadService {
    override fun hydrate(
        id: Long,
        viewerId: Long?,
    ): CommentSummary = getCommentById(id, viewerId)

    private fun followingField(viewerId: Long?): org.jooq.Field<*> =
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
        }

    override fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentSummary> =
        dsl
            .select(
                COMMENTS.ID,
                COMMENTS.BODY,
                COMMENTS.CREATED_AT,
                COMMENTS.UPDATED_AT,
                COMMENTS.AUTHOR_ID,
                USERS.USERNAME,
                USERS.BIO,
                USERS.IMAGE,
                followingField(viewerId),
            ).from(COMMENTS)
            .join(USERS)
            .on(USERS.ID.eq(COMMENTS.AUTHOR_ID))
            .join(ARTICLES)
            .on(ARTICLES.ID.eq(COMMENTS.ARTICLE_ID))
            .where(ARTICLES.SLUG.eq(slug))
            .orderBy(COMMENTS.CREATED_AT.desc())
            .fetch()
            .map { it.toCommentSummary() }

    private fun getCommentById(
        commentId: Long,
        viewerId: Long?,
    ): CommentSummary {
        val record =
            dsl
                .select(
                    COMMENTS.ID,
                    COMMENTS.BODY,
                    COMMENTS.CREATED_AT,
                    COMMENTS.UPDATED_AT,
                    COMMENTS.AUTHOR_ID,
                    USERS.USERNAME,
                    USERS.BIO,
                    USERS.IMAGE,
                    followingField(viewerId),
                ).from(COMMENTS)
                .join(USERS)
                .on(USERS.ID.eq(COMMENTS.AUTHOR_ID))
                .where(COMMENTS.ID.eq(commentId))
                .fetchOne() ?: throw NotFoundException("Comment not found")

        return record.toCommentSummary()
    }

    private fun Record.toCommentSummary(): CommentSummary =
        CommentSummary(
            id = get(COMMENTS.ID)!!,
            body = get(COMMENTS.BODY)!!,
            createdAt = get(COMMENTS.CREATED_AT)!!,
            updatedAt = get(COMMENTS.UPDATED_AT)!!,
            author =
                ProfileSummary(
                    username = get(USERS.USERNAME)!!,
                    bio = get(USERS.BIO),
                    image = get(USERS.IMAGE),
                    following = get("following", Int::class.java) > 0,
                ),
        )
}
