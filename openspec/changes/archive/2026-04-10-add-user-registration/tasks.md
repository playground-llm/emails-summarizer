## 1. Data model — schema.sql

- [x] 1.1 Add `USERS` table DDL to `schema.sql`: `login VARCHAR(100) NOT NULL PRIMARY KEY`, `github_id BIGINT`, `name VARCHAR(255)`, `avatar_url VARCHAR(500)`
- [x] 1.2 Add `ROLES` table DDL to `schema.sql`: surrogate `id BIGINT AUTO_INCREMENT PK`, `login VARCHAR(100) NOT NULL FK → USERS(login)`, `role VARCHAR(50) NOT NULL`, `UNIQUE(login, role)`

## 2. JPA entities

- [x] 2.1 Create `AppUser.java` entity in `user/` package mapping to `USERS`; use `login` as `@Id`
- [x] 2.2 Create `UserRole.java` entity in `user/` package mapping to `ROLES`; fields: `id`, `login`, `role`
- [x] 2.3 Create `AppUserRepository` extending `JpaRepository<AppUser, String>`
- [x] 2.4 Create `UserRoleRepository` extending `JpaRepository<UserRole, Long>`; add `findAllByLogin(String login)` derived query

## 3. Service — find-or-create logic

- [x] 3.1 Create `UserService` in `user/` package annotated `@Service`
- [x] 3.2 Implement `findOrRegister(String login, Long githubId, String name, String avatarUrl): List<String>` annotated `@Transactional`
  - If `login` not found in `USERS`: insert `AppUser` + insert `UserRole(login, "ROLE_READ")`; return `["ROLE_READ"]`
  - If found: load and return all `UserRole.getRole()` values for that login
- [x] 3.3 Log at INFO level on first registration; log at DEBUG on subsequent logins

## 4. Wire into introspector

- [x] 4.1 Inject `UserService` into `GitHubOpaqueTokenIntrospector` via constructor
- [x] 4.2 Call `userService.findOrRegister(login, githubId, name, avatarUrl)` after resolving the GitHub principal
- [x] 4.3 Convert returned role strings to `SimpleGrantedAuthority` instances and set on the principal

## 5. Verification

- [x] 5.1 Start `api-rs` with a valid GitHub token; confirm `GET /categories` returns 200 on first request
- [x] 5.2 Open H2 console (`http://localhost:8080/h2-console`); confirm a row exists in `USERS` and `ROLES` for your login with `role = 'ROLE_READ'`
- [x] 5.3 Manually insert `ROLE_EDIT` for your login; confirm `POST /categories` returns 201 (requires service restart to clear token cache)
- [x] 5.4 Restart the application; confirm your GitHub token re-registers you with `ROLE_READ` automatically
