## 1. Gradle Build Setup

- [x] 1.1 Apply the `org.springframework.boot` Gradle plugin (latest 4.x) and `io.spring.dependency-management` plugin in `api-rs/build.gradle`
- [x] 1.2 Add dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`
- [x] 1.3 Add runtime dependency: `h2` (in-memory database)
- [x] 1.4 Verify `./gradlew :api-rs:build` resolves all dependencies without errors

## 2. GitHub OAuth App Registration

- [ ] 2.1 Register a new GitHub OAuth App at https://github.com/settings/developers named "emails-summarizer"
- [ ] 2.2 Set the "Authorization callback URL" to the `ui` origin (e.g. `http://localhost:5500/index.html`) — this is used by the `ui`, not api-rs directly
- [ ] 2.3 Copy the generated `Client ID` and `Client Secret` — store them as environment variables `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` locally

## 3. Application Bootstrap

- [x] 3.1 Create the main application class `ApiRsApplication.java` under `src/main/java/com/emailssummarizer/apirs/` with `@SpringBootApplication` and `main` method
- [x] 3.2 Create `src/main/resources/application.yml` with H2 datasource config (`jdbc:h2:mem:emailsdb`), JPA settings (`ddl-auto: none`), H2 console enabled for dev, and GitHub OAuth client-id / client-secret placeholders (e.g. `${GITHUB_CLIENT_ID}`)

## 4. Database Schema and Sample Data

- [x] 4.1 Create `src/main/resources/schema.sql` defining `CATEGORY` table (`id` BIGINT PK, `name` VARCHAR, `code` VARCHAR UNIQUE, `description` VARCHAR)
- [x] 4.2 Add `MESSAGE` table to `schema.sql` (`id` UUID PK DEFAULT `RANDOM_UUID()`, `title` VARCHAR, `body` CLOB, `category_code` VARCHAR)
- [x] 4.3 Create `src/main/resources/data.sql` with at least 3 sample categories (e.g. INBOX, WORK, PERSONAL)
- [x] 4.4 Add at least 6 sample messages to `data.sql` spread across the categories

## 5. Category Feature

- [x] 5.1 Create `Category.java` JPA entity mapped to the `CATEGORY` table with fields: `id`, `name`, `code`, `description`
- [x] 5.2 Create `CategoryRepository.java` extending `JpaRepository<Category, Long>`
- [x] 5.3 Create `CategoryController.java` with `@RestController` and `GET /categories` endpoint returning `List<Category>`

## 6. Message Feature

- [x] 6.1 Create `Message.java` JPA entity mapped to the `MESSAGE` table with fields: `id` (UUID), `title`, `body`, `categoryCode`
- [x] 6.2 Create `MessageRepository.java` extending `JpaRepository<Message, UUID>` with derived query method `findByCategoryCode(String categoryCode)`
- [x] 6.3 Create `MessageController.java` with `@RestController` and `GET /messages` endpoint accepting `@RequestParam String categoryCode` and returning `List<Message>`
- [x] 6.4 Ensure missing `categoryCode` parameter returns HTTP 400 (Spring MVC default for required `@RequestParam`)

## 7. OAuth2 Security Configuration

- [x] 7.1 Create `GitHubOpaqueTokenIntrospector.java` implementing `OpaqueTokenIntrospector`; call `GET https://api.github.com/user` with `Authorization: Bearer <token>`; on HTTP 200 return an `OAuth2AuthenticatedPrincipal` with the GitHub `login` as the principal name; on non-200 throw `OAuth2IntrospectionException`
- [x] 7.2 Create `ResourceServerConfig.java` annotated with `@Configuration` and `@EnableWebSecurity`; configure `HttpSecurity` to require authentication on `/categories` and `/messages`, permit the H2 console path, and register the custom `GitHubOpaqueTokenIntrospector` bean as the `OpaqueTokenIntrospector`
- [x] 7.3 Configure CORS in `ResourceServerConfig.java` to allow the `ui` origin (e.g. `http://localhost:5500`) so the Vue page can call the API from the browser

## 8. Verification

- [ ] 8.1 Run `./gradlew :api-rs:bootRun` (with `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` set) and confirm the application starts on port 8080 without errors
- [ ] 8.2 Obtain a GitHub access token via the Authorization Code flow (through the `ui` or manually via `gh auth token` CLI)
- [ ] 8.3 Call `GET /categories` with `Authorization: Bearer <github_token>` and confirm HTTP 200 with a JSON array of 3+ categories
- [ ] 8.4 Call `GET /messages?categoryCode=INBOX` with the Bearer token and confirm HTTP 200 with messages containing `id` (UUID), `title`, and `body`
- [ ] 8.5 Call `GET /categories` without a token and confirm HTTP 401
- [ ] 8.6 Call `GET /messages` without `categoryCode` param and confirm HTTP 400
- [ ] 8.7 Open H2 console at `http://localhost:8080/h2-console` and verify data is loaded in both tables
- [ ] 8.8 Use an invalid/revoked GitHub token and confirm HTTP 401 is returned
