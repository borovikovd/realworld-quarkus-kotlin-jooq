package com.example.infrastructure.persistence.jooq.comment

import com.example.application.outport.CommentReadRepository
import com.example.application.outport.CryptoService
import com.example.application.readmodel.CommentReadModel
import com.example.infrastructure.persistence.jooq.decryptAuthorProfile
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.vault.tables.references.ENCRYPTION_KEY
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqCommentReadRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : CommentReadRepository {
    override fun findByArticleSlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel> =
        dsl
            .select(commentFields(viewerId))
            .from(COMMENTS)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(COMMENTS.AUTHOR_ID))
            .leftJoin(ENCRYPTION_KEY)
            .on(ENCRYPTION_KEY.USER_ID.eq(COMMENTS.AUTHOR_ID))
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
            .select(commentFields(viewerId))
            .from(COMMENTS)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(COMMENTS.AUTHOR_ID))
            .leftJoin(ENCRYPTION_KEY)
            .on(ENCRYPTION_KEY.USER_ID.eq(COMMENTS.AUTHOR_ID))
            .where(COMMENTS.ID.eq(id))
            .fetchOne()
            ?.toCommentReadModel()

    private fun commentFields(viewerId: Long?): List<Field<*>> =
        listOf(
            COMMENTS.ID,
            COMMENTS.BODY,
            COMMENTS.CREATED_AT,
            COMMENTS.UPDATED_AT,
            COMMENTS.AUTHOR_ID,
            ENCRYPTION_KEY.KEY_CIPHERTEXT,
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
}
