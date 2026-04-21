package com.example.infrastructure.persistence.jooq.comment

import com.example.domain.comment.CommentViewReader
import com.example.domain.comment.readmodel.CommentView
import com.example.domain.profile.readmodel.ProfileView
import com.example.domain.shared.NotFoundException
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqCommentViewReader(
    private val dsl: DSLContext,
) : CommentViewReader {
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
    ): List<CommentView> =
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
            .map { it.toCommentView() }

    override fun getCommentById(
        id: Long,
        viewerId: Long?,
    ): CommentView {
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
                .where(COMMENTS.ID.eq(id))
                .fetchOne() ?: throw NotFoundException("Comment not found")

        return record.toCommentView()
    }

    private fun Record.toCommentView(): CommentView =
        CommentView(
            id = get(COMMENTS.ID)!!,
            body = get(COMMENTS.BODY)!!,
            createdAt = get(COMMENTS.CREATED_AT)!!,
            updatedAt = get(COMMENTS.UPDATED_AT)!!,
            author =
                ProfileView(
                    username = get(USERS.USERNAME)!!,
                    bio = get(USERS.BIO),
                    image = get(USERS.IMAGE),
                    following = get("following", Int::class.java) > 0,
                ),
        )
}
