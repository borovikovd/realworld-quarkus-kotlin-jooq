package com.example.comment

import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import com.example.profile.ProfileSummary
import com.example.shared.architecture.ReadService
import com.example.shared.exceptions.NotFoundException
import com.example.user.UserId
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ReadService
class CommentReadService(
    private val dsl: DSLContext,
) {
    fun hydrate(
        id: CommentId,
        viewerId: UserId?,
    ): CommentSummary = getCommentById(id.value, viewerId)

    private fun followingField(viewerId: UserId?): org.jooq.Field<*> {
        val viewerIdValue = viewerId?.value
        return if (viewerIdValue != null) {
            select(count())
                .from(FOLLOWERS)
                .where(FOLLOWERS.FOLLOWEE_ID.eq(COMMENTS.AUTHOR_ID))
                .and(FOLLOWERS.FOLLOWER_ID.eq(viewerIdValue))
                .asField<Int>("following")
        } else {
            org.jooq.impl.DSL
                .`val`(0)
                .`as`("following")
        }
    }

    fun getCommentsBySlug(
        slug: String,
        viewerId: UserId?,
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
        viewerId: UserId?,
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
