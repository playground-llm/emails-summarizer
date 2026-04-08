---
name: api-rs-code-layout
description: Established the layout of the api-rs Spring Boot project, including package structure, file roles, and naming conventions.
compatibility: api-rs subproject only.
---

# api-rs Code Layout Skill

Understand and follow the layout of the `api-rs` Spring Boot project before writing any backend code.
All source code lives under `api-rs/src/main/java/com/emailssummarizer/apirs/`.
Tests live under `api-rs/src/test/java/com/emailssummarizer/apirs/` — mirroring the main package tree.

## Project Structure

```
api-rs/
├── build.gradle                          ← Gradle build script (Spring Boot 4, Java 21)
└── src/
    ├── main/
    │   ├── java/com/emailssummarizer/apirs/
    │   │   ├── ApiRsApplication.java             ← Spring Boot entry point (@SpringBootApplication)
    │   │   │
    │   │   ├── category/                         ← Category feature slice
    │   │   │   ├── Category.java                 ← JPA entity (@Entity, table: categories)
    │   │   │   ├── CategoryController.java        ← REST controller (@RestController, /categories); delegates to CategoryService
    │   │   │   ├── CategoryRepository.java        ← Spring Data JPA repository; called only from CategoryService
    │   │   │   ├── CategoryRequest.java           ← Request DTO (record)
    │   │   │   └── CategoryService.java           ← @Service; owns all business logic, validation, and orchestration
    │   │   │
    │   │   ├── message/                          ← Message feature slice
    │   │   │   ├── Message.java                  ← JPA entity (@Entity, table: messages)
    │   │   │   ├── MessageController.java         ← REST controller (@RestController, /messages); delegates to MessageService
    │   │   │   ├── MessageRepository.java         ← Spring Data JPA repository; called only from MessageService
    │   │   │   ├── MessageRequest.java            ← Request DTO (record)
    │   │   │   └── MessageService.java            ← @Service; owns all business logic, validation, and orchestration
    │   │   │
    │   │   ├── oauth/                            ← OAuth2 token exchange
    │   │   │   └── OAuthController.java           ← POST /oauth2/token proxy to GitHub
    │   │   │
    │   │   └── security/                         ← Spring Security configuration
    │   │       ├── GitHubOpaqueTokenIntrospector.java  ← Validates GitHub tokens, assigns ROLE_READ/EDIT/DEL
    │   │       └── ResourceServerConfig.java           ← SecurityFilterChain, CORS, per-method authorization
    │   │
    │   └── resources/
    │       ├── application.yml                   ← Spring Boot config (H2, OAuth2, app.readers/editors/deleters)
    │       ├── schema.sql                        ← DDL run at startup (categories, messages tables)
    │       └── data.sql                          ← Seed data run at startup
    │
    └── test/
        ├── java/com/emailssummarizer/apirs/
        │   ├── ApiRsApplicationTests.java        ← Spring Boot smoke test (@SpringBootTest)
        │   │
        │   ├── category/
        │   │   ├── CategoryControllerTest.java   ← @WebMvcTest slice — HTTP layer only; CategoryService mocked with @MockitoBean
        │   │   ├── CategoryRepositoryTest.java   ← @DataJpaTest slice — DB queries against H2
        │   │   └── CategoryServiceTest.java      ← plain JUnit + Mockito — business logic with mocked repository
        │   │
        │   ├── message/
        │   │   ├── MessageControllerTest.java    ← @WebMvcTest slice — HTTP layer only; MessageService mocked with @MockitoBean
        │   │   ├── MessageRepositoryTest.java    ← @DataJpaTest slice — DB queries against H2
        │   │   └── MessageServiceTest.java       ← plain JUnit + Mockito — business logic with mocked repository
        │   │
        │   └── security/
        │       └── SecurityFilterChainTest.java  ← @SpringBootTest + MockMvc — authorization rules (401/403)
        │
        └── resources/
            └── application.yml                   ← Test overrides (H2 in-memory, fixed allow-lists for roles)
```

## Conventions

### Feature slices
Each domain concept (category, message) gets its own package containing:
- `<Name>.java` — JPA entity, maps directly to a DB table
- `<Name>Controller.java` — `@RestController`; handles HTTP only (status codes, request/response mapping); delegates all logic to `<Name>Service`; never injects a repository directly
- `<Name>Service.java` — `@Service`; owns all business logic, cross-entity validation, and error conditions (duplicate checks, FK safety, not-found throws); the only class allowed to inject repositories
- `<Name>Repository.java` — `JpaRepository`; called only from `<Name>Service`, never from a controller or another service
- `<Name>Request.java` — Java `record` used as request body DTO (keeps entity fields private)

### Layering rule

```
Controller  →  Service  →  Repository
```

- Controllers must not import or call any `Repository` directly.
- Services must not import or call another feature's `Controller`.
- Cross-feature access (e.g. `MessageService` needing to check a category) goes through the other feature's `Service` or `Repository`, **not** its `Controller`.

### Security
- `GitHubOpaqueTokenIntrospector` calls `GET https://api.github.com/user` to validate tokens and assigns authorities
- Three allow-lists injected via `@Value("${app.readers:}")`, `${app.editors:}`, `${app.deleters:}` → env vars `READERS_GITHUB_LOGINS`, `EDITORS_GITHUB_LOGINS`, `DELETERS_GITHUB_LOGINS`
- `ResourceServerConfig` declares per-method rules: GET→ROLE_READ, POST/PUT→ROLE_EDIT, DELETE→ROLE_DEL

### HTTP status codes
| Situation | Status |
|-----------|--------|
| Successful GET / PUT | 200 |
| Successful POST (created) | 201 |
| Successful DELETE | 204 |
| Resource not found | 404 |
| Conflict (duplicate code, FK violation) | 409 |
| Missing / invalid token | 401 |
| Authenticated but missing role | 403 |

### Database
- H2 in-memory, initialized from `schema.sql` then `data.sql` at startup
- `categories` table: `code` (PK, VARCHAR), `name`, `description`
- `messages` table: `id` (PK, auto-increment), `title`, `body`, `category_code` (FK → categories.code)

### Testing

Each test class mirrors the package of the class under test.

| Test class | Annotation | What it covers |
|---|---|---|
| `<Name>ControllerTest` | `@WebMvcTest(<Name>Controller.class)` | HTTP status, request mapping, JSON serialisation; `<Name>Service` mocked with `@MockitoBean` |
| `<Name>ServiceTest` | plain JUnit 5 + Mockito | Business logic, validation, error conditions; repository mocked with `@Mock` / `@InjectMocks` |
| `<Name>RepositoryTest` | `@DataJpaTest` | Custom query methods against the H2 in-memory DB; schema.sql / data.sql loaded automatically |
| `SecurityFilterChainTest` | `@SpringBootTest` + `MockMvc` | Per-endpoint 401 (no token) and 403 (wrong role) rules declared in `ResourceServerConfig` |
| `ApiRsApplicationTests` | `@SpringBootTest` | Smoke test — context loads without errors |

Test resource overrides (`src/test/resources/application.yml`) must at minimum set:
```yaml
app:
  readers: reader-user
  editors: editor-user
  deleters: deleter-user
```
so role-based tests have deterministic allow-lists independent of env vars.

## Steps Before Writing Code

1. Identify which feature slice the new code belongs to (category, message, or new package).
2. Follow the existing slice structure: entity → repository → request record → service → controller.
3. Put all business logic, validation, and error conditions in `<Name>Service`; keep the controller thin (HTTP mapping only).
4. Add any new custom repository methods with explicit names (`findByCode`, `existsByCategoryCode`, etc.).
5. For new endpoints, declare authorization rules in `ResourceServerConfig.securityFilterChain()`.
6. If the endpoint touches a new DB table, add DDL to `schema.sql` and optional seed rows to `data.sql`.
7. For each new class, add a corresponding test class in the mirrored package under `src/test/`:
   - Controller → `@WebMvcTest` with service mocked via `@MockitoBean`
   - Service → plain JUnit 5 + Mockito (`@Mock` / `@InjectMocks`)
   - Repository → `@DataJpaTest` for each custom query method
   - New security rule → extend `SecurityFilterChainTest` with 401/403 assertions
8. Run all tests to verify correctness and security rules before committing code.
9. Check if all classes and methods have appropriate JavaDoc comments explaining their purpose and any important details and write them if missing.
