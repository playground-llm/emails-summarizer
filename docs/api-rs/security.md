# api-rs — Security

`api-rs` acts as an OAuth2 Resource Server. Every request (except `/oauth2/token` and `/h2-console`) must carry a valid GitHub Bearer token. Roles are assigned at introspection time based on configurable allow-lists.

---

## Overview

| Class | Responsibility |
|---|---|
| `GitHubOpaqueTokenIntrospector` | Validates GitHub tokens via `GET https://api.github.com/user`; assigns `ROLE_READ`, `ROLE_EDIT`, `ROLE_DEL` |
| `ResourceServerConfig` | Declares the `SecurityFilterChain`: per-method authorization rules, CORS, session policy |

---

## Token Introspection

`GitHubOpaqueTokenIntrospector` implements `OpaqueTokenIntrospector`. On each request, Spring Security calls `introspect(token)`:

1. Calls `GET https://api.github.com/user` with `Authorization: Bearer <token>`.
2. A `200` response with a `login` field confirms the token is valid.
3. The `login` is compared (case-insensitively) against three allow-lists.
4. Matching allow-lists add the corresponding role authority.

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
Check allow-lists:
  readers.contains("octocat")  → add ROLE_READ
  editors.contains("octocat")  → add ROLE_EDIT
  deleters.contains("octocat") → add ROLE_DEL
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
| any | `/h2-console/**` | none (permit-all) |
| any | anything else | authenticated |

---

## Role Configuration

Roles are controlled by three environment variables. Each is a comma-separated list of GitHub login names (case-insensitive). Roles are **independent** — a user must appear in each list to hold multiple roles.

| Environment Variable | Role | Methods |
|---|---|---|
| `READERS_GITHUB_LOGINS` | `ROLE_READ` | GET |
| `EDITORS_GITHUB_LOGINS` | `ROLE_EDIT` | POST, PUT |
| `DELETERS_GITHUB_LOGINS` | `ROLE_DEL` | DELETE |

**Example: read-only user**
```bash
export READERS_GITHUB_LOGINS=alice,bob
export EDITORS_GITHUB_LOGINS=alice
export DELETERS_GITHUB_LOGINS=alice
# bob can only GET; alice can GET, POST, PUT, DELETE
```

**Example: warn when an allow-list is empty**

On startup, if any allow-list is empty, the introspector logs a warning:
```
WARN: READERS_GITHUB_LOGINS is empty — GET /categories and GET /messages will be denied for all users
```

---

## CORS Configuration

CORS is configured in `ResourceServerConfig.corsConfigurationSource()`. Allowed origins:

```java
config.setAllowedOrigins(List.of(
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:8000",
    "http://127.0.0.1:8000",
    "http://local.example.com:5500"
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

**Example: add a new admin endpoint requiring all three roles**
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
