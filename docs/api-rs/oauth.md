# api-rs — OAuth2 Token Exchange (`OAuthController`)

`OAuthController` proxies the GitHub OAuth2 Authorization Code → access token exchange. It exposes `POST /oauth2/token` as a permit-all endpoint so the `ui` can complete the OAuth2 flow without exposing the `client_secret` to the browser.

---

## Overview

GitHub's token endpoint (`https://github.com/login/oauth/access_token`) does not emit CORS headers, so a browser cannot call it directly. The flow is:

1. `ui` receives `?code=<auth-code>` from GitHub's redirect.
2. `ui` calls `POST /oauth2/token` on `api-rs` with the code and redirect URI.
3. `api-rs` performs a server-to-server call to GitHub, exchanging the code for an access token.
4. `api-rs` returns only the `accessToken` to the browser — the `client_secret` never leaves the server.

---

## Endpoint

### `POST /oauth2/token`

Exchanges a GitHub authorization code for an access token. No authentication required.

**Request body**
```json
{
  "code": "<authorization-code-from-github-callback>",
  "redirectUri": "http://localhost:5500/index.html"
}
```

**Response body**
```json
{
  "accessToken": "gho_xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

| Field | Description |
|---|---|
| `code` | The `code` query parameter from the GitHub OAuth2 callback URL |
| `redirectUri` | Must exactly match the `redirect_uri` used in the initial authorization request and registered in the GitHub OAuth App |

---

## Examples

**Example: exchange a code from a browser callback**
```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/json" \
  -d '{
    "code": "6b2d9f1e3c4a5b6c",
    "redirectUri": "http://localhost:5500/index.html"
  }'
```

Response:
```json
{ "accessToken": "gho_ABCDEFGHIJ1234567890abcdefghij" }
```

**Example: error when code is invalid or already used**
```bash
# GitHub returns an error payload — api-rs throws 500
{ "error": "bad_verification_code", "error_description": "The code passed is incorrect or expired." }
```

---

## Configuration

The controller reads GitHub credentials from Spring properties, which are backed by environment variables:

| Property | Environment Variable | Description |
|---|---|---|
| `spring.security.oauth2.resourceserver.opaquetoken.client-id` | `GITHUB_CLIENT_ID` | GitHub OAuth App client ID |
| `spring.security.oauth2.resourceserver.opaquetoken.client-secret` | `GITHUB_CLIENT_SECRET` | GitHub OAuth App client secret |

**Example: set credentials before starting api-rs**
```bash
export GITHUB_CLIENT_ID=Ov23lidSXJDYQSwOXa5m
export GITHUB_CLIENT_SECRET=abc123secret
./gradlew :api-rs:bootRun
```

---

## Security Notes

- `GITHUB_CLIENT_SECRET` is read from environment variables only — it is never hardcoded or committed.
- The endpoint is `permitAll()` — no Bearer token is required, because the caller doesn't have one yet.
- The `redirect_uri` sent to GitHub must exactly match the value registered in the GitHub OAuth App settings. A mismatch causes GitHub to reject the exchange.

---

## Registering the GitHub OAuth App

1. Go to [https://github.com/settings/developers](https://github.com/settings/developers).
2. Click **New OAuth App**.
3. Set **Authorization callback URL** to the `ui` origin + path (e.g., `http://localhost:5500/index.html`).
4. Copy the **Client ID** and generate a **Client Secret**.
5. Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` environment variables before starting `api-rs`.
