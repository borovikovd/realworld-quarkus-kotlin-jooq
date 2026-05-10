# ![RealWorld Example App](logo.png)

> ### Quarkus + Kotlin + jOOQ codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.


### [Demo](https://demo.realworld.build/)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)


This codebase was created to demonstrate a fully fledged fullstack application built with **Quarkus** including CRUD operations, authentication, routing, pagination, and more.

We've gone to great lengths to adhere to the **Quarkus** community styleguides & best practices.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.


# How it works

A production-grade REST API implementing the RealWorld spec. Package-by-feature layout, Argon2id passwords, single-use refresh tokens.

## Stack

| Layer | Technology |
|---|---|
| Runtime | Quarkus 3.x (JVM), Kotlin |
| Persistence | PostgreSQL, jOOQ, Atlas (migrations) |
| Auth | SmallRye JWT (RS256), Argon2id, single-use refresh tokens |
| Observability | Micrometer, OpenTelemetry, structured JSON logging |
| Quality | ktlint, detekt, SpotBugs + FindSecBugs, OWASP dependency-check |

## Package layout

```
com.example/
├── article/        ArticleResource, ArticleService, ArticleRepository, ArticleDtos, SlugGenerator
├── comment/        CommentResource, CommentService, CommentRepository, CommentDtos
├── user/           UserResource, ProfileResource, UserService, ProfileService, UserRepository, UserDtos
├── tag/            TagResource
└── common/
    ├── security/   CurrentUser, PasswordHashing, TokenIssuer,
    │               RefreshTokenRepository, RevokedTokenRepository,
    │               RevokedTokenFilter, RefreshTokenCleanupJob
    ├── web/        Exception mappers, Filters (MDC, status code), Validation
    ├── persistence/ JooqConfiguration, req() extension
    └── time/       Clock
```

## Highlights

**Single-use refresh tokens with optimistic locking.** Refresh tokens are stored as SHA-256 hashes only (never the raw token). Rotation uses a conditional UPDATE (`WHERE revoked_at IS NULL`) — if two concurrent requests race, only one wins and the other gets 401. Revoke + issue happens in a single `@Transactional` boundary so a mid-flight crash can't lock out the user.

**jOOQ with multiset.** No ORM, no lazy loading, no N+1. `multiset()` fetches nested collections (tags, favorite counts, author profiles with follow status) in a single SQL query.

**Timing equalization on auth flows.** Login always runs Argon2 verification even for unknown emails (using a pre-computed dummy hash) so response latency doesn't leak account existence.

**Bean Validation at the boundary.** Format constraints (`@NotBlank`, `@Size`, `@Email`) live on request DTOs and are enforced at the resource boundary. Business invariants (uniqueness, ownership) stay in services and throw `ValidationException` directly, which maps to 422 with field-level error detail.


# Getting started

**Prerequisites:** Java 21, Docker, [Atlas CLI](https://atlasgo.io/getting-started)

```bash
# 1. Start PostgreSQL
docker compose up -d postgres

# 2. Apply DB migrations
atlas migrate apply --env local

# 3. Run (hot reload)
QUARKUS_HTTP_CORS_ORIGINS=http://localhost:3000 ./gradlew quarkusDev
```

Swagger UI: http://localhost:8080/swagger-ui/

## Tests

```bash
./gradlew build   # compile + tests + lint + spotbugs
```

Integration tests use Testcontainers (real PostgreSQL). No mocked infrastructure.

## Docker

`docker-compose.yml` starts PostgreSQL for local development. Build a container image with `./gradlew build -Dquarkus.container-image.build=true` using the Quarkus Docker plugin.
