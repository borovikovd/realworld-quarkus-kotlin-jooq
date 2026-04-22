# ![RealWorld Example App](https://raw.githubusercontent.com/realworld-apps/realworld/main/assets/media/realworld.png)

> ### Quarkus + Kotlin + jOOQ codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

### [Demo](https://demo.realworld.build/)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)

This codebase demonstrates a fully fledged backend built with **Quarkus + Kotlin + jOOQ**, organized in a hexagonal / ports-and-adapters layout with a CQRS-lite command/query split.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# How it works

**Stack:** Quarkus, Kotlin, jOOQ, PostgreSQL, Atlas (migrations), SmallRye JWT, Argon2id

**Architecture:** hexagonal (ports & adapters), layer-first packaging, CQRS-lite split between command and query sides.

```
com.example/
├── domain/                          # pure business model — no framework code
│   ├── <aggregate>/                 # Article, Comment, Profile, User
│   │   ├── Article.kt               # aggregate root (domain entity)
│   │   ├── ArticleRepository.kt     # command-side port
│   │   ├── ArticleId.kt             # typed ID (value object)
│   │   └── ... VOs (Slug, Email, Title, …)
│   ├── auth/                        # PasswordHashing, TokenIssuer, TokenVerifier ports
│   └── shared/                      # Entity/Repository base, Clock, exceptions
│
├── application/                     # use cases + read projections
│   ├── CurrentUser.kt               # cross-cutting port (who's calling)
│   ├── command/                     # write side
│   │   └── ArticleCommands.kt       # @Transactional mutations
│   └── query/                       # read side
│       ├── ArticleQueries.kt        # port for queries
│       └── readmodel/
│           └── ArticleReadModel.kt  # denormalized projection
│
├── infrastructure/                  # adapters
│   ├── persistence/jooq/<agg>/
│   │   ├── JooqArticleRepository.kt # implements ArticleRepository
│   │   └── JooqArticleQueries.kt    # implements ArticleQueries with jOOQ multiset
│   ├── security/                    # JWT adapters, rate limiter, logging MDC
│   ├── web/                         # JAX-RS exception mappers + filters
│   └── time/SystemClock.kt
│
├── presentation/rest/<agg>/
│   └── ArticleResource.kt           # JAX-RS, implements generated OpenAPI interface
│
├── api/                             # OpenAPI-generated interfaces + DTOs (build/)
└── jooq/                            # jOOQ-generated schema classes       (build/)
```

**Dependency direction (enforced by ArchUnit):**

```
presentation → application → domain
infrastructure → domain (adapters implement domain ports)
```

No layer depends inward-to-outward. `domain` has zero framework imports.

**Code generation:** OpenAPI spec generates API interfaces + DTOs (`./gradlew generateApi`). DB schema generates jOOQ classes (`./gradlew generateJooq`). Application code consumes the generated code, never the reverse.

**Schema management:** Atlas HCL (`db/schema.hcl`) is the source of truth. `atlas migrate diff` generates SQL migrations. No hand-written DDL.

# Design decisions

**CQRS-lite: commands vs queries.** The application layer is split into `command/` (mutations via aggregates + repository) and `query/` (reads via direct jOOQ projections). Resources inject both and call whichever they need — no service mediates reads just to forward them. Command-side protects invariants through the aggregate; query-side projects exactly what the UI needs, bypassing the write model entirely.

**jOOQ over Hibernate.** Full control over SQL. `multiset()` fetches nested collections (tags, author profile, favorite counts) in a single query — no N+1, no lazy loading, no entity graphs. The query side returns `*ReadModel` data classes built straight from result sets.

**Read models are not domain.** `ArticleReadModel`, `ProfileReadModel`, etc. live in `application/query/readmodel/`. They're use-case-shaped projections (viewer-relative `favorited` / `following` flags, cross-aggregate denormalization) — not aggregates and not subject to invariants. Keeping them out of `domain/` keeps the write model stable against presentation churn.

**Value objects for domain primitives.** `Email`, `Username`, `Slug`, `Title`, `ArticleId`, `UserId`, `CommentId`, `PasswordHash` — all `@JvmInline value class` with `init { require(...) }` invariants. Construction validates; domain code never sees unvalidated strings.

**Ports & adapters.** Domain defines what it needs (`UserRepository`, `PasswordHashing`, `TokenIssuer`, `TokenVerifier`, `Clock`, `ArticleQueries`). Infrastructure provides concrete implementations. Nothing in `domain/` imports Jakarta, Quarkus, JWT libraries, or jOOQ.

**Nullable returns over thrown exceptions in queries.** `ArticleQueries.getArticleBySlug(...)` returns `ArticleReadModel?`. The resource decides when a missing result is a 404. Adapters don't guess at error semantics.

**ArchUnit enforcement.** Layer direction, aggregate boundaries, DSL-context scoping, naming conventions, and transaction scoping are all compile-time tests — not conventions. The build fails if a query service becomes transactional, if a domain class imports jOOQ, or if a command crosses the layer boundary.

**OpenAPI-first.** The spec defines the contract. Generated models have protected constructors and fluent setters — this is intentional, forces explicit field-by-field construction. Resources implement the generated API interfaces, so breaking changes surface at compile time.

**Offset pagination.** The RealWorld spec uses offset/limit. Cursor-based would be better at scale, but deviating from the spec wasn't worth it here.

**No domain events.** Commands are synchronous. For 4 aggregates this is fine. At scale you'd introduce an event bus for cross-aggregate reactions.

# Getting started

**Prerequisites:** Java 21, Docker

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Apply migrations (install Atlas: https://atlasgo.io/getting-started)
atlas migrate apply --env local

# Run in dev mode (hot reload on http://localhost:8080)
./gradlew quarkusDev
```

Swagger UI: http://localhost:8080/swagger-ui/

## Tests

```bash
./gradlew build                                              # compile + tests + linting + spotbugs
./gradlew test --tests com.example.application.command.*    # unit tests only
./gradlew test --tests com.example.archunit.*               # architecture tests only
```

Integration tests use Testcontainers (real PostgreSQL). No mocked DB layer.

## Docker

```bash
docker-compose up --build    # PostgreSQL + API
```
