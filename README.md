# ![RealWorld Example App](https://raw.githubusercontent.com/realworld-apps/realworld/main/assets/media/realworld.png)

> ### Quarkus + Kotlin + jOOQ codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

### [Demo](https://demo.realworld.build/)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)

This codebase was created to demonstrate a fully fledged backend application built with **Quarkus + Kotlin + jOOQ** including CRUD operations, authentication, routing, pagination, and more.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# How it works

**Stack:** Quarkus, Kotlin, jOOQ, PostgreSQL, Atlas (migrations), SmallRye JWT, Argon2id

**Architecture:** DDD-lite with CQRS read/write separation, organized by aggregate:

```
com.example/
├── article/        # Article aggregate (tags, favorites)
├── comment/        # Comment aggregate
├── user/           # User aggregate (registration, auth)
├── profile/        # Follow relationships
└── shared/         # Security, exceptions, domain base classes
```

Each aggregate contains:
- `*WriteService.kt` — commands, `@Transactional`, returns typed domain IDs
- `*ReadService.kt` — queries, jOOQ multiset, returns read-side data classes (e.g., `ArticleSummary`)
- `*Repository.kt` — interface, implemented by `Jooq*Repository.kt`
- `*Resource.kt` — JAX-RS endpoint, delegates to WriteService + ReadService, maps to API DTOs
- Domain entity — class with behavior methods, `@AggregateRoot` annotated

**Code generation:** OpenAPI spec generates API interfaces and DTOs (`gradle generateApi`). DB schema generates jOOQ classes (`gradle generateJooq`). Application code implements/uses the generated code, never the reverse.

**Schema management:** Atlas HCL (`db/schema.hcl`) is the source of truth. `atlas migrate diff` generates SQL migrations. No hand-written DDL.

# Design decisions

**jOOQ over Hibernate.** Full control over SQL. `multiset()` fetches nested collections (tags, author profile, favorite counts) in a single query — no N+1 surprises, no lazy loading, no entity graphs. Trade-off: more verbose for simple CRUD, but every query is visible and optimizable.

**Typed domain IDs.** `ArticleId`, `UserId`, `CommentId` as `@JvmInline value class`. Services return these after commands instead of full entities. This prevents leaking domain internals (e.g., `User.passwordHash`) through the service layer and makes the command/query boundary explicit: write returns ID, read hydrates it.

**ReadService for reads.** ReadServices run optimized jOOQ queries and return plain Kotlin data classes (`ArticleSummary`, `UserSummary`, etc.). Only Resources are allowed to import OpenAPI generated models — enforced by ArchUnit.

**ArchUnit enforcement.** Architecture rules are tested, not documented. Aggregate boundaries, layer dependencies, technology boundaries, naming conventions, and scope/transaction rules are all compile-time verified. Rules catch violations like a service importing an API DTO or a repository managing transactions.

**OpenAPI-first.** The spec defines the contract. Generated models have protected constructors and fluent setters — this is intentional, prevents construction with missing fields. Resources implement generated API interfaces, so breaking changes are caught at compile time.

**Offset pagination, not cursor.** The RealWorld spec uses offset/limit. Cursor-based would be better for production at scale, but deviating from the spec wasn't worth it here.

**No domain events.** Commands are synchronous. For a CRUD app with 4 aggregates this is fine. At scale, you'd introduce an event bus for cross-aggregate reactions (e.g., "article favorited" triggering feed updates).

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
./gradlew build              # compilation + tests + linting
./gradlew test --tests ArticleServiceTest  # single test class
```

Tests use Testcontainers (real PostgreSQL), no mocked DB layer.

## Docker

```bash
docker-compose up --build    # PostgreSQL + API
```
