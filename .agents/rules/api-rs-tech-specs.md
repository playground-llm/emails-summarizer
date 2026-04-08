---
paths:
  - "api-rs/**"
---

# api-rs Technical Specifications

Technical specifications for the `api-rs` subproject.
Apply these rules to every backend code change.

---

## Tech stack

| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Web | Spring MVC (`spring-boot-starter-web`) |
| Persistence | Spring Data JPA (`spring-boot-starter-data-jpa`) |
| Security | Spring Security + OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`) |
| Database | H2 in-memory (`jdbc:h2:mem:emailsdb`) |
| Build | Gradle 8.13 via wrapper (`./gradlew`) |
| Testing | JUnit 5, Mockito, `spring-boot-starter-test`, `spring-security-test` |

---

## Project layout

```
api-rs/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/com/emailssummarizer/apirs/
    │   │   ├── ApiRsApplication.java             ← @SpringBootApplication entry point
    │   │   ├── category/
    │   │   │   ├── Category.java                 ← JPA entity
    │   │   │   ├── CategoryController.java        ← @RestController /categories; delegates to CategoryService
    │   │   │   ├── CategoryRepository.java        ← JpaRepository; called only from CategoryService
    │   │   │   ├── CategoryRequest.java           ← Request DTO (record)
    │   │   │   └── CategoryService.java           ← @Service; all business logic
    │   │   ├── message/
    │   │   │   ├── Message.java
    │   │   │   ├── MessageController.java         ← @RestController /messages; delegates to MessageService
    │   │   │   ├── MessageRepository.java         ← JpaRepository; called only from MessageService
    │   │   │   ├── MessageRequest.java
    │   │   │   └── MessageService.java
    │   │   ├── oauth/
    │   │   │   └── OAuthController.java           ← POST /oauth2/token GitHub proxy (permit-all)
    │   │   └── security/
    │   │       ├── GitHubOpaqueTokenIntrospector.java  ← Token validation + role assignment
    │   │       └── ResourceServerConfig.java           ← SecurityFilterChain, CORS
    │   └── resources/
    │       ├── application.yml                   ← Spring Boot config
    │       ├── schema.sql                        ← DDL executed at startup
    │       └── data.sql                          ← Seed data executed at startup
    └── test/
        ├── java/com/emailssummarizer/apirs/
        │   ├── ApiRsApplicationTests.java
        │   ├── category/
        │   │   ├── CategoryControllerTest.java   ← @WebMvcTest + @MockitoBean(CategoryService)
        │   │   ├── CategoryRepositoryTest.java   ← @DataJpaTest
        │   │   └── CategoryServiceTest.java      ← JUnit 5 + Mockito
        │   ├── message/
        │   │   ├── MessageControllerTest.java
        │   │   ├── MessageRepositoryTest.java
        │   │   └── MessageServiceTest.java
        │   └── security/
        │       └── SecurityFilterChainTest.java  ← @SpringBootTest + MockMvc; 401/403 assertions
        └── resources/
            └── application.yml                   ← Test overrides (fixed role allow-lists)
```

---

## Layering rules

```
Controller  →  Service  →  Repository
```

- **Controllers** handle HTTP only: parse request, call service, map result to `ResponseEntity`. They MUST NOT inject or call any `Repository` directly.
- **Services** own all business logic, validation, and error conditions (duplicate checks, FK safety, not-found exceptions). They are the only classes allowed to inject Repositories.
- **Repositories** are called only from Services — never from Controllers or other Services' controllers.
- **Cross-feature access** (e.g. `MessageService` checking a category) goes through the other feature's `Service` or `Repository`, never its `Controller`.
- **Request DTOs** (`<Name>Request` records) are used for all request bodies — JPA entities must never be exposed as request bodies.

---

## Feature slice structure

Every domain concept gets its own package with exactly these five files:

| File | Role |
|---|---|
| `<Name>.java` | JPA `@Entity` mapping to a DB table |
| `<Name>Controller.java` | `@RestController`; delegates to service |
| `<Name>Service.java` | `@Service`; all business logic |
| `<Name>Repository.java` | `JpaRepository`; DB queries |
| `<Name>Request.java` | `record` DTO for request body |

---

## REST API

| Method | Path | Role required | Success | Error cases |
|---|---|---|---|---|
| GET | `/categories` | `ROLE_READ` | 200 | 401, 403 |
| POST | `/categories` | `ROLE_EDIT` | 201 | 401, 403, 409 (duplicate code) |
| PUT | `/categories/{code}` | `ROLE_EDIT` | 200 | 401, 403, 404 |
| DELETE | `/categories/{code}` | `ROLE_DEL` | 204 | 401, 403, 404, 409 (has messages) |
| GET | `/messages?categoryCode=` | `ROLE_READ` | 200 | 400 (missing param), 401, 403 |
| POST | `/messages` | `ROLE_EDIT` | 201 | 401, 403, 409 (unknown categoryCode) |
| PUT | `/messages/{id}` | `ROLE_EDIT` | 200 | 401, 403, 404, 409 (unknown categoryCode) |
| DELETE | `/messages/{id}` | `ROLE_DEL` | 204 | 401, 403, 404 |
| POST | `/oauth2/token` | none (permit-all) | 200 `{accessToken}` | 400 |

---

## Security

**Token validation** (`GitHubOpaqueTokenIntrospector`):
- Calls `GET https://api.github.com/user` with the Bearer token
- 200 response → valid; extracts `login` as the principal name
- Non-200 → throws `BadOpaqueTokenException` → client receives 401

**Role assignment** (at introspection time, three independent allow-lists):

| Role | Grant condition | `application.yml` property | Env var |
|---|---|---|---|
| `ROLE_READ` | login in readers list | `app.readers` | `READERS_GITHUB_LOGINS` |
| `ROLE_EDIT` | login in editors list | `app.editors` | `EDITORS_GITHUB_LOGINS` |
| `ROLE_DEL` | login in deleters list | `app.deleters` | `DELETERS_GITHUB_LOGINS` |

- Each env var is a comma-separated list of GitHub logins (case-insensitive comparison)
- Roles are **independent** — a user must appear in each list separately to hold multiple roles
- An empty list means no user holds that role (all requests for that method return 403)

**`ResourceServerConfig` authorization rules** (in declaration order):
1. `/h2-console/**` → `permitAll`
2. `/oauth2/token` → `permitAll`
3. `DELETE /categories/**, /messages/**` → `hasRole("DEL")`
4. `POST /categories, /messages` → `hasRole("EDIT")`
5. `PUT /categories/**, /messages/**` → `hasRole("EDIT")`
6. `GET /categories, /messages` → `hasRole("READ")`
7. `anyRequest()` → `authenticated`

**CORS**: allows configured `ui` origins; `allowCredentials` is disabled; allowed methods: GET, POST, PUT, DELETE, OPTIONS.

---

## Database

- H2 in-memory; JDBC URL: `jdbc:h2:mem:emailsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Schema initialized from `schema.sql`, seed data from `data.sql` (`spring.sql.init.mode: always`)
- H2 console available at `http://localhost:8080/h2-console` (dev only)
- `ddl-auto: none` — schema managed exclusively by `schema.sql`

**Tables:**

```sql
CATEGORY (id BIGINT PK AUTO_INCREMENT, name VARCHAR, code VARCHAR UNIQUE, description VARCHAR)
MESSAGE  (id UUID PK DEFAULT RANDOM_UUID(), title VARCHAR, body CLOB,
          category_code VARCHAR FK → CATEGORY.code)
```

- `category.code` is immutable after creation — it is a URL path parameter and a FK target
- Deleting a category that has messages returns `409 Conflict`; caller must delete messages first

---

## Testing

`src/test/resources/application.yml` must override the role allow-lists with deterministic values:

```yaml
app:
  readers: reader-user
  editors: editor-user
  deleters: deleter-user
```

| Test class | Annotation | Scope |
|---|---|---|
| `<Name>ControllerTest` | `@WebMvcTest(<Name>Controller.class)` | HTTP mapping, status codes; service mocked with `@MockitoBean` |
| `<Name>ServiceTest` | plain JUnit 5 + `@Mock` / `@InjectMocks` | Business logic, validation, error conditions |
| `<Name>RepositoryTest` | `@DataJpaTest` | Custom query methods against H2 |
| `SecurityFilterChainTest` | `@SpringBootTest` + `MockMvc` | 401 (no/invalid token) and 403 (wrong role) per endpoint |
| `ApiRsApplicationTests` | `@SpringBootTest` | Context loads without errors |

Run tests: `./gradlew :api-rs:test`
