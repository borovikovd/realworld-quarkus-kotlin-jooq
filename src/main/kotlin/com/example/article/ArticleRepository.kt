package com.example.article

import com.example.common.persistence.req
import com.example.jooq.public.tables.Favorite
import com.example.jooq.public.tables.Follower
import com.example.jooq.public.tables.User
import com.example.jooq.public.tables.references.ARTICLE
import com.example.jooq.public.tables.references.ARTICLE_TAG
import com.example.jooq.public.tables.references.FAVORITE
import com.example.jooq.public.tables.references.FOLLOWER
import com.example.jooq.public.tables.references.TAG
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
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.select

@ApplicationScoped
class ArticleRepository(
    private val dsl: DSLContext,
) {
    private val author: User = USER.`as`("author")
    private val favByViewer: Favorite = FAVORITE.`as`("fav_by_viewer")
    private val folByViewer: Follower = FOLLOWER.`as`("fol_by_viewer")

    fun nextId(): ArticleId =
        ArticleId(dsl.select(DSL.field("nextval('article_id_seq')", Long::class.java)).fetchSingle().value1()!!)

    fun insert(article: Article) {
        dsl
            .insertInto(ARTICLE)
            .set(ARTICLE.ID, article.id.value)
            .set(ARTICLE.SLUG, article.slug)
            .set(ARTICLE.TITLE, article.title)
            .set(ARTICLE.DESCRIPTION, article.description)
            .set(ARTICLE.BODY, article.body)
            .set(ARTICLE.AUTHOR_ID, article.authorId.value)
            .set(ARTICLE.CREATED_AT, article.createdAt)
            .set(ARTICLE.UPDATED_AT, article.updatedAt)
            .execute()

        saveTags(article.id, article.tags)
    }

    fun findBySlug(slug: String): Article? = selectArticleWhere(ARTICLE.SLUG.eq(slug))

    fun findIdBySlug(slug: String): ArticleId? =
        dsl
            .select(ARTICLE.ID)
            .from(ARTICLE)
            .where(ARTICLE.SLUG.eq(slug))
            .fetchOne()
            ?.let { ArticleId(it.req(ARTICLE.ID)) }

    fun existsBySlug(slug: String): Boolean =
        dsl.fetchExists(dsl.selectOne().from(ARTICLE).where(ARTICLE.SLUG.eq(slug)))

    fun update(article: Article) {
        dsl
            .update(ARTICLE)
            .set(ARTICLE.SLUG, article.slug)
            .set(ARTICLE.TITLE, article.title)
            .set(ARTICLE.DESCRIPTION, article.description)
            .set(ARTICLE.BODY, article.body)
            .set(ARTICLE.UPDATED_AT, article.updatedAt)
            .where(ARTICLE.ID.eq(article.id.value))
            .execute()

        dsl.deleteFrom(ARTICLE_TAG).where(ARTICLE_TAG.ARTICLE_ID.eq(article.id.value)).execute()
        saveTags(article.id, article.tags)
    }

    fun deleteById(id: ArticleId) {
        dsl.deleteFrom(ARTICLE).where(ARTICLE.ID.eq(id.value)).execute()
    }

    fun favorite(
        id: ArticleId,
        viewerId: UserId,
    ) {
        dsl
            .insertInto(FAVORITE)
            .set(FAVORITE.ARTICLE_ID, id.value)
            .set(FAVORITE.USER_ID, viewerId.value)
            .onDuplicateKeyIgnore()
            .execute()
    }

    fun unfavorite(
        id: ArticleId,
        viewerId: UserId,
    ) {
        dsl
            .deleteFrom(FAVORITE)
            .where(FAVORITE.ARTICLE_ID.eq(id.value))
            .and(FAVORITE.USER_ID.eq(viewerId.value))
            .execute()
    }

    fun findDtoBySlug(
        slug: String,
        viewerId: UserId?,
    ): ArticleDto? =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLE)
            .join(author)
            .on(author.ID.eq(ARTICLE.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(ARTICLE.SLUG.eq(slug))
            .fetchOne()
            ?.toDto()

    fun findDtoById(
        id: ArticleId,
        viewerId: UserId?,
    ): ArticleDto? =
        dsl
            .select(articleFields(viewerId))
            .from(ARTICLE)
            .join(author)
            .on(author.ID.eq(ARTICLE.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(ARTICLE.ID.eq(id.value))
            .fetchOne()
            ?.toDto()

    fun list(
        filter: ArticleFilter,
        page: Page,
        viewerId: UserId?,
    ): List<ArticleListItemDto> =
        dsl
            .select(articleFields(viewerId, includeBody = false))
            .from(ARTICLE)
            .join(author)
            .on(author.ID.eq(ARTICLE.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(buildConditions(filter))
            .orderBy(ARTICLE.CREATED_AT.desc())
            .limit(page.limit)
            .offset(page.offset)
            .fetch()
            .map { it.toListItemDto() }

    fun feed(
        viewerId: UserId,
        page: Page,
    ): List<ArticleListItemDto> =
        dsl
            .select(articleFields(viewerId, includeBody = false))
            .from(ARTICLE)
            .join(author)
            .on(author.ID.eq(ARTICLE.AUTHOR_ID))
            .applyViewerJoins(viewerId)
            .where(
                ARTICLE.AUTHOR_ID.`in`(
                    select(FOLLOWER.FOLLOWEE_ID)
                        .from(FOLLOWER)
                        .where(FOLLOWER.FOLLOWER_ID.eq(viewerId.value)),
                ),
            ).orderBy(ARTICLE.CREATED_AT.desc())
            .limit(page.limit)
            .offset(page.offset)
            .fetch()
            .map { it.toListItemDto() }

    fun count(filter: ArticleFilter): Int =
        dsl
            .selectCount()
            .from(ARTICLE)
            .where(buildConditions(filter))
            .fetchOne(0, Int::class.java) ?: 0

    fun feedCount(viewerId: UserId): Int =
        dsl
            .selectCount()
            .from(ARTICLE)
            .where(
                ARTICLE.AUTHOR_ID.`in`(
                    select(FOLLOWER.FOLLOWEE_ID)
                        .from(FOLLOWER)
                        .where(FOLLOWER.FOLLOWER_ID.eq(viewerId.value)),
                ),
            ).fetchOne(0, Int::class.java) ?: 0

    fun allTags(): List<String> =
        dsl
            .select(TAG.NAME)
            .from(TAG)
            .orderBy(TAG.NAME)
            .fetch()
            .mapNotNull { it.value1() }

    private fun buildConditions(filter: ArticleFilter): List<Condition> {
        val conditions = mutableListOf<Condition>()
        filter.tag?.let {
            conditions.add(
                ARTICLE.ID.`in`(
                    select(ARTICLE_TAG.ARTICLE_ID)
                        .from(ARTICLE_TAG)
                        .join(TAG)
                        .on(TAG.ID.eq(ARTICLE_TAG.TAG_ID))
                        .where(TAG.NAME.eq(it)),
                ),
            )
        }
        filter.author?.let {
            conditions.add(ARTICLE.AUTHOR_ID.`in`(select(USER.ID).from(USER).where(USER.USERNAME.eq(it))))
        }
        filter.favorited?.let {
            conditions.add(
                ARTICLE.ID.`in`(
                    select(FAVORITE.ARTICLE_ID)
                        .from(FAVORITE)
                        .join(USER)
                        .on(USER.ID.eq(FAVORITE.USER_ID))
                        .where(USER.USERNAME.eq(it)),
                ),
            )
        }
        return conditions
    }

    private fun <R : Record> SelectJoinStep<R>.applyViewerJoins(viewerId: UserId?): SelectJoinStep<R> {
        viewerId ?: return this
        return leftJoin(favByViewer)
            .on(favByViewer.ARTICLE_ID.eq(ARTICLE.ID).and(favByViewer.USER_ID.eq(viewerId.value)))
            .leftJoin(folByViewer)
            .on(folByViewer.FOLLOWEE_ID.eq(ARTICLE.AUTHOR_ID).and(folByViewer.FOLLOWER_ID.eq(viewerId.value)))
    }

    private fun articleFields(
        viewerId: UserId?,
        includeBody: Boolean = true,
    ): List<Field<*>> {
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

        return buildList<Field<*>> {
            add(ARTICLE.ID)
            add(ARTICLE.SLUG)
            add(ARTICLE.TITLE)
            add(ARTICLE.DESCRIPTION)
            if (includeBody) add(ARTICLE.BODY)
            add(ARTICLE.AUTHOR_ID)
            add(ARTICLE.CREATED_AT)
            add(ARTICLE.UPDATED_AT)
            add(author.USERNAME)
            add(author.BIO)
            add(author.IMAGE)
            add(
                multiset(
                    select(TAG.NAME)
                        .from(TAG)
                        .join(ARTICLE_TAG)
                        .on(ARTICLE_TAG.TAG_ID.eq(TAG.ID))
                        .where(ARTICLE_TAG.ARTICLE_ID.eq(ARTICLE.ID)),
                ).`as`("tags").convertFrom { it.map { r -> r.value1() } },
            )
            add(
                select(DSL.count())
                    .from(FAVORITE)
                    .where(FAVORITE.ARTICLE_ID.eq(ARTICLE.ID))
                    .asField<Int>("favoritesCount"),
            )
            add(favoritedField)
            add(followingField)
        }
    }

    private fun Record.toDto(): ArticleDto =
        ArticleDto(
            slug = req(ARTICLE.SLUG),
            title = req(ARTICLE.TITLE),
            description = req(ARTICLE.DESCRIPTION),
            body = req(ARTICLE.BODY),
            tagList = @Suppress("UNCHECKED_CAST") (get("tags") as? List<String> ?: emptyList()),
            createdAt = req(ARTICLE.CREATED_AT),
            updatedAt = req(ARTICLE.UPDATED_AT),
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

    private fun Record.toListItemDto(): ArticleListItemDto =
        ArticleListItemDto(
            slug = req(ARTICLE.SLUG),
            title = req(ARTICLE.TITLE),
            description = req(ARTICLE.DESCRIPTION),
            tagList = @Suppress("UNCHECKED_CAST") (get("tags") as? List<String> ?: emptyList()),
            createdAt = req(ARTICLE.CREATED_AT),
            updatedAt = req(ARTICLE.UPDATED_AT),
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
                ARTICLE.asterisk(),
                multiset(
                    dsl
                        .select(TAG.NAME)
                        .from(TAG)
                        .join(ARTICLE_TAG)
                        .on(ARTICLE_TAG.TAG_ID.eq(TAG.ID))
                        .where(ARTICLE_TAG.ARTICLE_ID.eq(ARTICLE.ID)),
                ).`as`("tags").convertFrom { it.map { r -> r.value1() } },
            ).from(ARTICLE)
            .where(condition)
            .fetchOne()
            ?.let { record ->
                @Suppress("UNCHECKED_CAST")
                val tags = record.get("tags") as? List<String> ?: emptyList()
                Article(
                    id = ArticleId(record.req(ARTICLE.ID)),
                    slug = record.req(ARTICLE.SLUG),
                    title = record.req(ARTICLE.TITLE),
                    description = record.req(ARTICLE.DESCRIPTION),
                    body = record.req(ARTICLE.BODY),
                    authorId = UserId(record.req(ARTICLE.AUTHOR_ID)),
                    tags = tags.toSet(),
                    createdAt = record.req(ARTICLE.CREATED_AT),
                    updatedAt = record.req(ARTICLE.UPDATED_AT),
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
                    .insertInto(TAG)
                    .set(TAG.NAME, tag)
                    .onConflict(TAG.NAME)
                    .doUpdate()
                    .set(TAG.NAME, tag)
            }
        dsl.batch(tagInserts).execute()

        val tagIds =
            dsl
                .select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.`in`(tags))
                .fetch()
                .associate { it.req(TAG.NAME) to it.req(TAG.ID) }

        val articleTagInserts =
            tags.mapNotNull { tag ->
                tagIds[tag]?.let { tagId ->
                    dsl
                        .insertInto(ARTICLE_TAG)
                        .set(ARTICLE_TAG.ARTICLE_ID, articleId.value)
                        .set(ARTICLE_TAG.TAG_ID, tagId)
                        .onConflict(ARTICLE_TAG.ARTICLE_ID, ARTICLE_TAG.TAG_ID)
                        .doNothing()
                }
            }
        if (articleTagInserts.isNotEmpty()) {
            dsl.batch(articleTagInserts).execute()
        }
    }
}
