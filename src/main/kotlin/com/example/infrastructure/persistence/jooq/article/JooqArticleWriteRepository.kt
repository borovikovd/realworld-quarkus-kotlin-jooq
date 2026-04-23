package com.example.infrastructure.persistence.jooq.article

import com.example.application.port.outbound.ArticleWriteRepository
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.user.UserId
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.ARTICLE_TAGS
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.TAGS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqArticleWriteRepository(
    private val dsl: DSLContext,
) : ArticleWriteRepository {
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
            .set(ARTICLES.TITLE, entity.title)
            .set(ARTICLES.DESCRIPTION, entity.description)
            .set(ARTICLES.BODY, entity.body)
            .set(ARTICLES.AUTHOR_ID, entity.authorId.value)
            .set(ARTICLES.CREATED_AT, entity.createdAt)
            .set(ARTICLES.UPDATED_AT, entity.updatedAt)
            .execute()

        saveTags(entity.id, entity.tags)

        return entity
    }

    override fun update(entity: Article): Article {
        dsl
            .update(ARTICLES)
            .set(ARTICLES.SLUG, entity.slug.value)
            .set(ARTICLES.TITLE, entity.title)
            .set(ARTICLES.DESCRIPTION, entity.description)
            .set(ARTICLES.BODY, entity.body)
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

    private fun saveTags(
        articleId: ArticleId,
        tags: Set<String>,
    ) {
        if (tags.isEmpty()) return

        val tagInserts =
            tags.map { tagName ->
                dsl
                    .insertInto(TAGS)
                    .set(TAGS.NAME, tagName)
                    .onConflict(TAGS.NAME)
                    .doUpdate()
                    .set(TAGS.NAME, tagName)
            }
        dsl.batch(tagInserts).execute()

        val tagIds =
            dsl
                .select(TAGS.ID, TAGS.NAME)
                .from(TAGS)
                .where(TAGS.NAME.`in`(tags))
                .fetch()
                .associate { it.value2()!! to it.value1()!! }

        val articleTagInserts =
            tags.mapNotNull { tagName ->
                tagIds[tagName]?.let { tagId ->
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

    private fun toArticle(result: org.jooq.Record): Article {
        val record = result.into(ARTICLES)

        @Suppress("UNCHECKED_CAST")
        val tags = result.get("tags") as? List<String> ?: emptyList()

        return Article(
            id = ArticleId(record.id!!),
            slug = Slug(record.slug!!),
            title = record.title!!,
            description = record.description!!,
            body = record.body!!,
            authorId = UserId(record.authorId!!),
            tags = tags.toSet(),
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
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
}
