# ![RealWorld Example App](https://raw.githubusercontent.com/realworld-apps/realworld/main/assets/media/realworld.png)

> ### Quarkus + Kotlin + jOOQ codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

### [Demo](https://demo.realworld.build/)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)

This codebase demonstrates a fully fledged backend built with **Quarkus + Kotlin + jOOQ**, organized in a hexagonal / ports-and-adapters layout with a CQRS-lite command/query split.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# How it works

**Stack:** Quarkus, Kotlin, jOOQ, PostgreSQL, Atlas (migrations), SmallRye JWT, Argon2id, HashiCorp Vault, Google Tink

**Architecture:** hexagonal (ports & adapters), layer-first packaging, CQRS-lite split between command and query sides.

```
com.example/
├── domain/                          # pure business model — no framework code
│   ├── Entity.kt, AggregateRoot.kt, ValueObject.kt   # DDD building blocks
│   ├── aggregate/<agg>/             # article, comment, user
│   │   ├── Article.kt               # aggregate root
│   │   └── ArticleId.kt, Slug.kt, Title.kt, …        # value objects
│   ├── exception/                   # NotFound, Forbidden, Validation, …
│   └── service/SlugGenerator.kt     # stateless domain service
│
├── application/                     # use cases and ports
│   ├── inport/
│   │   ├── command/XCommands.kt     # inbound port interfaces — write side
│   │   └── query/XQueries.kt        # inbound port interfaces — read side
│   ├── readmodel/                   # use-case-shaped projections returned by queries
│   ├── outport/                     # Repository, XWriteRepository, XReadRepository,
│   │                                # FollowWriteRepository, Clock, CurrentUser,
│   │                                # PasswordHashing, TokenIssuer
│   └── service/
│       └── XApplicationService.kt  # implements XCommands + XQueries; @ApplicationScoped
│
├── infrastructure/                  # adapters
│   ├── rest/
│   │   ├── <agg>/XResource.kt       # JAX-RS, implements generated OpenAPI interface
│   │   └── exception/               # JAX-RS exception mappers + response filters
│   ├── persistence/jooq/<agg>/
│   │   ├── JooqArticleWriteRepository.kt  # implements ArticleWriteRepository
│   │   └── JooqArticleReadRepository.kt   # implements ArticleReadRepository w/ multiset
│   ├── security/                    # JWT adapters (JwtCurrentUser, JwtTokenIssuer),
│   │                                # Argon2id password hashing
│   ├── ratelimit/                   # in-process rate limiter + JAX-RS filter
│   ├── logging/                     # MDC request filter
│   └── time/SystemClock.kt
│
├── api/                             # OpenAPI-generated interfaces + DTOs (build/)
└── jooq/                            # jOOQ-generated schema classes       (build/)
```

**Dependency direction (enforced by ArchUnit):**

```
infrastructure → application → domain
(REST adapters, persistence adapters, and security live in infrastructure)
```

No layer depends inward-to-outward. `domain` has zero framework imports.

**Code generation:** OpenAPI spec generates API interfaces + DTOs (`./gradlew generateApi`). DB schema generates jOOQ classes (`./gradlew generateJooq`). Application code consumes the generated code, never the reverse.

**Schema management:** Atlas HCL (`db/schema.hcl`) is the source of truth. `atlas migrate diff` generates SQL migrations. No hand-written DDL.

# Design decisions

**CQRS-lite: commands vs queries.** The application layer separates write and read via inbound ports: `application/inport/command/XCommands` (mutations) and `application/inport/query/XQueries` (reads). Resources inject both interfaces. Command-side protects invariants through the aggregate; query-side projects exactly what the UI needs, bypassing the write model entirely.

**One ApplicationService per aggregate.** `XApplicationService` in `application/service/` implements both `XCommands` and `XQueries` for its aggregate. CDI resolves the single `@ApplicationScoped` bean for both interface injection points. `@Transactional` is declared per command method; query methods are unannotated.

**jOOQ over Hibernate.** Full control over SQL. `multiset()` fetches nested collections (tags, author profile, favorite counts) in a single query — no N+1, no lazy loading, no entity graphs. The query side returns `*ReadModel` data classes built straight from result sets.

**Read models are not domain.** `ArticleReadModel`, `ProfileReadModel`, etc. live in `application/readmodel/` — they're application-owned response shapes, the return types of query port methods. They're use-case-shaped projections (viewer-relative `favorited` / `following` flags, cross-aggregate denormalization) — not aggregates and not subject to invariants. Shared between inports and outports, so they sit at the `application/` root rather than nested under `inport/query/`. Keeping them out of `domain/` keeps the write model stable against REST-layer churn.

**Value objects for domain primitives.** `Email`, `Username`, `Slug`, `Title`, `ArticleId`, `UserId`, `CommentId`, `PasswordHash` — all `@JvmInline value class` with `init { require(...) }` invariants. Construction validates; domain code never sees unvalidated strings.

**Ports & adapters.** The application layer has two port layers: `application/inport/` (driving ports — interfaces through which callers invoke use cases) and `application/outport/` (driven ports — interfaces through which the application reaches infrastructure). Inbound ports are split by concern: `inport/command/` for mutations, `inport/query/` for reads. Outbound ports cover write repositories, read repositories, and collaborators (`PasswordHashing`, `TokenIssuer`, `TokenVerifier`, `Clock`, `CurrentUser`). Nothing in `domain/` imports Jakarta, Quarkus, JWT libraries, or jOOQ.

**Nullable returns over thrown exceptions in queries.** `ArticleQueries.getArticleBySlug(slug, viewerId): ArticleReadModel?` returns null for a miss. The resource decides when a missing result is a 404. Adapters don't guess at error semantics.

**ArchUnit enforcement.** Layer direction, aggregate boundaries, DSL-context scoping, naming conventions, and transaction scoping are all compile-time tests — not conventions. The build fails if a query service becomes transactional, if a domain class imports jOOQ, or if a command crosses the layer boundary.

**OpenAPI-first.** The spec defines the contract. Generated models have protected constructors and fluent setters — this is intentional, forces explicit field-by-field construction. Resources implement the generated API interfaces, so breaking changes surface at compile time.

**Offset pagination.** The RealWorld spec uses offset/limit. Cursor-based would be better at scale, but deviating from the spec wasn't worth it here.

**Field-level encryption with Tink + Vault.** Personal data (`email`, `username`, `bio`, `image`) is encrypted at rest using Google Tink AEAD (AES-256-GCM). HashiCorp Vault Transit wraps two Tink keysets at rest (one AEAD keyset for field encryption, two MAC keysets for lookup hashes). Vault is called exactly twice at startup to unwrap the keysets into process memory — zero Vault calls on the hot path. Lookup hashes (`email_hash`, `username_hash`, `token_hash`) use HMAC-SHA256 via a Tink MAC keyset, storing only the tag so raw values are never persisted. Cross-user binding is enforced via Tink associated data (`AD = userId || len(field) || field`), preventing ciphertext swap attacks between columns.

**No domain events.** Commands are synchronous. For 4 aggregates this is fine. At scale you'd introduce an event bus for cross-aggregate reactions.

# Getting started

**Prerequisites:** Java 21, Docker, [Atlas CLI](https://atlasgo.io/getting-started)

```bash
# 1. Start PostgreSQL and Vault
docker compose up -d postgres
docker run -d --rm --name vault-dev \
  -p 8200:8200 \
  -e VAULT_DEV_ROOT_TOKEN_ID=dev-root-token \
  hashicorp/vault:1.17

# 2. Apply DB migrations
atlas migrate apply --env local

# 3. Provision Tink keysets (first time, or after resetting Vault)
VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=dev-root-token \
  ./gradlew provisionKeysets | grep "^APP_TINK" > .env
# The .env file is gitignored. Vault ciphertexts are safe to store there locally.

# 4. Run in dev mode (hot reload on http://localhost:8080)
source .env && QUARKUS_HTTP_CORS_ORIGINS=http://localhost:3000 ./gradlew quarkusDev
```

Swagger UI: http://localhost:8080/swagger-ui/

## Tests

```bash
./gradlew build                        # compile + tests + linting + spotbugs
./gradlew test --tests "com.example.archunit.*"   # architecture tests only
```

Integration tests use Testcontainers (real PostgreSQL + Vault). No mocked infrastructure.

## Docker

The `docker-compose.yml` starts PostgreSQL for local development. The API image is not included — run the service directly with `./gradlew quarkusDev` during development or build a container image with the Quarkus container-image plugin.
