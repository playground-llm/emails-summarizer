# UI — Authentication (`useAuth.js`)

`composables/useAuth.js` owns all OAuth2 state in the `ui`. It provides token storage helpers, builds the GitHub authorization URL with CSRF protection, and exchanges authorization codes for access tokens via `api-rs`.

---

## Overview

The composable implements the browser side of the GitHub OAuth2 Authorization Code flow. It keeps the `client_secret` off the browser by routing the code exchange through `api-rs` (`POST /oauth2/token`).

---

## Structure

| Export | Type | Description |
|---|---|---|
| `token` | object | sessionStorage helpers (`get`, `save`, `clear`) |
| `REDIRECT_URI` | string | Derived from `window.location.origin + pathname` |
| `buildGitHubAuthUrl()` | function | Builds GitHub authorize URL with CSRF state |
| `exchangeCode(code)` | async function | POSTs code to api-rs and returns access token |

---

## Token Storage

The access token is stored in `sessionStorage` under the key `access_token`. It is cleared on sign-out or on 401 responses.

**Example: manually inspect or clear the token in browser DevTools**
```javascript
// Read token
sessionStorage.getItem('access_token');

// Clear token (forces re-login on next page load)
sessionStorage.removeItem('access_token');
```

---

## Building the Authorization URL

`buildGitHubAuthUrl()` creates a CSRF-safe GitHub authorization URL. It generates a random UUID as `state`, stores it in `sessionStorage`, and embeds it in the URL.

**Example: what the generated URL looks like**
```
https://github.com/login/oauth/authorize
  ?client_id=Ov23lidSXJDYQSwOXa5m
  &redirect_uri=http%3A%2F%2Flocalhost%3A5500%2Findex.html
  &scope=read%3Auser
  &state=3f2a1b4c-8e7d-4f9c-a2b1-1234567890ab
```

**Example: generate a URL in JavaScript**
```javascript
import { buildGitHubAuthUrl } from './composables/useAuth.js';
const url = buildGitHubAuthUrl();
window.location.href = url; // redirect user to GitHub login
```

---

## Exchanging the Authorization Code

After GitHub redirects back with `?code=<auth-code>&state=<state>`, the app verifies the state and calls `exchangeCode(code)`.

**Example: code exchange via fetch (what exchangeCode does internally)**
```javascript
const res = await fetch('http://localhost:8080/oauth2/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    code: '<auth-code-from-query-param>',
    redirectUri: 'http://localhost:5500/index.html'
  })
});
const { accessToken } = await res.json();
sessionStorage.setItem('access_token', accessToken);
```

**Example: using exchangeCode in the app**
```javascript
import { exchangeCode, token } from './composables/useAuth.js';

const params = new URLSearchParams(window.location.search);
const code = params.get('code');
if (code) {
  const accessToken = await exchangeCode(code);
  token.save(accessToken);
}
```

---

## CSRF Protection

A random UUID `state` is generated with each call to `buildGitHubAuthUrl()` and stored in `sessionStorage` under `oauth_state`. On callback, the app compares the `state` query parameter with the stored value. A mismatch aborts the flow and shows an error.

```javascript
// State is verified in index.js created() hook:
const savedState = sessionStorage.getItem('oauth_state');
if (state && savedState && state !== savedState) {
  // CSRF detected — clear token and show error
  token.clear();
  this.authError = 'OAuth state mismatch — possible CSRF.';
}
sessionStorage.removeItem('oauth_state'); // clean up after verification
```

---

## Configuration

| Variable | Default | Description |
|---|---|---|
| `window.__GITHUB_CLIENT_ID__` | `'Ov23lidSXJDYQSwOXa5m'` | GitHub OAuth App client ID |
| `REDIRECT_URI` | `window.location.origin + pathname` | Must match GitHub OAuth App callback URL |

Override the client ID before the module loads:
```html
<script>
  window.__GITHUB_CLIENT_ID__ = 'your-actual-client-id';
</script>
```
