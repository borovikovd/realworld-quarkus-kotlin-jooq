# ![RealWorld Example App](https://raw.githubusercontent.com/realworld-apps/realworld/main/assets/media/realworld.png)

# Realworld Backend — Quarkus + Kotlin + jOOQ

> A production-grade REST API implementing the [RealWorld](https://github.com/gothinkster/realworld) spec, built with the kind of engineering rigour you'd expect at a fintech. Hexagonal architecture, field-level encryption, idempotency keys, envelope encryption with Vault + Tink, Argon2id, single-use refresh tokens, ArchUnit-enforced layer boundaries.

### [RealWorld spec](https://github.com/gothinkster/realworld)&nbsp;&nbsp;&nbsp;&nbsp;[API docs (Swagger)](http://localhost:8080/swagger-ui/)

---

## Stack

| Layer | Technology |
|---|---|
| Runtime | Quarkus 3.x (JVM), Kotlin |
| Persistence | PostgreSQL, jOOQ, Atlas (migrations) |
| Auth | SmallRye JWT (RS256), Argon2id, single-use refresh tokens |
| Encryption | Google Tink (AES-256-GCM + HMAC-SHA256), HashiCorp Vault Transit |
| Observability | Micrometer, OpenTelemetry, structured JSON logging |
| Quality | ArchUnit, ktlint, detekt, SpotBugs + FindSecBugs, OWASP dependency-check |

---

## What makes this interesting

**Hexagonal architecture with compile-time enforcement.** Three-layer hexagonal layout (`domain → application → infrastructure`) enforced by ArchUnit. The build fails if a domain class imports jOOQ, a query service gains `@Transactional`, or a resource bypasses the port layer. Not conventions — tests.

**Field-level encryption with zero hot-path Vault calls.** Personal data (`email`, `username`, `bio`, `image`) is encrypted at rest with AES-256-GCM via Google Tink. Two Tink keysets (one AEAD, two MAC) are wrapped at rest by a Vault Transit KEK and unwrapped exactly twice at startup — never on the request path. Lookup hashes use HMAC-SHA256 with a separate keyset from the token MAC keyset, so each can rotate independently. Associated data `AD = userId ∥ len(field) ∥ field` prevents cross-column ciphertext swap attacks.

**Idempotency keys on all POST endpoints.** Clients send an `Idempotency-Key` header; the server stores the response and replays it on duplicate requests. Keys are scoped per user so one user's key can't replay another's response. Concurrent duplicates get 409 + `Retry-After: 1`. Server errors delete the record so the next retry gets a fresh attempt. 24-hour expiry with a daily cleanup job.

**Single-use refresh tokens with optimistic locking.** Refresh tokens are stored as HMAC tags only (never the raw token). Rotation uses a conditional UPDATE (`WHERE revoked_at IS NULL`) — if two concurrent requests race, only one wins and the other gets 401. Revoke + issue happens in a single `@Transactional` boundary so a mid-flight crash can't lock out the user.

**CQRS-lite command/query split.** Inbound ports are split: `XCommands` for mutations, `XQueries` for reads. Command side enforces invariants through the aggregate; query side bypasses the write model entirely and returns use-case-shaped read models. `@Transactional` is declared only on command methods — enforced by ArchUnit.

**jOOQ with multiset.** No ORM, no lazy loading, no N+1. `multiset()` fetches nested collections (tags, favorite counts, author profiles with follow status) in a single SQL query. Query side returns `*ReadModel` data classes built directly from result sets.

**OpenAPI-first contract.** The spec (`openapi.yaml`) is the source of truth. Quarkus generates API interfaces and DTOs at build time. Resources implement the generated interfaces — breaking spec changes surface as compile errors, not runtime surprises.

**Timing equalization on auth flows.** Login always runs Argon2 verification even for unknown emails (using a pre-computed dummy hash) so response latency doesn't leak account existence. The dummy hash is computed eagerly at startup, not lazily on first request.

---

## Architecture

```
com.example/
├── domain/                  # pure business model — zero framework imports
│   ├── aggregate/<agg>/     # aggregates with value objects (Email, Slug, …)
│   ├── exception/           # domain exceptions (NotFound, Forbidden, …)
│   └── service/             # stateless domain services
│
├── application/             # use cases and ports
│   ├── inport/command/      # XCommands — inbound write ports
│   ├── inport/query/        # XQueries  — inbound read ports
│   ├── outport/             # driven ports: repositories, Clock, CryptoService, …
│   ├── readmodel/           # use-case projections returned by query ports
│   └── service/             # XApplicationService implements XCommands + XQueries
│
└── infrastructure/          # adapters (depends on application, never on domain directly)
    ├── rest/                # JAX-RS resources + exception mappers
    ├── persistence/jooq/    # jOOQ repository implementations
    ├── security/            # TinkCryptoService, JwtTokenIssuer, Argon2PasswordHashing
    ├── idempotency/         # IdempotencyFilter (request + response)
    ├── ratelimit/           # in-process rate limiter
    └── logging/             # MDC filter (requestId, userId)
```

Dependency rule: `infrastructure → application → domain`. Enforced by ArchUnit at build time.

---

## Getting started

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

# 3. Provision Tink keysets (once per Vault instance)
VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=dev-root-token \
  ./gradlew provisionKeysets | grep "^APP_TINK" > .env

# 4. Run (hot reload)
source .env && QUARKUS_HTTP_CORS_ORIGINS=http://localhost:3000 ./gradlew quarkusDev
```

Swagger UI: http://localhost:8080/swagger-ui/

## Tests

```bash
./gradlew build                              # compile + tests + lint + spotbugs
./gradlew test --tests "com.example.archunit.*"  # architecture tests only
```

Integration tests use Testcontainers (real PostgreSQL + Vault). No mocked infrastructure.

## Docker

`docker-compose.yml` starts PostgreSQL for local development. Build a container image with `./gradlew build -Dquarkus.container-image.build=true` using the Quarkus Docker plugin.
