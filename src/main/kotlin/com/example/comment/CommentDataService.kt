package com.example.comment

import com.example.api.model.Profile
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import com.example.shared.architecture.DataService
import com.example.shared.exceptions.NotFoundException
import com.example.user.UserId
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select
import com.example.api.model.Comment as ApiComment

@DataService
class CommentDataService(
    private val dsl: DSLContext,
) {
    fun hydrate(
        id: CommentId,
        viewerId: UserId?,
    ): ApiComment = getCommentById(id.value, viewerId)

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
    ): List<ApiComment> =
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
            .join(com.example.jooq.public.tables.references.ARTICLES)
            .on(
                com.example.jooq.public.tables.references.ARTICLES.ID
                    .eq(COMMENTS.ARTICLE_ID),
            ).where(
                com.example.jooq.public.tables.references.ARTICLES.SLUG
                    .eq(slug),
            ).orderBy(COMMENTS.CREATED_AT.desc())
            .fetch()
            .map { record ->
                ApiComment()
                    .id(record.get(COMMENTS.ID)?.toInt())
                    .body(record.get(COMMENTS.BODY))
                    .createdAt(record.get(COMMENTS.CREATED_AT))
                    .updatedAt(record.get(COMMENTS.UPDATED_AT))
                    .author(
                        Profile()
                            .username(record.get(USERS.USERNAME))
                            .bio(record.get(USERS.BIO))
                            .image(record.get(USERS.IMAGE))
                            .following(record.get("following", Int::class.java) > 0),
                    )
            }

    private fun getCommentById(
        commentId: Long,
        viewerId: UserId?,
    ): ApiComment {
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

        return ApiComment()
            .id(record.get(COMMENTS.ID)?.toInt())
            .body(record.get(COMMENTS.BODY))
            .createdAt(record.get(COMMENTS.CREATED_AT))
            .updatedAt(record.get(COMMENTS.UPDATED_AT))
            .author(
                Profile()
                    .username(record.get(USERS.USERNAME))
                    .bio(record.get(USERS.BIO))
                    .image(record.get(USERS.IMAGE))
                    .following(record.get("following", Int::class.java) > 0),
            )
    }
}
