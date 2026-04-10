# api-rs — Security

`api-rs` acts as an OAuth2 Resource Server. Every request (except `POST /oauth2/token` and `/h2-console`) must carry a valid GitHub Bearer token. On each request the token is validated with GitHub, the authenticated user is registered or looked up in the database, and their roles are loaded from the `ROLES` table.

---

## Overview

| Class | Responsibility |
|---|---|
| `GitHubOpaqueTokenIntrospector` | Validates GitHub tokens via `GET https://api.github.com/user`; delegates to `UserService` for user registration and role loading |
| `ResourceServerConfig` | Declares the `SecurityFilterChain`: per-method authorization rules, CORS, session policy |

---

## Token Introspection

`GitHubOpaqueTokenIntrospector` implements `OpaqueTokenIntrospector`. On each request, Spring Security calls `introspect(token)`:

1. Calls `GET https://api.github.com/user` with `Authorization: Bearer <token>`.
2. A `200` response with a `login` field confirms the token is valid.
3. The `login` is passed to `UserService.findOrRegister`:
   - **First login** — inserts a new row in `USERS` and grants `ROLE_READ` in `ROLES`.
   - **Returning user** — loads all role rows from `ROLES` for that login.
4. Returns a `GitHubPrincipal` with the user's login and granted authorities.

**Token introspection flow**
```
Request: Authorization: Bearer gho_xxxxx
         │
         ▼
GitHubOpaqueTokenIntrospector.introspect(token)
         │
         ▼
GET https://api.github.com/user
Authorization: Bearer gho_xxxxx
         │
         ▼
{ "login": "octocat", "id": 1, "name": "The Octocat" }
         │
         ▼
UserService.findOrRegister("octocat", 1, "The Octocat", "https://...")
         │
         ├─ First login? YES → INSERT USERS + INSERT ROLES(ROLE_READ)
         │               NO  → SELECT role FROM ROLES WHERE login = 'octocat'
         │
         ▼
GitHubPrincipal { name: "octocat", authorities: [ROLE_READ, ROLE_EDIT, ROLE_DEL] }
```

---

## Authorization Rules

Declared in `ResourceServerConfig.securityFilterChain()`:

| HTTP Method | Path | Required Role |
|---|---|---|
| GET | `/categories`, `/messages` | `ROLE_READ` |
| POST | `/categories`, `/messages` | `ROLE_EDIT` |
| PUT | `/categories/**`, `/messages/**` | `ROLE_EDIT` |
| DELETE | `/categories/**`, `/messages/**` | `ROLE_DEL` |
| POST | `/oauth2/token` | none (permit-all) |
| any | `/h2-console` | none (permit-all) |
| any | anything else | authenticated |

---

## Role Management

Roles are stored in the `ROLES` table and loaded from the database on every introspection. There are no environment variables for roles.

| Role | Granted by | HTTP Methods |
|---|---|---|
| `ROLE_READ` | Automatically on first login | GET |
| `ROLE_EDIT` | Manual DB insert | POST, PUT |
| `ROLE_DEL` | Manual DB insert | DELETE |

**Example: grant ROLE_EDIT and ROLE_DEL via H2 console**
```sql
-- http://localhost:8080/h2-console
INSERT INTO ROLES (login, role) VALUES ('octocat', 'ROLE_EDIT');
INSERT INTO ROLES (login, role) VALUES ('octocat', 'ROLE_DEL');
```

**Example: revoke a role**
```sql
DELETE FROM ROLES WHERE login = 'octocat' AND role = 'ROLE_DEL';
```

**Example: list all users and their current roles**
```sql
SELECT u.login, r.role
FROM USERS u
LEFT JOIN ROLES r ON r.login = u.login
ORDER BY u.login, r.role;
```

> See [users.md](users.md) for full user and role management documentation.

---

## CORS Configuration

CORS is configured in `ResourceServerConfig.corsConfigurationSource()`. Allowed origins:

```java
config.setAllowedOrigins(List.of(
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:8000",
    "http://127.0.0.1:8000",
    "http://local.example.com:5500",
    "http://local.example.com:8080"
));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
config.setAllowCredentials(false);
```

To add a new UI origin (e.g., port 3000), add it to `setAllowedOrigins`:
```java
config.setAllowedOrigins(List.of(
    "http://localhost:5500",
    "http://localhost:3000"   // ← add here
));
```

---

## Adding a New Protected Endpoint

To add authorization to a new endpoint, declare a rule in `ResourceServerConfig.securityFilterChain()`:

**Example: add a new admin endpoint requiring ROLE_EDIT**
```java
.authorizeHttpRequests(auth -> auth
    // existing rules …
    .requestMatchers(HttpMethod.POST, "/admin/**").hasRole("EDIT")
    .requestMatchers(HttpMethod.DELETE, "/admin/**").hasRole("DEL")
)
```

---

## Error Responses

| Situation | Status |
|---|---|
| No `Authorization` header or invalid/expired token | `401 Unauthorized` |
| Valid token but missing required role | `403 Forbidden` |

**Example: test unauthorized access**
```bash
# No token
curl http://localhost:8080/categories
# → 401 Unauthorized

# Wrong role (token has ROLE_READ only)
curl -X DELETE http://localhost:8080/categories/INBOX \
  -H "Authorization: Bearer <read-only-token>"
# → 403 Forbidden
```
