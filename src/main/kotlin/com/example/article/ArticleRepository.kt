package com.example.article

import com.example.common.persistence.req
import com.example.jooq.public.tables.Favorites
import com.example.jooq.public.tables.Followers
import com.example.jooq.public.tables.User
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.ARTICLE_TAGS
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.TAGS
import com.example.jooq.public.tables.references.USER
import com.example.user.ProfileDto
import com.example.user.UserId
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

interface ArticleRepository {
    fun nextId(): ArticleId

    fun insert(article: Article)

    fun findBySlug(slug: String): Article?

    fun findIdBySlug(slug: String): ArticleId?

    fun existsBySlug(slug: String): Boolean

    fun update(article: Article)

    fun deleteById(id: ArticleId)

    fun favorite(
        id: ArticleId,
        viewerId: UserId,
    )

    fun unfavorite(
        id: ArticleId,
        viewerId: UserId,
    )

    fun findDtoBySlug(
        slug: String,
        viewerId: UserId?,
    ): ArticleDto?

    fun findDtoById(
        id: ArticleId,
        viewerId: UserId?,
    ): ArticleDto?

    fun list(
        filter: ArticleFilter,
        page: Page,
        viewerId: UserId?,
    ): List<ArticleDto>

    fun feed(
        viewerId: UserId,
        page: Page,
    ): List<ArticleDto>

    fun count(filter: ArticleFilter): Int

    fun feedCount(viewerId: UserId): Int

    fun allTags(): List<String>
}

@ApplicationScoped
class JooqArticleRepository(
    private val dsl: DSLContext,
) : ArticleRepository {
    private val author: User = USER.`as`("author")
    private val favByViewer: Favorites = FAVORITES.`as`("fav_by_viewer")
    private val folByViewer: Followers = FOLLOWERS.`as`("fol_by_viewer")

    override fun nextId(): ArticleId =
        ArticleId(dsl.select(DSL.field("nextval('articles_id_seq')", Long::class.java)).fetchSingle().value1()!!)

    override fun insert(article: Article) {
        dsl
            .insertInto(ARTICLES)
            .set(ARTICLES.ID, article.id.value)
            .set(ARTICLES.SLUG, article.slug)
            .set(ARTICLES.TITLE, article.title)
            .set(ARTICLES.DESCRIPTION, article.description)
            .set(ARTICLES.BODY, article.body)
            .set(ARTICLES.AUTHOR_ID, article.authorId.value)
            .set(ARTICLES.CREATED_AT, article.createdAt)
            .set(ARTICLES.UPDATED_AT, article.updatedAt)
            .execute()

        saveTags(article.id, article.tags)
    }

    override fun findBySlug(slug: String): Article? = selectArticleWhere(ARTICLES.SLUG.eq(slug))

    override fun findIdBySlug(slug: String): ArticleId? =
        dsl
            .select(ARTICLES.ID)
            .from(ARTICLES)
            .where(ARTICLES.SLUG.eq(slug))
            .fetchOne()
            ?.let { ArticleId(it.req(ARTICLES.ID)) }

    override fun existsBySlug(slug: String): Boolean =
        dsl.fetchExists(dsl.selectOne().from(ARTICLES).where(ARTICLES.SLUG.eq(slug)))

    override fun update(article: Article) {
        dsl
            .update(ARTICLES)
            .set(ARTICLES.SLUG, article.slug)
            .set(ARTICLES.TITLE, article.title)
            .set(ARTICLES.DESCRIPTION, article.description)
            .set(ARTICLES.BODY, article.body)
            .set(ARTICLES.UPDATED_AT, article.updatedAt)
            .where(ARTICLES.ID.eq(article.id.value))
            .execute()

        dsl.deleteFrom(ARTICLE_TAGS).where(ARTICLE_TAGS.ARTICLE_ID.eq(article.id.value)).execute()
        saveTags(article.id, article.tags)
    }

    override fun deleteById(id: ArticleId) {
        dsl.deleteFrom(ARTICLES).where(ARTICLES.ID.eq(id.value)).execute()
    }

    override fun favorite(
        id: ArticleId,
        viewerId: UserId,
    ) {
        dsl
            .insertInto(FAVORITES)
            .set(FAVORITES.ARTICLE_ID, id.value)
            .set(FAVORITES.USER_ID, viewerId.value)
            .onDuplicateKeyIgnore()
            .execute()
    }

    override fun unfavorite(
        id: ArticleId,
        viewerId: UserId,
    ) {
        dsl
            .deleteFrom(FAVORITES)
            .where(FAVORITES.ARTICLE_ID.eq(id.value))
            .and(FAVORITES.USER_ID.eq(viewerId.value))
            .execute()
    }

    override fun findDtoBySlug(
        slug: String,
        viewerId: UserId?,
    ): ArticleDto? =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .join(author)
            .on(author.ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(ARTICLES.SLUG.eq(slug))
            .fetchOne()
            ?.toDto()

    override fun findDtoById(
        id: ArticleId,
        viewerId: UserId?,
    ): ArticleDto? =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .join(author)
            .on(author.ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(ARTICLES.ID.eq(id.value))
            .fetchOne()
            ?.toDto()

    override fun list(
        filter: ArticleFilter,
        page: Page,
        viewerId: UserId?,
    ): List<ArticleDto> =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .join(author)
            .on(author.ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(buildConditions(filter))
            .orderBy(ARTICLES.CREATED_AT.desc())
            .limit(page.limit)
            .offset(page.offset)
            .fetch()
            .map { it.toDto() }

    override fun feed(
        viewerId: UserId,
        page: Page,
    ): List<ArticleDto> =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLES)
            .join(author)
            .on(author.ID.eq(ARTICLES.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(
                ARTICLES.AUTHOR_ID.`in`(
                    select(FOLLOWERS.FOLLOWEE_ID)
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWER_ID.eq(viewerId.value)),
                ),
            ).orderBy(ARTICLES.CREATED_AT.desc())
            .limit(page.limit)
            .offset(page.offset)
            .fetch()
            .map { it.toDto() }

    override fun count(filter: ArticleFilter): Int =
        dsl
            .selectCount()
            .from(ARTICLES)
            .where(buildConditions(filter))
            .fetchOne(0, Int::class.java) ?: 0

    override fun feedCount(viewerId: UserId): Int =
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

    private fun buildConditions(filter: ArticleFilter): List<Condition> {
        val conditions = mutableListOf<Condition>()
        filter.tag?.let {
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
        filter.author?.let {
            conditions.add(ARTICLES.AUTHOR_ID.`in`(select(USER.ID).from(USER).where(USER.USERNAME.eq(it))))
        }
        filter.favorited?.let {
            conditions.add(
                ARTICLES.ID.`in`(
                    select(FAVORITES.ARTICLE_ID)
                        .from(FAVORITES)
                        .join(USER)
                        .on(USER.ID.eq(FAVORITES.USER_ID))
                        .where(USER.USERNAME.eq(it)),
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
            author.USERNAME,
            author.BIO,
            author.IMAGE,
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

    private fun Record.toDto(): ArticleDto =
        ArticleDto(
            slug = req(ARTICLES.SLUG),
            title = req(ARTICLES.TITLE),
            description = req(ARTICLES.DESCRIPTION),
            body = req(ARTICLES.BODY),
            tagList = @Suppress("UNCHECKED_CAST") (get("tags") as? List<String> ?: emptyList()),
            createdAt = req(ARTICLES.CREATED_AT),
            updatedAt = req(ARTICLES.UPDATED_AT),
            favorited = req("favorited", Int::class.java) > 0,
            favoritesCount = req("favoritesCount", Int::class.java),
            author =
                ProfileDto(
                    username = req(author.USERNAME),
                    bio = get(author.BIO),
                    image = get(author.IMAGE),
                    following = req("following", Int::class.java) > 0,
                ),
        )

    private fun selectArticleWhere(condition: Condition): Article? =
        dsl
            .select(
                ARTICLES.asterisk(),
                multiset(
                    dsl
                        .select(TAGS.NAME)
                        .from(TAGS)
                        .join(ARTICLE_TAGS)
                        .on(ARTICLE_TAGS.TAG_ID.eq(TAGS.ID))
                        .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID)),
                ).`as`("tags").convertFrom { it.map { r -> r.value1() } },
            ).from(ARTICLES)
            .where(condition)
            .fetchOne()
            ?.let { record ->
                @Suppress("UNCHECKED_CAST")
                val tags = record.get("tags") as? List<String> ?: emptyList()
                Article(
                    id = ArticleId(record.req(ARTICLES.ID)),
                    slug = record.req(ARTICLES.SLUG),
                    title = record.req(ARTICLES.TITLE),
                    description = record.req(ARTICLES.DESCRIPTION),
                    body = record.req(ARTICLES.BODY),
                    authorId = UserId(record.req(ARTICLES.AUTHOR_ID)),
                    tags = tags.toSet(),
                    createdAt = record.req(ARTICLES.CREATED_AT),
                    updatedAt = record.req(ARTICLES.UPDATED_AT),
                )
            }

    private fun saveTags(
        articleId: ArticleId,
        tags: Set<String>,
    ) {
        if (tags.isEmpty()) return

        val tagInserts =
            tags.map { tag ->
                dsl
                    .insertInto(TAGS)
                    .set(TAGS.NAME, tag)
                    .onConflict(TAGS.NAME)
                    .doUpdate()
                    .set(TAGS.NAME, tag)
            }
        dsl.batch(tagInserts).execute()

        val tagIds =
            dsl
                .select(TAGS.ID, TAGS.NAME)
                .from(TAGS)
                .where(TAGS.NAME.`in`(tags))
                .fetch()
                .associate { it.req(TAGS.NAME) to it.req(TAGS.ID) }

        val articleTagInserts =
            tags.mapNotNull { tag ->
                tagIds[tag]?.let { tagId ->
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
}
