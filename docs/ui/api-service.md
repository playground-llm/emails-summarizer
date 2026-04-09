# UI — API Service (`services/api.js`)

`services/api.js` is the HTTP transport layer for the `ui`. It provides the backend base URL and a fetch wrapper that attaches the Bearer token and handles `401 Unauthorized` responses.

---

## Overview

The service owns one concern: sending authenticated HTTP requests to `api-rs`. It has no knowledge of the OAuth2 flow — token management is handled by `useAuth.js`.

---

## Exports

| Export | Type | Description |
|---|---|---|
| `API_BASE` | string | Base URL of the `api-rs` backend |
| `apiFetch(path, getToken)` | async function | Authenticated fetch wrapper |

---

## `API_BASE`

The default base URL is `http://local.example.com:8080`. Override it at runtime via `window.__API_BASE__`.

**Example: override API_BASE in index.html**
```html
<script>
  window.__API_BASE__ = 'http://localhost:8080';
</script>
<script type="module" src="index.js"></script>
```

**Example: what `API_BASE` resolves to**
```javascript
import { API_BASE } from './services/api.js';
console.log(API_BASE); // 'http://localhost:8080' (if overridden)
```

---

## `apiFetch(path, getToken)`

Sends a `GET` request to `API_BASE + path` with `Authorization: Bearer <token>`. Returns the parsed JSON body on success. Throws an error with `status: 401` on unauthorized responses so the caller can redirect to login.

**Signature**
```javascript
apiFetch(path: string, getToken: () => string): Promise<any>
```

**Example: fetch all categories**
```javascript
import { apiFetch } from './services/api.js';
import { token } from './composables/useAuth.js';

const categories = await apiFetch('/categories', token.get);
// → [{ id: 1, name: 'Inbox', code: 'INBOX', description: '...' }, ...]
```

**Example: fetch messages for a category**
```javascript
const messages = await apiFetch(
  `/messages?categoryCode=${encodeURIComponent('INBOX')}`,
  token.get
);
// → [{ id: 'uuid', title: '...', body: '...', categoryCode: 'INBOX' }, ...]
```

**Example: handling a 401 (auto-redirect to login)**
```javascript
// In index.js — the app wraps apiFetch to clear the token and redirect on 401:
async apiFetch(path) {
  try {
    return await apiFetch(path, token.get);
  } catch (err) {
    if (err.status === 401) {
      token.clear();
      window.location.href = buildGitHubAuthUrl(); // redirect to GitHub login
    }
    throw err;
  }
}
```

---

## Adding a New API Call

To call a new endpoint, use `apiFetch` with the appropriate path:

```javascript
// POST is not wrapped by apiFetch — use fetch directly for mutations:
const res = await fetch(`${API_BASE}/categories`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token.get()}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ name: 'Archive', code: 'ARCHIVE', description: 'Archived emails' }),
});
const created = await res.json();
// → { id: 4, name: 'Archive', code: 'ARCHIVE', description: 'Archived emails' }
```

> `apiFetch` only handles GET requests. For POST/PUT/DELETE, use `fetch` directly with the `Authorization` header from `token.get()`.
