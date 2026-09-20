# Nequi Franchise API — Project Context

Technical challenge (TalentPool Pragma × Nequi): a reactive Franchise Management API.
Domain: Franchise → has many Branches → each has many Products (name + numeric stock).

## Architecture

Bancolombia Clean Architecture scaffold (Gradle plugin `co.com.bancolombia.cleanArchitecture`), multi-module:

- `domain/model` — pure domain, **zero dependencies** (enforced by `./gradlew validateStructure`)
  - `co.com.bancolombia.model.aggregate` — `Franchise`, `Branch`, `Product`. `Franchise` is the aggregate root; `Branch`/`Product` are contained entities, no back-references to their parent.
  - `co.com.bancolombia.model.gateway` — output ports (interfaces only). Currently: `FranchiseRepository` (`save`, `findById`, both `Mono<Franchise>`).
  - `co.com.bancolombia.model.exception` — `BusinessException` (domain-level validation failures).
- `domain/usecase` — business logic, depends **only** on `model`. One use case class per business operation, constructor-injected with the gateway interface (`@RequiredArgsConstructor`).
- `infrastructure/entry-points/reactive-web` — `RouterFunction` + `Handler`. No `@RestController`, ever.
- `infrastructure/driven-adapters` — MongoDB Atlas adapter implementing `FranchiseRepository` (reactive driver, `ReactiveMongoRepository`/`ReactiveMongoTemplate`).
- `applications/app-service` — wiring only, the one module with `main()`.

Java 21 (LTS), fixed explicitly in `main.gradle` toolchain — not left at the plugin's default. Lombok (`@Value`, `@Builder(toBuilder = true)`) is injected to all subprojects centrally via `main.gradle`'s `subprojects {}` block — never add Lombok as a dependency inside an individual module's `build.gradle`.

## Reactive rules (Pilar 1 — strict, violations are automatic disqualification)

- `RouterFunction`/`HandlerFunction` only. `@RestController` is forbidden.
- No `try-catch` in reactive flow. Use `onErrorResume`/`onErrorMap`/`onErrorReturn`.
- `flatMap` when the mapped function returns a `Publisher` (`Mono`/`Flux`); `map` when the transformation is synchronous.
- No `if` for conditional logic inside a reactive chain — use `filter`, `switchIfEmpty`, etc.
- Never call `.block()` anywhere, including tests.
- Never use `java.util.stream` (`List.stream()...`) inside a reactive flow — convert with `Flux.fromIterable(list)` and continue with reactive operators instead. `java.util.List` itself as a plain in-memory container (entity fields, method params) is fine; the restriction is on the `Stream` API specifically, not on collections.
- Business validation (blank names, negative stock, not-found) is expressed as `filter(...).switchIfEmpty(Mono.error(new BusinessException(...)))`, not as thrown exceptions in an `if` block.

## Testing conventions

- `StepVerifier` (from `reactor-test`) for every use case test — never `.block()` to pull a result out for assertion.
- Mockito for mocking gateway interfaces (`Mockito.mock(FranchiseRepository.class)`), not real adapters.
- Test real business scenarios (happy path + failure/edge cases: blank name, entity not found, negative stock, empty list for max-stock query) — not just getters/setters or trivial builder round-trips.
- One test class per use case class, named `<UseCaseName>Test`, in the same package under `src/test/java`.
- JaCoCo coverage target: >70% on business-logic modules (`usecase`, `reactive-web`).
- PITest (mutation testing) is available in the build — a passing JaCoCo % isn't the only bar; tests should actually kill mutants, not just execute lines.

## Persistence

MongoDB Atlas (reactive driver, native `Mono`/`Flux` support) — chosen over RDS/R2DBC for driver simplicity with WebFlux, and over DynamoDB/ElastiCache for the nested aggregate document shape. Implementation lives only in `driven-adapters`, never referenced from `model` or `usecase`.

## Git / commits

Trunk-based: commit directly to `main` for normal work; short-lived branches (same day) only for changes that could leave `main` in a broken state temporarily (e.g. Terraform restructuring), merged back with `--no-ff`.

Commit messages follow the `commit-conventional` skill: pure Conventional Commits (`<type>(<scope>): <description>`), English, imperative mood, subject line ≤50-72 chars, no ticket references, no author scope, one logical change per commit. Never add a `Co-Authored-By` trailer or any Claude/Anthropic attribution to commit messages.

## Language

All code, comments, commit messages, and API documentation (OpenAPI/Swagger) in English — this is an explicit requirement of the challenge. README can be in English too for consistency with the rest of the repo.

## Lessons learned (Day 2)

- **`.body(flux, Class)` streaming bug**: `Handler.getTopStockProducts` used to build the response with `ServerResponse.ok().body(flux, TopStockProduct.class)`. That call returns an already-successful `Mono<ServerResponse>` — the `Flux` is only consumed later, while the body streams, *after* the 200 status is already committed. So `onErrorResume` never saw errors from the use case, and a `NotFoundException` surfaced as a raw 500 mid-stream instead of a 404. Fixed by collecting to a list first: `.collectList().flatMap(list -> ServerResponse.ok().bodyValue(list))`, so the error happens before the response is built and `onErrorResume` can map it correctly.
- **Lombok `@Value` + `@Builder` constructor visibility vs Jackson**: combining `@Value` and `@Builder(toBuilder = true)` on `Product`/`Branch`/`Franchise` makes Lombok generate the all-args constructor as package-private (intentional, to steer callers toward the builder). Jackson only auto-detects **public** constructors as creators, so it couldn't deserialize these types (e.g. as the `product` field inside `TopStockProduct`). Fixed with `@AllArgsConstructor(access = AccessLevel.PUBLIC)` alongside `@Value`/`@Builder`. Separately, also added `-parameters` to `javac` in `main.gradle`'s `subprojects` block — needed for Jackson to read constructor parameter names at all; `-parameters` alone was not sufficient without the public constructor fix, and the public constructor alone would not have worked without `-parameters` either.
- **Request DTOs live in `entry-points/reactive-web/api/dto`, not `domain/model`**: they're HTTP-shape records (`AddBranchRequest`, `RenameRequest`, etc.) tied to the JSON wire format, not domain concepts. Keeping them in the web layer preserves `domain/model`'s zero-dependency rule and lets the DTO shape evolve independently of the domain aggregates.
- **`NotFoundException` vs `BusinessException`**: `NotFoundException extends BusinessException` and is used specifically for "Franchise/Branch/Product not found" cases; `Handler.handleError` maps it to 404, while any other `BusinessException` (blank name, negative stock, etc.) maps to 400. Validation messages never became `NotFoundException` — only "not found" messages did.
- **`RouterRestTest`/`ConfigTest` are known-broken, deliberately deferred to Day 3**: both still reference the old scaffold route (`/api/usecase/path`) and don't provide `@MockitoBean`s for the 9 use cases `Handler` now requires, so they fail once Gradle's build cache stops masking it. This is a known gap, not a regression from today's work — fixing them (real route + `@MockitoBean` wiring, mirroring `HandlerTest`) is intentionally left for Day 3.
