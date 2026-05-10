# Pragmatic Design — RealWorld/Conduit on Quarkus + Kotlin + jOOQ

A blog-scale CRUD API designed for production legibility, not architectural showcasing. Layered shape that any Java/Kotlin reviewer recognizes immediately, with deliberate divergences from MicroProfile defaults where they buy real engineering value.

## Philosophy

- **Package by feature**, not by layer. One package per aggregate; controller, service, repository, DTOs all in the same folder.
- **Three layers, each pulling its weight**: Resource (HTTP), Service (auth + transactions + orchestration), Repository (data access).
- **Two type families per feature, not one**: an internal `Article` entity for writes; an external `ArticleDto` for responses. They never share fields they shouldn't.
- **Hand-written DTOs with Bean Validation**, not OpenAPI codegen. Smallrye-OpenAPI generates the spec from annotations for Swagger UI; the contract is the code.
- **jOOQ over JPA.** Explicit, type-safe, schema-driven SQL. Conduit's per-viewer projections (`favorited`, `following`, `favoritesCount`) compose cleanly in jOOQ and ugly in JPA.
- **MP-canonical naming**: `XxxResource` per Jakarta REST convention. `XxxController` is Spring vocabulary.
- **Validate once, at the boundary.** Service trusts well-formed inputs.
- **Immutable entities, `copy()` for partial updates.** No Hibernate dirty-checking magic.

## Package layout

```
com/example/
├── article/
│   ├── ArticleResource.kt
│   ├── ArticleService.kt
│   ├── ArticleRepository.kt        // interface + JooqArticleRepository, one file
│   ├── Article.kt                  // ArticleId, Article, ArticleFilter, Page
│   ├── ArticleDtos.kt              // ArticleDto, NewArticle, ArticlePatch, …
│   ├── SlugGenerator.kt
│   └── TagResource.kt
├── comment/
│   ├── CommentResource.kt
│   ├── CommentService.kt
│   ├── CommentRepository.kt
│   ├── Comment.kt                  // CommentId, Comment
│   └── CommentDtos.kt              // CommentDto, NewComment
├── user/
│   ├── UserResource.kt              // /users, /user
│   ├── ProfileResource.kt           // /profiles/:username
│   ├── UserService.kt               // register, login, getCurrent, update, getProfile, follow, unfollow
│   ├── UserRepository.kt            // users + follows tables in one repo
│   ├── User.kt                      // UserId, User
│   └── UserDtos.kt                  // UserDto, RegisterUser, LoginUser, UpdateUser, ProfileDto
└── common/
    ├── persistence/ // jOOQ DSLContext config, req() extension
    ├── ratelimit/   // RateLimiter
    ├── security/    // CurrentUser, TokenIssuer, PasswordHashing, RefreshToken repos
    └── web/         // exception mappers, Validation, Patch
```

## Naming

| Role | Class name |
|---|---|
| HTTP endpoint | `XxxResource` (JAX-RS convention; not `Controller`) |
| Business orchestration | `XxxService` |
| Data access | `XxxRepository` (interface) + `JooqXxxRepository` (impl), same file |
| Entity (write-side) | bare noun: `Article`, `Comment`, `User` |
| Response DTO | `XxxDto` |
| Request DTO | `NewXxx` (create), `XxxPatch` (update) |
| Envelope | `XxxEnvelope`, `XxxListEnvelope` |
| Typed id | `XxxId` (`@JvmInline value class`) |

## Layering rules

1. **Resource** depends on **Service**, never on Repository.
2. **Service** depends on Repository, `CurrentUser`, `Clock`. Never on JAX-RS or jOOQ types.
3. **Repository** depends on jOOQ DSLContext. Never on `CurrentUser` or any HTTP concept.
4. **Cross-feature**: services and resources may depend on *other features' services*; repositories may not.
5. `@Transactional` on writes only. Reads stay unwrapped.

## Type families per feature

```
Article              — internal entity. Write-side load shape. Plain data class.
ArticleDto           — external response. Includes author profile + viewer flags.
NewArticle           — create input. Bean-Validated.
ArticlePatch         — update input. Nullable fields.
ArticleFilter, Page  — query params.
ArticleEnvelope      — { article: ArticleDto }, the JSON envelope.
```

The entity carries `id` and `authorId`; the DTO never does. No `@JsonIgnore` smell.

## Validation

- **Bean Validation on request DTOs**, triggered by `@Valid` at the resource boundary.
- **Optional `init { require(...) }` blocks on DTOs** as a construction-time safety net for non-resource callers (scheduled jobs, GraphQL handlers, internal callers). Belt and suspenders.
- **Service does not revalidate format constraints.** It enforces business invariants only: ownership, uniqueness (via DB), policy.
- **Throwing `IllegalArgumentException` from `init` maps to 400** via `IllegalArgumentExceptionMapper` in `common/web/`.

---

# Article — canonical example

## `ArticleDtos.kt`

```kotlin
package com.example.article

import com.example.user.ProfileDto
import com.example.user.UserId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

@JvmInline value class ArticleId(val value: UUID) {
    override fun toString() = value.toString()
    companion object { fun random() = ArticleId(UUID.randomUUID()) }
}

// Internal write-side entity. No behavior, no value-object scaffolding.
data class Article(
    val id: ArticleId,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val authorId: UserId,
    val tags: Set<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

// External response shape. No internal ids.
data class ArticleDto(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileDto,
)

data class ArticleEnvelope(val article: ArticleDto)
data class ArticleListEnvelope(val articles: List<ArticleDto>, val articlesCount: Int)

data class NewArticleRequest(@field:Valid val article: NewArticle)
data class NewArticle(
    @field:NotBlank @field:Size(max = 256)  val title: String,
    @field:NotBlank @field:Size(max = 1024) val description: String,
    @field:NotBlank                          val body: String,
    val tagList: List<@NotBlank @Size(max = 64) String> = emptyList(),
) {
    init { require(tagList.size <= 10) { "Too many tags" } }
}

data class UpdateArticleRequest(@field:Valid val article: ArticlePatch)
data class ArticlePatch(
    @field:Size(min = 1, max = 256)  val title: String?,
    @field:Size(min = 1, max = 1024) val description: String?,
    @field:Size(min = 1)              val body: String?,
)

data class ArticleFilter(val tag: String?, val author: String?, val favorited: String?)
data class Page(val limit: Int, val offset: Int)
```

## `ArticleRepository.kt`

```kotlin
package com.example.article

import com.example.common.persistence.req
import com.example.user.ProfileDto
import com.example.user.UserId
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import com.example.jooq.tables.references.*
import java.time.OffsetDateTime

interface ArticleRepository {
    // entity-shaped (writes + ownership/state loads)
    fun findBySlug(slug: String): Article?
    fun existsBySlug(slug: String): Boolean
    fun insert(article: Article)
    fun update(article: Article)
    fun deleteById(id: ArticleId)
    fun favorite(id: ArticleId, viewerId: UserId)
    fun unfavorite(id: ArticleId, viewerId: UserId)

    // projection-shaped (reads with author + viewer flags)
    fun findDtoBySlug(slug: String, viewerId: UserId?): ArticleDto?
    fun list(filter: ArticleFilter, page: Page, viewerId: UserId?): List<ArticleDto>
    fun feed(viewerId: UserId, page: Page): List<ArticleDto>
    fun count(filter: ArticleFilter): Int
    fun feedCount(viewerId: UserId): Int
}

@ApplicationScoped
class JooqArticleRepository(private val ctx: DSLContext) : ArticleRepository {

    // ---- writes ----------------------------------------------------

    override fun findBySlug(slug: String): Article? =
        ctx.select(
                ARTICLES.ID, ARTICLES.SLUG, ARTICLES.TITLE, ARTICLES.DESCRIPTION,
                ARTICLES.BODY, ARTICLES.AUTHOR_ID,
                multiset(
                    select(ARTICLE_TAGS.TAG).from(ARTICLE_TAGS)
                        .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID))
                ),
                ARTICLES.CREATED_AT, ARTICLES.UPDATED_AT,
            )
            .from(ARTICLES)
            .where(ARTICLES.SLUG.eq(slug))
            .fetchOne { r ->
                Article(
                    id          = ArticleId(r.value1().req()),
                    slug        = r.value2().req(),
                    title       = r.value3().req(),
                    description = r.value4().req(),
                    body        = r.value5().req(),
                    authorId    = UserId(r.value6().req()),
                    tags        = r.value7().map { it.value1()!! }.toSet(),
                    createdAt   = r.value8().req(),
                    updatedAt   = r.value9().req(),
                )
            }

    override fun existsBySlug(slug: String): Boolean =
        ctx.fetchExists(selectOne().from(ARTICLES).where(ARTICLES.SLUG.eq(slug)))

    override fun insert(article: Article) {
        ctx.insertInto(ARTICLES)
            .set(ARTICLES.ID,          article.id.value)
            .set(ARTICLES.SLUG,        article.slug)
            .set(ARTICLES.TITLE,       article.title)
            .set(ARTICLES.DESCRIPTION, article.description)
            .set(ARTICLES.BODY,        article.body)
            .set(ARTICLES.AUTHOR_ID,   article.authorId.value)
            .set(ARTICLES.CREATED_AT,  article.createdAt)
            .set(ARTICLES.UPDATED_AT,  article.updatedAt)
            .execute()
        if (article.tags.isNotEmpty()) writeTags(article.id, article.tags)
    }

    override fun update(article: Article) {
        ctx.update(ARTICLES)
            .set(ARTICLES.SLUG,        article.slug)
            .set(ARTICLES.TITLE,       article.title)
            .set(ARTICLES.DESCRIPTION, article.description)
            .set(ARTICLES.BODY,        article.body)
            .set(ARTICLES.UPDATED_AT,  article.updatedAt)
            .where(ARTICLES.ID.eq(article.id.value))
            .execute()
        // RealWorld doesn't change tags on update; skip rewriting them.
    }

    override fun deleteById(id: ArticleId) {
        ctx.deleteFrom(ARTICLES).where(ARTICLES.ID.eq(id.value)).execute()
    }

    override fun favorite(id: ArticleId, viewerId: UserId) {
        ctx.insertInto(ARTICLE_FAVORITES)
            .set(ARTICLE_FAVORITES.ARTICLE_ID, id.value)
            .set(ARTICLE_FAVORITES.USER_ID,    viewerId.value)
            .onConflictDoNothing()
            .execute()
    }

    override fun unfavorite(id: ArticleId, viewerId: UserId) {
        ctx.deleteFrom(ARTICLE_FAVORITES)
            .where(ARTICLE_FAVORITES.ARTICLE_ID.eq(id.value))
            .and(ARTICLE_FAVORITES.USER_ID.eq(viewerId.value))
            .execute()
    }

    private fun writeTags(articleId: ArticleId, tags: Set<String>) {
        var step = ctx.insertInto(ARTICLE_TAGS, ARTICLE_TAGS.ARTICLE_ID, ARTICLE_TAGS.TAG)
        tags.forEach { step = step.values(articleId.value, it) }
        step.onConflictDoNothing().execute()
    }

    // ---- reads -----------------------------------------------------

    override fun findDtoBySlug(slug: String, viewerId: UserId?): ArticleDto? =
        dtoSelect(viewerId).where(ARTICLES.SLUG.eq(slug)).fetchOne(::mapDto)

    override fun list(filter: ArticleFilter, page: Page, viewerId: UserId?): List<ArticleDto> =
        dtoSelect(viewerId)
            .where(filterConditions(filter))
            .orderBy(ARTICLES.CREATED_AT.desc())
            .limit(page.limit).offset(page.offset)
            .fetch(::mapDto)

    override fun feed(viewerId: UserId, page: Page): List<ArticleDto> =
        dtoSelect(viewerId)
            .where(ARTICLES.AUTHOR_ID.`in`(
                select(FOLLOWS.FOLLOWEE_ID).from(FOLLOWS)
                    .where(FOLLOWS.FOLLOWER_ID.eq(viewerId.value))
            ))
            .orderBy(ARTICLES.CREATED_AT.desc())
            .limit(page.limit).offset(page.offset)
            .fetch(::mapDto)

    override fun count(filter: ArticleFilter): Int =
        ctx.selectCount().from(ARTICLES)
            .join(USERS).on(USERS.ID.eq(ARTICLES.AUTHOR_ID))
            .where(filterConditions(filter))
            .fetchOne(0, Int::class.java) ?: 0

    override fun feedCount(viewerId: UserId): Int =
        ctx.selectCount().from(ARTICLES)
            .where(ARTICLES.AUTHOR_ID.`in`(
                select(FOLLOWS.FOLLOWEE_ID).from(FOLLOWS)
                    .where(FOLLOWS.FOLLOWER_ID.eq(viewerId.value))
            ))
            .fetchOne(0, Int::class.java) ?: 0

    private fun dtoSelect(viewerId: UserId?) =
        ctx.select(
            ARTICLES.SLUG, ARTICLES.TITLE, ARTICLES.DESCRIPTION, ARTICLES.BODY,
            ARTICLES.CREATED_AT, ARTICLES.UPDATED_AT,
            multiset(
                select(ARTICLE_TAGS.TAG).from(ARTICLE_TAGS)
                    .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID))
            ),
            field(exists(
                selectOne().from(ARTICLE_FAVORITES)
                    .where(ARTICLE_FAVORITES.ARTICLE_ID.eq(ARTICLES.ID))
                    .and(ARTICLE_FAVORITES.USER_ID.eq(viewerId?.value))
            )),
            field(
                select(count()).from(ARTICLE_FAVORITES)
                    .where(ARTICLE_FAVORITES.ARTICLE_ID.eq(ARTICLES.ID))
            ),
            USERS.USERNAME, USERS.BIO, USERS.IMAGE,
            field(exists(
                selectOne().from(FOLLOWS)
                    .where(FOLLOWS.FOLLOWER_ID.eq(viewerId?.value))
                    .and(FOLLOWS.FOLLOWEE_ID.eq(USERS.ID))
            )),
        )
        .from(ARTICLES)
        .join(USERS).on(USERS.ID.eq(ARTICLES.AUTHOR_ID))

    private fun filterConditions(f: ArticleFilter): Condition =
        listOfNotNull(
            f.tag?.let {
                exists(selectOne().from(ARTICLE_TAGS)
                    .where(ARTICLE_TAGS.ARTICLE_ID.eq(ARTICLES.ID))
                    .and(ARTICLE_TAGS.TAG.eq(it)))
            },
            f.author?.let { USERS.USERNAME.eq(it) },
            f.favorited?.let { username ->
                exists(selectOne().from(ARTICLE_FAVORITES)
                    .join(USERS.`as`("fav")).on(USERS.`as`("fav").ID.eq(ARTICLE_FAVORITES.USER_ID))
                    .where(ARTICLE_FAVORITES.ARTICLE_ID.eq(ARTICLES.ID))
                    .and(USERS.`as`("fav").USERNAME.eq(username)))
            },
        ).fold(noCondition()) { acc, c -> acc.and(c) }

    private fun mapDto(r: org.jooq.Record): ArticleDto = ArticleDto(
        slug           = r.get(ARTICLES.SLUG).req(),
        title          = r.get(ARTICLES.TITLE).req(),
        description    = r.get(ARTICLES.DESCRIPTION).req(),
        body           = r.get(ARTICLES.BODY).req(),
        createdAt      = r.get(ARTICLES.CREATED_AT).req(),
        updatedAt      = r.get(ARTICLES.UPDATED_AT).req(),
        tagList        = (r[6] as List<*>).map { (it as org.jooq.Record1<*>).value1() as String },
        favorited      = r.get(7, Boolean::class.java),
        favoritesCount = r.get(8, Int::class.java),
        author = ProfileDto(
            username  = r.get(USERS.USERNAME).req(),
            bio       = r.get(USERS.BIO).req(),
            image     = r.get(USERS.IMAGE).req(),
            following = r.get(12, Boolean::class.java),
        ),
    )
}
```

## `ArticleService.kt`

Load entity → `copy()` with patches → save → refetch DTO. Pure Kotlin partial-merge.

```kotlin
package com.example.article

import com.example.common.security.CurrentUser
import com.example.common.time.Clock
import com.example.common.web.ForbiddenException
import com.example.common.web.NotFoundException
import io.micrometer.core.annotation.Counted
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class ArticleService(
    private val articles: ArticleRepository,
    private val currentUser: CurrentUser,
    private val clock: Clock,
) {
    @Counted("article.creation.count")
    @Transactional
    fun create(input: NewArticle): ArticleDto {
        val authorId = currentUser.require()
        val slug = SlugGenerator.unique(input.title) { articles.existsBySlug(it) }
        val now = clock.now()
        articles.insert(Article(
            id          = ArticleId.random(),
            slug        = slug,
            title       = input.title,
            description = input.description,
            body        = input.body,
            authorId    = authorId,
            tags        = input.tagList.toSet(),
            createdAt   = now,
            updatedAt   = now,
        ))
        return articles.findDtoBySlug(slug, authorId)
            ?: error("article $slug vanished after insert")
    }

    @Transactional
    fun update(slug: String, patch: ArticlePatch): ArticleDto {
        val viewerId = currentUser.require()
        val current = articles.findBySlug(slug)
            ?: throw NotFoundException("Article not found")
        if (current.authorId != viewerId) {
            throw ForbiddenException("You can only update your own articles")
        }

        val newSlug = patch.title
            ?.takeIf { it != current.title }
            ?.let { SlugGenerator.unique(it) { c -> c != slug && articles.existsBySlug(c) } }
            ?: current.slug

        val updated = current.copy(
            slug        = newSlug,
            title       = patch.title       ?: current.title,
            description = patch.description ?: current.description,
            body        = patch.body        ?: current.body,
            updatedAt   = clock.now(),
        )
        articles.update(updated)
        return articles.findDtoBySlug(updated.slug, viewerId)!!
    }

    @Transactional
    fun delete(slug: String) {
        val viewerId = currentUser.require()
        val current = articles.findBySlug(slug)
            ?: throw NotFoundException("Article not found")
        if (current.authorId != viewerId) {
            throw ForbiddenException("You can only delete your own articles")
        }
        articles.deleteById(current.id)
        log.info("Article deleted: id={} slug={}", current.id, slug)
    }

    @Transactional
    fun favorite(slug: String): ArticleDto {
        val viewerId = currentUser.require()
        val current = articles.findBySlug(slug)
            ?: throw NotFoundException("Article not found")
        articles.favorite(current.id, viewerId)
        return articles.findDtoBySlug(slug, viewerId)!!
    }

    @Transactional
    fun unfavorite(slug: String): ArticleDto {
        val viewerId = currentUser.require()
        val current = articles.findBySlug(slug)
            ?: throw NotFoundException("Article not found")
        articles.unfavorite(current.id, viewerId)
        return articles.findDtoBySlug(slug, viewerId)!!
    }

    fun getBySlug(slug: String): ArticleDto =
        articles.findDtoBySlug(slug, currentUser.id)
            ?: throw NotFoundException("Article not found")

    fun list(filter: ArticleFilter, page: Page) = articles.list(filter, page, currentUser.id)
    fun count(filter: ArticleFilter) = articles.count(filter)
    fun feed(page: Page) = articles.feed(currentUser.require(), page)
    fun feedCount() = articles.feedCount(currentUser.require())

    companion object { private val log = LoggerFactory.getLogger(ArticleService::class.java) }
}
```

## `ArticleResource.kt`

```kotlin
package com.example.article

import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import org.jboss.resteasy.reactive.ResponseStatus

@Path("/api/articles")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@ApplicationScoped
class ArticleResource(private val articles: ArticleService) {

    @POST @ResponseStatus(201) @RolesAllowed("user")
    fun create(@Valid body: NewArticleRequest) = ArticleEnvelope(articles.create(body.article))

    @GET @Path("/{slug}")
    fun get(@PathParam("slug") slug: String) = ArticleEnvelope(articles.getBySlug(slug))

    @PUT @Path("/{slug}") @RolesAllowed("user")
    fun update(@PathParam("slug") slug: String, @Valid body: UpdateArticleRequest) =
        ArticleEnvelope(articles.update(slug, body.article))

    @DELETE @Path("/{slug}") @ResponseStatus(204) @RolesAllowed("user")
    fun delete(@PathParam("slug") slug: String) { articles.delete(slug) }

    @GET
    fun list(
        @QueryParam("tag") tag: String?,
        @QueryParam("author") author: String?,
        @QueryParam("favorited") favorited: String?,
        @QueryParam("limit")  @DefaultValue("20") limit: Int,
        @QueryParam("offset") @DefaultValue("0")  offset: Int,
    ): ArticleListEnvelope {
        val f = ArticleFilter(tag, author, favorited); val p = Page(limit, offset)
        return ArticleListEnvelope(articles.list(f, p), articles.count(f))
    }

    @GET @Path("/feed") @RolesAllowed("user")
    fun feed(
        @QueryParam("limit")  @DefaultValue("20") limit: Int,
        @QueryParam("offset") @DefaultValue("0")  offset: Int,
    ): ArticleListEnvelope {
        val p = Page(limit, offset)
        return ArticleListEnvelope(articles.feed(p), articles.feedCount())
    }

    @POST @Path("/{slug}/favorite") @RolesAllowed("user")
    fun favorite(@PathParam("slug") slug: String) = ArticleEnvelope(articles.favorite(slug))

    @DELETE @Path("/{slug}/favorite") @RolesAllowed("user")
    fun unfavorite(@PathParam("slug") slug: String) = ArticleEnvelope(articles.unfavorite(slug))
}
```

---

# Comment

Same shape, simpler. **No `Comment` entity** — RealWorld doesn't allow comment updates, so no partial-patch snapshot is needed. Repository returns `CommentDto` for reads, takes `(articleId, authorId, body, now)` for inserts, and a small `(commentId, authorId)` lookup for delete-ownership.

```kotlin
@JvmInline value class CommentId(val value: Long)

data class CommentDto(
    val id: CommentId,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val body: String,
    val author: ProfileDto,
)

data class NewCommentRequest(@field:Valid val comment: NewComment)
data class NewComment(@field:NotBlank @field:Size(max = 4096) val body: String)
```

`CommentService` is ~30 lines: `add`, `delete`, `listByArticle`. Cross-feature dependency: `articles.findIdBySlug(slug)` (or similar minimal lookup) lives on `ArticleRepository`.

# User / Profile

The `user/` package contains the services and repositories for user management:

- `UserService` — handles both user actions (register, login, get current, update self, mounted at `/api/users` and `/api/user`) and profile actions (get profile, follow, unfollow, mounted at `/api/profiles/:username`).
- Both sets of REST endpoints share `UserRepository`, which also owns the `follows` table. Following has no aggregate of its own; it's a join table with `follow(followerId, followeeId)` / `unfollow(followerId, followeeId)` / `findProfile(username, viewerId): ProfileDto?` methods on `UserRepository`.

`ProfileDto` carries `following: Boolean` computed by a viewer-aware projection in the same SQL that loads the user row. No N+1.

# Cross-cutting

## Auth

- **JWT access tokens** (~15 min) + **rotated refresh tokens** stored as HMAC, single-use, revoked on logout/rotation/erase.
- `CurrentUser` is `@RequestScoped`, populated by a JWT filter. Service code uses `currentUser.id` (nullable) for read paths and `currentUser.require()` for writes.
- `@RolesAllowed("user")` on resource methods that need authentication.
- Argon2id for password hashing; constant-time comparison for any secret check.
- Login / token redemption: equalize timing on found and not-found branches.

## Transactions

- `jakarta.transaction.@Transactional` on service write methods only.
- Reads are unwrapped — no transaction overhead, no `readOnly = true` ceremony.
- Resource-level `@Transactional` is forbidden (boundary is the business operation, not the HTTP request).

## Errors

Exception-to-HTTP mappers in `common/web/`:

| Exception | Status |
|---|---|
| `NotFoundException` | 404 |
| `ForbiddenException` | 403 |
| `UnauthorizedException` | 401 |
| `ValidationException` (cross-field; rare) | 422 |
| `IllegalArgumentException` (DTO `init` blocks) | 400 |
| `DataAccessException` (constraint violations) | 409 / 400 as appropriate |
| Anything else | 500 with sanitized body |

## Pagination

Default `limit = 20`, `offset = 0` declared inline at the resource via `@DefaultValue`. `Page(limit, offset)` is a small data class passed through.

# Decisions

## Kept (carried over from original codebase)

- jOOQ + Atlas migration pipeline.
- Argon2id, HKDF subkey derivation, Tink-based crypto.
- Refresh-token rotation, idempotency filter, rate limiting.
- Testcontainers for integration tests (real Postgres).
- JWT auth, `CurrentUser` request-scoped bean, `Clock` injected for determinism.
- Micrometer counters / metrics.

## Adopted (new direction)

- **Package-by-feature** instead of `domain/`/`application/`/`infrastructure/`.
- **`Article` as thin Kotlin data class** instead of aggregate root with value-object scaffolding.
- **One `ArticleService`** instead of `ArticleCommands` + `ArticleQueries` interfaces.
- **One `ArticleRepository`** (writes + read projections) instead of `ArticleRepository` + `ArticleFinder` split.
- **Repository as concrete class in feature package** — no port/adapter separation. Interface stays for test seam only.
- **Hand-written DTOs with Bean Validation** instead of OpenAPI-generated POJOs. Smallrye-OpenAPI generates the spec from annotations.
- **`XxxResource` naming** per Jakarta REST convention.
- **DTOs with no internal fields** — `ArticleDto` does not carry `authorId`. Separate `Article` entity for write-side state.
- **Optional `init { require(...) }` blocks on DTOs** as a safety net for non-resource callers.

## Rejected (alternatives considered)

- **Pure DTO-only design.** Removes the entity entirely; relies on `@JsonIgnore`'d internal fields on DTOs. Rejected because the annotation-discipline guarantee is weaker than a type-level guarantee, and `@JsonIgnore` is the kind of detail that breaks silently.
- **Logic pushed into `ArticleRepository.update(...)` returning sealed outcome.** Identified as a transaction-script pattern in repository's clothing. Rejected: roles get muddled. Auth lives in the service; data access lives in the repo.
- **Transaction Script (`ArticleUseCases`) one class per feature.** Defensible at this scale, but breaks the layered convention reviewers expect. Reserved for an even smaller app.
- **JPA + `@Entity` + Hibernate.** MP-canonical but produces ugly native-SQL escape hatches for projection-heavy reads. The convention-matching benefit doesn't pay for the loss of compile-time SQL safety.
- **OpenAPI-first codegen.** Strong contract enforcement, but the generated POJOs are non-idiomatic Kotlin (mutable, fluent setters), and there's no second consumer of the spec to justify the build complexity.
- **Active record / `@Inject EntityManager` directly in the resource.** The Quarkus tutorial shape; fine for demos, doesn't scale to a real codebase.

# When to revisit

Replace this design *only* when one of these triggers fires:

- **Real domain logic appears.** Drafts vs published, scheduled posts, version history. Then `Article` grows methods and a `Status` enum, and the service shrinks toward orchestration.
- **A second adapter to the same data** beyond REST + maybe GraphQL. Repository abstraction earns its keep when there's a non-HTTP write path with different concurrency or transaction semantics.
- **Multi-tenant or org-scoped operations.** Auth gets richer (ABAC, row-level security) and the service might benefit from a use-case-handler split.
- **Performance asymmetry between reads and writes.** If reads outscale writes by 100×, a CQRS split with separate read replicas / projections becomes worth its complexity. Until then, one repository, two return shapes.
- **The team grows past ~10 engineers.** Stricter layering and more enforcement (ArchUnit rules, module boundaries) start paying for themselves.

For a Conduit-scale CRUD blog with one team and one transport, this design is the right ceiling. Going lighter (transaction script) is defensible. Going heavier (hexagonal aggregates) is not.
