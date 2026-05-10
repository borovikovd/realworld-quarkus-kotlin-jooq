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