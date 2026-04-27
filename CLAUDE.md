# Claude Code Assistant Guidelines

Production-grade code. Idiomatic Kotlin + Quarkus + jOOQ on hexagonal architecture: domain → application (ports) → infrastructure (adapters).

## Workflow

1. **Read before you write.** Understand what depends on the symbol you are changing. A "dead code" bug may have callers that rely on the bug; check the full call graph, not just the obvious site.
2. **Write code and tests together.** Don't push code without the test that proves it.
3. **Run the full gate before claiming done.** `./gradlew build` runs compile + tests + ktlint + detekt + architecture tests. Pushed ≠ green; if you didn't run it, you didn't verify it.
4. **Regenerate after schema/spec changes.** OpenAPI, jOOQ, and other generators consume input files; relying on stale generated symbols is a common trap. Discover the right task with `./gradlew tasks` — don't memorize names.
5. **Conventional commits.** `feat/fix/refactor/test/chore/docs(scope): subject`. Keep commits focused; split independent concerns rather than bundling.

## Code

- Prefer `val`, immutable collections, scope functions, null-safe operators. Use `!!` only for invariants you can prove.
- **No comments unless the WHY is non-obvious.** Warnings about hacks, subtle invariants, security tradeoffs — yes. Restating WHAT the code does — no.
- **No speculative engineering.** No fallbacks, retries, or validation for cases that can't happen. No abstractions for the second caller until it exists.
- **No breadcrumbs for removed code.** Delete it; don't leave shims, re-exports, or `// removed` markers.
- Validate input only at system boundaries. Trust internal code.

## Architecture (enforced by ArchUnit)

- **Outbound port packages contain interfaces only.** DTOs, read models, and value-bag types live in dedicated DTO/readmodel packages, never in port packages.
- **Scope correctly.** `@ApplicationScoped` for stateless services, `@RequestScoped` for per-request state. Never store request data in singletons.
- **Transactions on writes only.** Mark command-side service methods `@Transactional`; reads don't need it.
- **Aggregates, not rows.** Repositories return whole aggregates. DTO mapping happens at the REST layer.
- **Domain throws domain exceptions; mappers translate to HTTP.** Any exception without a mapper becomes a 500 leak — register a mapper instead.

## Persistence

- Schema flow: edit HCL → generate migration → apply → regenerate jOOQ.
- When you add a migration, verify the test resource picks up new files — don't assume it loads the whole directory.
- Build query conditions in a list, apply once. Don't reassign query variables across `.where()`.
- Use `multiset` for nested collections; never query inside a `map`/`forEach`.
- `fetchOne()` is nullable. Resolve with `?:`, not `!!`.

## Security

- Argon2id for passwords. Constant-time comparison for any secret check.
- Derive subkeys from a master key via HKDF; never log secrets.
- Auth tokens: short-lived access (~15 min) plus long-lived refresh tokens that are single-use, rotated, stored only as HMACs, and revoked on logout / rotation / user erase.
- **Equalize timing on lookup-then-verify flows** (login, password reset, token redemption). Run the verify step on both the found and not-found branches so latency does not leak existence.
- Security-critical config (CORS origins, master keys, signing keys) must fail closed: required env vars, no permissive defaults like `*`.

## Tests

- Integration tests hit real PostgreSQL via Testcontainers. No DB mocks.
- **In-memory state in `@ApplicationScoped` beans (caches, rate limiters, counters) survives across tests in the same JVM.** When you add such state, also add config knobs that let the test profile disable or relax it.
- After changing shared infrastructure (filters, interceptors, generated code, schema), run the **full** suite — local effects may break distant tests.
- Don't try to assert on timing in unit tests; verify behavior (status codes, side effects) instead.

## Pitfalls

- **Detekt is part of the gate.** Thresholds are typically inclusive: count == threshold fails. Magic numbers, return count, function count, and complexity rules trip easily — fix the code or adjust the threshold deliberately, with a one-line reason.
- **A correctness fix that activates previously-dead code can break tests that relied on the bug.** Run the full suite; don't trust scoped test runs.
- **Generated code is gitignored.** Regenerate after schema/spec edits and review the diff for accidental drift.
- **Don't trust documented task or symbol names blindly** — including names in this file. The codebase is the source of truth; verify with `./gradlew tasks`, `grep`, or a quick read.
- When unsure whether to do extra work, ask. Silently "improving" a user's recommendation is worse than asking.
