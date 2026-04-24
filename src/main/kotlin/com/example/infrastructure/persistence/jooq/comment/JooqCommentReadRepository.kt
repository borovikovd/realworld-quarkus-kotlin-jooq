package com.example.infrastructure.persistence.jooq.comment

import com.example.application.inport.query.readmodel.CommentReadModel
import com.example.application.inport.query.readmodel.ProfileReadModel
import com.example.application.outport.CommentReadRepository
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
class JooqCommentReadRepository(
    private val dsl: DSLContext,
) : CommentReadRepository {
    override fun findByArticleSlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel> =
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
            .map { it.toCommentReadModel() }

    override fun findById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel? =
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
            .fetchOne()
            ?.toCommentReadModel()

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

    private fun Record.toCommentReadModel(): CommentReadModel =
        CommentReadModel(
            id = get(COMMENTS.ID)!!,
            body = get(COMMENTS.BODY)!!,
            createdAt = get(COMMENTS.CREATED_AT)!!,
            updatedAt = get(COMMENTS.UPDATED_AT)!!,
            author =
                ProfileReadModel(
                    username = get(USERS.USERNAME)!!,
                    bio = get(USERS.BIO),
                    image = get(USERS.IMAGE),
                    following = get("following", Int::class.java) > 0,
                ),
        )
}
