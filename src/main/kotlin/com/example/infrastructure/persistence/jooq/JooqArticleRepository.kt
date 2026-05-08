package com.example.infrastructure.persistence.jooq

import com.example.application.port.ArticleRepository
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.article.Body
import com.example.domain.aggregate.article.Description
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.article.Tag
import com.example.domain.aggregate.article.Title
import com.example.domain.aggregate.user.UserId
import com.example.infrastructure.persistence.jooq.shared.req
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.ARTICLE_TAGS
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.TAGS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqArticleRepository(
    private val dsl: DSLContext,
) : ArticleRepository {
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

    override fun findById(id: ArticleId): Article? = selectArticleWhere(ARTICLES.ID.eq(id.value))

    override fun findBySlug(slug: Slug): Article? = selectArticleWhere(ARTICLES.SLUG.eq(slug.value))

    private fun selectArticleWhere(condition: Condition): Article? =
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
            .where(condition)
            .fetchOne()
            ?.let(::toArticle)

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
                .associate { it.req(TAGS.NAME) to it.req(TAGS.ID) }

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
        @Suppress("UNCHECKED_CAST")
        val tags = result.get("tags") as? List<String> ?: emptyList()

        return Article(
            id = ArticleId(result.req(ARTICLES.ID)),
            slug = Slug(result.req(ARTICLES.SLUG)),
            title = Title(result.req(ARTICLES.TITLE)),
            description = Description(result.req(ARTICLES.DESCRIPTION)),
            body = Body(result.req(ARTICLES.BODY)),
            authorId = UserId(result.req(ARTICLES.AUTHOR_ID)),
            tags = tags.map { Tag(it) }.toSet(),
            createdAt = result.req(ARTICLES.CREATED_AT),
            updatedAt = result.req(ARTICLES.UPDATED_AT),
        )
    }
}
