# api-rs — Overview

`api-rs` is the Spring Boot 4 REST API backend for emails-summarizer. It provides CRUD endpoints for categories and messages, proxies GitHub OAuth2 token exchange, and enforces three-role authorization using GitHub opaque token introspection.

---

## Contents

| Document | Description |
|---|---|
| [categories.md](categories.md) | Category CRUD endpoints and service logic |
| [messages.md](messages.md) | Message CRUD endpoints and service logic |
| [security.md](security.md) | Token introspection and role-based authorization |
| [oauth.md](oauth.md) | OAuth2 token exchange proxy (`POST /oauth2/token`) |
| [users.md](users.md) | User registration and DB-backed role management |

---

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 4.0.0-M1
- **ORM**: Spring Data JPA (Hibernate)
- **Database**: H2 in-memory
- **Security**: Spring Security, OAuth2 Resource Server, opaque token introspection
- **Build**: Gradle 8.13 (always use `./gradlew`)

---

## Project Structure

```
api-rs/src/main/java/com/emailssummarizer/apirs/
├── ApiRsApplication.java         ← Spring Boot entry point
├── category/                     ← Category feature slice
│   ├── Category.java             ← JPA entity
│   ├── CategoryController.java   ← REST controller (/categories)
│   ├── CategoryRepository.java   ← Spring Data JPA repository
│   ├── CategoryRequest.java      ← Request DTO (record)
│   └── CategoryService.java      ← Business logic
├── message/                      ← Message feature slice
│   ├── Message.java
│   ├── MessageController.java
│   ├── MessageRepository.java
│   ├── MessageRequest.java
│   └── MessageService.java
├── oauth/
│   └── OAuthController.java      ← POST /oauth2/token proxy
├── security/
│   ├── GitHubOpaqueTokenIntrospector.java  ← Token validation + role assignment
│   └── ResourceServerConfig.java           ← SecurityFilterChain, CORS, auth rules
└── user/                         ← User registration and role management
    ├── AppUser.java              ← JPA entity (USERS table)
    ├── AppUserRepository.java    ← Spring Data JPA repository
    ├── UserRole.java             ← JPA entity (ROLES table)
    ├── UserRoleRepository.java   ← Spring Data JPA repository
    └── UserService.java          ← find-or-register logic, role retrieval
```

---

## Running Locally

**Step 1 — Set required environment variables**
```bash
export GITHUB_CLIENT_ID=<your-client-id>
export GITHUB_CLIENT_SECRET=<your-client-secret>
```

**Step 2 — Start the server**
```bash
./gradlew :api-rs:bootRun
# API available at http://localhost:8080
```

**Step 3 — Verify it is running**
```bash
curl http://localhost:8080/categories \
  -H "Authorization: Bearer <your-github-token>"
# → 200 [ { "id": 1, "name": "Inbox", "code": "INBOX", ... }, ... ]
```

---

## API Endpoints Summary

| Method | Path | Role Required | Description |
|---|---|---|---|
| GET | `/categories` | ROLE_READ | List all categories |
| POST | `/categories` | ROLE_EDIT | Create a category |
| PUT | `/categories/{code}` | ROLE_EDIT | Update a category |
| DELETE | `/categories/{code}` | ROLE_DEL | Delete a category |
| GET | `/messages?categoryCode=` | ROLE_READ | List messages in a category |
| POST | `/messages` | ROLE_EDIT | Create a message |
| PUT | `/messages/{id}` | ROLE_EDIT | Update a message |
| DELETE | `/messages/{id}` | ROLE_DEL | Delete a message |
| POST | `/oauth2/token` | none | Exchange GitHub auth code for token |

---

## Configuration

`api-rs/src/main/resources/application.yml` — key properties:

| Property | Environment Variable | Description |
|---|---|---|
| `spring.security.oauth2.resourceserver.opaquetoken.client-id` | `GITHUB_CLIENT_ID` | GitHub OAuth App client ID |
| `spring.security.oauth2.resourceserver.opaquetoken.client-secret` | `GITHUB_CLIENT_SECRET` | GitHub OAuth App client secret |

> Roles are no longer configured through environment variables. They are stored in the `ROLES` table and assigned automatically on first login (`ROLE_READ`) or via direct DB inserts. See [users.md](users.md).
