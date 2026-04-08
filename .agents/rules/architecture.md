# Architecture

General architecture rules for the emails-summarizer monorepo.
Apply these rules to every code change, regardless of subproject.

---

## System overview

```
Browser (ui)
  │  OAuth2 Authorization Code flow (GitHub)
  │  Bearer token in sessionStorage
  │
  ├─── GET /categories ──────────────────────────────┐
  ├─── GET /messages?categoryCode=<code> ────────────┤
  ├─── POST/PUT /categories, /messages ──────────────┤  api-rs (Spring Boot 4, :8080)
  └─── DELETE  /categories, /messages ───────────────┤
                                                      │  OAuth2 Resource Server
       POST /oauth2/token (permit-all proxy) ─────────┘  GitHub user-info introspection
```

Two independent deployable units:

| Unit | Tech | Port |
|---|---|---|
| `ui` | Vue 3 (CDN, no bundler) | served separately (5500 / 3000) |
| `api-rs` | Java 21, Spring Boot 4 | 8080 |

They share no code — the only contract between them is the REST API described in `openspec/specs/`.

---

## `api-rs` internal layers

```
HTTP request
    │
    ▼
Controller          ← HTTP mapping only (status codes, request/response bodies)
    │
    ▼
Service             ← ALL business logic, validation, and error conditions
    │
    ▼
Repository          ← DB access (Spring Data JPA); called only from Services
    │
    ▼
H2 (in-memory)
```

**Rules:**
- Controllers MUST NOT import or call Repositories directly.
- Services MUST NOT import or call another feature's Controller.
- Cross-feature data access (e.g. MessageService checking a category) goes through the other feature's Service or Repository.
- Request bodies MUST use separate DTO records (`<Name>Request`), never JPA entities.

---

## `ui` internal layers

```
index.js  (Vue 3 app, event wiring, lifecycle)
    │
    ├── composables/useAuth.js   ← OAuth2 flow, token helpers
    └── services/api.js          ← Fetch wrapper, Bearer token injection
```

**Rules:**
- `index.js` orchestrates components and calls composables/services; it does not contain raw `fetch` calls.
- `useAuth.js` owns all OAuth2 state (token, CSRF state, redirect URL construction, code exchange).
- `api.js` owns the HTTP transport concern (base URL, Authorization header, 401 handling); it has no knowledge of OAuth2 flow.

---

## Authentication & authorisation

GitHub OAuth2 Authorization Code flow:

1. `ui` redirects user to `https://github.com/login/oauth/authorize`.
2. GitHub redirects back to `ui` with `?code=`.
3. `ui` sends the code to `POST /oauth2/token` on `api-rs` (avoids CORS on GitHub's token endpoint).
4. `api-rs` exchanges the code for a GitHub access token server-side and returns it.
5. `ui` stores the token in `sessionStorage` and sends it as `Authorization: Bearer <token>` on every API call.
6. `api-rs` validates each token by calling `GET https://api.github.com/user`; the GitHub login becomes the principal name.

**Roles** (assigned at introspection time, independent of each other):

| Role | HTTP methods | Env var |
|---|---|---|
| `ROLE_READ` | GET | `READERS_GITHUB_LOGINS` |
| `ROLE_EDIT` | POST, PUT | `EDITORS_GITHUB_LOGINS` |
| `ROLE_DEL` | DELETE | `DELETERS_GITHUB_LOGINS` |

Each env var is a comma-separated list of GitHub login names (case-insensitive).
A user must appear in each list separately to hold multiple roles — roles are NOT hierarchical.

---

## Data model

```
CATEGORY
  id           BIGINT PK AUTO_INCREMENT
  name         VARCHAR NOT NULL
  code         VARCHAR UNIQUE NOT NULL   ← business key, used as URL path parameter
  description  VARCHAR

MESSAGE
  id            UUID PK DEFAULT RANDOM_UUID()
  title         VARCHAR NOT NULL
  body          CLOB
  category_code VARCHAR NOT NULL FK → CATEGORY.code
```

**Rules:**
- `category.code` is immutable after creation — it is a URL identifier and a FK target.
- Deleting a category that still has messages returns `409 Conflict`; the caller must delete messages first.

---

## HTTP status codes

| Situation | Status |
|---|---|
| Successful GET / PUT | 200 |
| Successful POST (created) | 201 |
| Successful DELETE | 204 |
| Resource not found | 404 |
| Conflict (duplicate code, FK violation, category has messages) | 409 |
| Missing / invalid token | 401 |
| Authenticated but missing role | 403 |

---

## Security constraints

- `GITHUB_CLIENT_SECRET` MUST NEVER appear in `ui` code or be committed to the repository.
- The `ui` MUST be served over HTTP (not `file://`) — OAuth2 redirect URIs require a real origin.
- CSRF state (random UUID) MUST be stored in `sessionStorage` and verified on callback before exchanging the code.
- CORS on `api-rs` allows only known `ui` origins; credentials (`allowCredentials`) are disabled (Bearer token is used instead of cookies).
