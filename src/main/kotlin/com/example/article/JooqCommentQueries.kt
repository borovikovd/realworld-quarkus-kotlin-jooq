package com.example.article

import com.example.api.model.Profile
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select
import com.example.api.model.Comment as ApiComment

/**
 * jOOQ-based implementation of CommentQueries interface
 *
 * Implementation Details:
 * - Uses jOOQ DSL for type-safe SQL queries
 * - Scalar subqueries for following status
 * - Single SQL query per method for performance
 */
@ApplicationScoped
class JooqCommentQueries : CommentQueries {
    @Inject
    lateinit var dsl: DSLContext

    override fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<ApiComment> {
        val followingField =
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

        return dsl
            .select(
                COMMENTS.ID,
                COMMENTS.BODY,
                COMMENTS.CREATED_AT,
                COMMENTS.UPDATED_AT,
                COMMENTS.AUTHOR_ID,
                USERS.USERNAME,
                USERS.BIO,
                USERS.IMAGE,
                followingField,
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
    }

    override fun getCommentById(
        commentId: Long,
        viewerId: Long?,
    ): ApiComment {
        val followingField =
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
                    followingField,
                ).from(COMMENTS)
                .join(USERS)
                .on(USERS.ID.eq(COMMENTS.AUTHOR_ID))
                .where(COMMENTS.ID.eq(commentId))
                .fetchOne() ?: throw com.example.shared.exceptions
                .NotFoundException("Comment not found")

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
