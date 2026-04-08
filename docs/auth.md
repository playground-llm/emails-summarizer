# Authentication & Authorization

This document describes the end-to-end GitHub OAuth2 flow used by emails-summarizer, and the three-role authorization model enforced by `api-rs`.

---

## Overview

emails-summarizer uses the **GitHub OAuth2 Authorization Code flow**. The `ui` redirects users to GitHub for authentication. After login, GitHub issues a temporary code that the `ui` sends to `api-rs`, which exchanges it for a GitHub access token server-side (keeping the `client_secret` off the browser). The token is then stored in `sessionStorage` and sent as a Bearer token on every API request. `api-rs` validates each token by calling the GitHub user-info endpoint.

---

## OAuth2 Flow — Step by Step

```
Browser (ui)                    api-rs                      GitHub
     │                              │                           │
     │  1. User visits app          │                           │
     │  → no token in sessionStorage│                           │
     │                              │                           │
     │  2. Redirect to GitHub login ─────────────────────────► │
     │     (with client_id, state, redirect_uri)                │
     │                              │                           │
     │  3. User authorises ──────────────────────────────────── │
     │  ◄──────── GitHub redirects to ui with ?code=...         │
     │                              │                           │
     │  4. POST /oauth2/token ──────►                           │
     │     { code, redirectUri }    │                           │
     │                              │  5. POST access_token ──► │
     │                              │  ◄─────── { access_token }│
     │  ◄──── { accessToken } ──────│                           │
     │                              │                           │
     │  6. Store token in sessionStorage                        │
     │                              │                           │
     │  7. GET /categories ─────────►                           │
     │     Authorization: Bearer <token>                        │
     │                              │  8. GET /user ──────────► │
     │                              │  ◄─────── { login, id }   │
     │                              │  → assign roles            │
     │  ◄──── 200 [ categories ] ───│                           │
```

### Step-by-step walkthrough

1. The app checks `sessionStorage` for an existing token. If none, it checks for a `?code=` query parameter (GitHub callback).
2. If neither is present, the app redirects to GitHub:

```
https://github.com/login/oauth/authorize
  ?client_id=<GITHUB_CLIENT_ID>
  &redirect_uri=http://localhost:5500/index.html
  &scope=read:user
  &state=<random-uuid>
```

3. GitHub redirects back to the `redirect_uri` with `?code=<auth-code>&state=<state>`.
4. The app verifies `state` matches the value stored in `sessionStorage` (CSRF protection), then POSTs to `api-rs`:

**Example: exchange authorization code for access token**
```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/json" \
  -d '{"code":"<auth-code>","redirectUri":"http://localhost:5500/index.html"}'
```

Response:
```json
{ "accessToken": "gho_xxxxxxxxxxxxxxxxxxxx" }
```

5. The token is stored in `sessionStorage` and attached to every subsequent API call as `Authorization: Bearer <token>`.

---

## Authorization Roles

`api-rs` assigns roles at token introspection time. Roles are independent — a user must appear in each allow-list separately to hold multiple roles.

| Role | HTTP Methods | Environment Variable |
|---|---|---|
| `ROLE_READ` | GET | `READERS_GITHUB_LOGINS` |
| `ROLE_EDIT` | POST, PUT | `EDITORS_GITHUB_LOGINS` |
| `ROLE_DEL` | DELETE | `DELETERS_GITHUB_LOGINS` |

Each variable is a comma-separated list of GitHub login names (case-insensitive).

**Example: grant a user all three roles**
```bash
export READERS_GITHUB_LOGINS=octocat,alice
export EDITORS_GITHUB_LOGINS=octocat
export DELETERS_GITHUB_LOGINS=octocat
```

In this example, `octocat` has all three roles; `alice` can only read.

---

## Error Responses

| Situation | HTTP Status |
|---|---|
| No `Authorization` header or invalid token | `401 Unauthorized` |
| Valid token but missing required role | `403 Forbidden` |

**Example: calling a protected endpoint without a token**
```bash
curl http://localhost:8080/categories
# → 401 Unauthorized
```

**Example: calling DELETE without ROLE_DEL**
```bash
curl -X DELETE http://localhost:8080/categories/INBOX \
  -H "Authorization: Bearer <read-only-token>"
# → 403 Forbidden
```

---

## Security Constraints

- `GITHUB_CLIENT_SECRET` must never appear in `ui` code or be committed to the repository.
- CSRF state (random UUID) is stored in `sessionStorage` and verified on callback before the code is exchanged.
- CORS on `api-rs` allows only known `ui` origins; `allowCredentials` is disabled (Bearer token replaces cookies).
- The UI must be served over HTTP, not `file://` — OAuth2 redirect URIs require a real origin.
