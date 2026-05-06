package com.example.infrastructure.persistence.jooq

import com.example.application.port.ArticleRepository
import com.example.application.port.security.CryptoService
import com.example.application.readmodel.ArticleReadModel
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.article.Body
import com.example.domain.aggregate.article.Description
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.article.Tag
import com.example.domain.aggregate.article.Title
import com.example.domain.aggregate.user.UserId
import com.example.infrastructure.persistence.jooq.shared.decryptAuthorProfile
import com.example.jooq.public.tables.Favorites
import com.example.jooq.public.tables.Followers
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.ARTICLE_TAGS
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.TAGS
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqArticleRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : ArticleRepository {
    private val favByViewer: Favorites = FAVORITES.`as`("fav_by_viewer")
    private val folByViewer: Followers = FOLLOWERS.`as`("fol_by_viewer")

    override fun nextId(): ArticleId =
        ArticleId(
            dsl
                .select(DSL.field("nextval('articles_id_seq')", Long::class.java))
                .fetchSingle()
                .value1()!!,
        )

    override fun create(entity: Article): Article {
        dsl
            .insertInto(ARTICLES)
            .set(ARTICLES.ID, entity.id.value)
            .set(ARTICLES.SLUG, entity.slug.value)
            .set(ARTICLES.TITLE, entity.title.value)
            .set(ARTICLES.DESCRIPTION, entity.description.value)
            .set(ARTICLES.BODY, entity.body.value)
            .set(ARTICLES.AUTHOR_ID, entity.authorId.value)
            .set(ARTICLES.CREATED_AT, entity.createdAt)
            .set(ARTICLES.UPDATED_AT, entity.updatedAt)
            .execute()

        saveTags(entity.id, entity.tags)

        return entity
    }

    override fun findById(id: ArticleId): Article? {
        val result =
            dsl
                .select(
                    ARTICLES.asterisk(),
                    DSL
                        .multiset(
                            dsl
                                .select(TAGS.NAME)
                                .from(TAGS)
                                .join(ARTICLE_TAGS)
                                .on(ARTICLE_TAGS.TAG_ID.eq(TAGS.ID))
                                .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID)),
                        ).`as`("tags")
                        .convertFrom { it.map { r -> r.value1() } },
                ).from(ARTICLES)
                .where(ARTICLES.ID.eq(id.value))
                .fetchOne() ?: return null

        return toArticle(result)
    }

    override fun findBySlug(slug: Slug): Article? {
        val result =
            dsl
                .select(
                    ARTICLES.asterisk(),
                    DSL
                        .multiset(
                            dsl
                                .select(TAGS.NAME)
                                .from(TAGS)
                                .join(ARTICLE_TAGS)
                                .on(ARTICLE_TAGS.TAG_ID.eq(TAGS.ID))
                                .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID)),
                        ).`as`("tags")
                        .convertFrom { it.map { r -> r.value1() } },
                ).from(ARTICLES)
                .where(ARTICLES.SLUG.eq(slug.value))
                .fetchOne() ?: return null

        return toArticle(result)
    }

    override fun update(entity: Article): Article {
        dsl
            .update(ARTICLES)
            .set(ARTICLES.SLUG, entity.slug.value)
            .set(ARTICLES.TITLE, entity.title.value)
            .set(ARTICLES.DESCRIPTION, entity.description.value)
            .set(ARTICLES.BODY, entity.body.value)
            .set(ARTICLES.UPDATED_AT, entity.updatedAt)
            .where(ARTICLES.ID.eq(entity.id.value))
            .execute()

        dsl
            .deleteFrom(ARTICLE_TAGS)
            .where(ARTICLE_TAGS.ARTICLE_ID.eq(entity.id.value))
            .execute()

        saveTags(entity.id, entity.tags)

        return entity
    }

    override fun deleteById(id: ArticleId) {
        dsl.deleteFrom(ARTICLES).where(ARTICLES.ID.eq(id.value)).execute()
    }

    override fun favorite(
        articleId: ArticleId,
        userId: UserId,
    ) {
        dsl
            .insertInto(FAVORITES)
            .set(FAVORITES.ARTICLE_ID, articleId.value)
            .set(FAVORITES.USER_ID, userId.value)
            .onDuplicateKeyIgnore()
            .execute()
    }

    override fun unfavorite(
        articleId: ArticleId,
        userId: UserId,
    ) {
        dsl
            .deleteFrom(FAVORITES)
            .where(FAVORITES.ARTICLE_ID.eq(articleId.value))
            .and(FAVORITES.USER_ID.eq(userId.value))
            .execute()
    }

    override fun isFavorited(
        articleId: ArticleId,
        userId: UserId,
    ): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(FAVORITES)
                .where(FAVORITES.ARTICLE_ID.eq(articleId.value))
                .and(FAVORITES.USER_ID.eq(userId.value)),
        )

    override fun findById(
        id: ArticleId,
        viewerId: UserId?,
    ): ArticleReadModel? =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(ARTICLES.ID.eq(id.value))
            .fetchOne()
            ?.toArticleReadModel()

    override fun findBySlug(
        slug: String,
        viewerId: UserId?,
    ): ArticleReadModel? =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(ARTICLES.SLUG.eq(slug))
            .fetchOne()
            ?.toArticleReadModel()

    override fun list(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: UserId?,
    ): List<ArticleReadModel> =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(buildConditions(tag, author, favorited))
            .orderBy(ARTICLES.CREATED_AT.desc())
            .limit(limit)
            .offset(offset)
            .fetch()
            .map { it.toArticleReadModel() }

    override fun listFeed(
        viewerId: UserId,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel> =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .leftJoin(PERSON)
            .on(PERSON.USER_ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(
                ARTICLES.AUTHOR_ID.`in`(
                    select(FOLLOWERS.FOLLOWEE_ID)
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWER_ID.eq(viewerId.value)),
                ),
            ).orderBy(ARTICLES.CREATED_AT.desc())
            .limit(limit)
            .offset(offset)
            .fetch()
            .map { it.toArticleReadModel() }

    override fun count(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int =
        dsl
            .selectCount()
            .from(ARTICLES)
            .where(buildConditions(tag, author, favorited))
            .fetchOne(0, Int::class.java) ?: 0

    override fun countFeed(viewerId: UserId): Int =
        dsl
            .selectCount()
            .from(ARTICLES)
            .where(
                ARTICLES.AUTHOR_ID.`in`(
                    select(FOLLOWERS.FOLLOWEE_ID)
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWER_ID.eq(viewerId.value)),
                ),
            ).fetchOne(0, Int::class.java) ?: 0

    override fun allTags(): List<String> =
        dsl
            .select(TAGS.NAME)
            .from(TAGS)
            .orderBy(TAGS.NAME)
            .fetch()
            .mapNotNull { it.value1() }

    private fun buildConditions(
        tag: String?,
        author: String?,
        favorited: String?,
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()

        tag?.let {
            conditions.add(
                ARTICLES.ID.`in`(
                    select(ARTICLE_TAGS.ARTICLE_ID)
                        .from(ARTICLE_TAGS)
                        .join(TAGS)
                        .on(TAGS.ID.eq(ARTICLE_TAGS.TAG_ID))
                        .where(TAGS.NAME.eq(it)),
                ),
            )
        }

        author?.let {
            val authorHash = crypto.hmacUsername(it)
            conditions.add(
                ARTICLES.AUTHOR_ID.`in`(
                    select(PERSON.USER_ID)
                        .from(PERSON)
                        .where(PERSON.USERNAME_HASH.eq(authorHash)),
                ),
            )
        }

        favorited?.let {
            val favoritedHash = crypto.hmacUsername(it)
            conditions.add(
                ARTICLES.ID.`in`(
                    select(FAVORITES.ARTICLE_ID)
                        .from(FAVORITES)
                        .join(PERSON)
                        .on(PERSON.USER_ID.eq(FAVORITES.USER_ID))
                        .where(PERSON.USERNAME_HASH.eq(favoritedHash)),
                ),
            )
        }

        return conditions
    }

    private fun <R : Record> SelectJoinStep<R>.applyViewerJoins(viewerId: UserId?): SelectJoinStep<R> {
        viewerId ?: return this
        return leftJoin(favByViewer)
            .on(favByViewer.ARTICLE_ID.eq(ARTICLES.ID).and(favByViewer.USER_ID.eq(viewerId.value)))
            .leftJoin(folByViewer)
            .on(folByViewer.FOLLOWEE_ID.eq(ARTICLES.AUTHOR_ID).and(folByViewer.FOLLOWER_ID.eq(viewerId.value)))
    }

    private fun articleFields(viewerId: UserId?): List<Field<*>> {
        val favoritedField =
            if (viewerId != null) {
                DSL.`when`(favByViewer.USER_ID.isNotNull, 1).otherwise(0).`as`("favorited")
            } else {
                DSL.`val`(0).`as`("favorited")
            }

        val followingField =
            if (viewerId != null) {
                DSL.`when`(folByViewer.FOLLOWER_ID.isNotNull, 1).otherwise(0).`as`("following")
            } else {
                DSL.`val`(0).`as`("following")
            }

        return listOf(
            ARTICLES.ID,
            ARTICLES.SLUG,
            ARTICLES.TITLE,
            ARTICLES.DESCRIPTION,
            ARTICLES.BODY,
            ARTICLES.AUTHOR_ID,
            ARTICLES.CREATED_AT,
            ARTICLES.UPDATED_AT,
            PERSON.USERNAME_ENC,
            PERSON.BIO_ENC,
            PERSON.IMAGE_ENC,
            multiset(
                select(TAGS.NAME)
                    .from(TAGS)
                    .join(ARTICLE_TAGS)
                    .on(ARTICLE_TAGS.TAG_ID.eq(TAGS.ID))
                    .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID)),
            ).`as`("tags").convertFrom { it.map { r -> r.value1() } },
            select(count())
                .from(FAVORITES)
                .where(FAVORITES.ARTICLE_ID.eq(ARTICLES.ID))
                .asField<Int>("favoritesCount"),
            favoritedField,
            followingField,
        )
    }

    private fun Record.toArticleReadModel(): ArticleReadModel =
        ArticleReadModel(
            slug = get(ARTICLES.SLUG)!!,
            title = get(ARTICLES.TITLE)!!,
            description = get(ARTICLES.DESCRIPTION)!!,
            body = get(ARTICLES.BODY)!!,
            tagList =
                @Suppress("UNCHECKED_CAST")
                (get("tags") as? List<String> ?: emptyList()),
            createdAt = get(ARTICLES.CREATED_AT)!!,
            updatedAt = get(ARTICLES.UPDATED_AT)!!,
            favorited = get("favorited", Int::class.java) > 0,
            favoritesCount = get("favoritesCount", Int::class.java),
            author = decryptAuthorProfile(crypto, get(ARTICLES.AUTHOR_ID), get("following", Int::class.java) > 0),
        )

    private fun saveTags(
        articleId: ArticleId,
        tags: Set<Tag>,
    ) {
        if (tags.isEmpty()) return

        val tagInserts =
            tags.map { tag ->
                dsl
                    .insertInto(TAGS)
                    .set(TAGS.NAME, tag.value)
                    .onConflict(TAGS.NAME)
                    .doUpdate()
                    .set(TAGS.NAME, tag.value)
            }
        dsl.batch(tagInserts).execute()

        val tagIds =
            dsl
                .select(TAGS.ID, TAGS.NAME)
                .from(TAGS)
                .where(TAGS.NAME.`in`(tags.map { it.value }))
                .fetch()
                .associate { it.value2()!! to it.value1()!! }

        val articleTagInserts =
            tags.mapNotNull { tag ->
                tagIds[tag.value]?.let { tagId ->
                    dsl
                        .insertInto(ARTICLE_TAGS)
                        .set(ARTICLE_TAGS.ARTICLE_ID, articleId.value)
                        .set(ARTICLE_TAGS.TAG_ID, tagId)
                        .onConflict(ARTICLE_TAGS.ARTICLE_ID, ARTICLE_TAGS.TAG_ID)
                        .doNothing()
                }
            }
        if (articleTagInserts.isNotEmpty()) {
            dsl.batch(articleTagInserts).execute()
        }
    }

    private fun toArticle(result: org.jooq.Record): Article {
        val record = result.into(ARTICLES)

        @Suppress("UNCHECKED_CAST")
        val tags = result.get("tags") as? List<String> ?: emptyList()

        return Article(
            id = ArticleId(record.id!!),
            slug = Slug(record.slug!!),
            title = Title(record.title!!),
            description = Description(record.description!!),
            body = Body(record.body!!),
            authorId = UserId(record.authorId!!),
            tags = tags.map { Tag(it) }.toSet(),
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
    }
}
