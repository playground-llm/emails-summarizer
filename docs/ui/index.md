# UI — Overview

The `ui` subproject is a Vue 3 single-page application that lets authenticated users browse email categories in a sidebar and read messages in a detail panel.

---

## Contents

| Document | Description |
|---|---|
| [auth.md](auth.md) | OAuth2 flow, token management (`useAuth.js`) |
| [api-service.md](api-service.md) | API fetch service (`services/api.js`) |
| [layout.md](layout.md) | App layout, sidebar, message panel (`index.js`, `index.html`) |

---

## Tech Stack

- **Framework**: Vue 3 (loaded from unpkg CDN — no bundler required)
- **Language**: JavaScript (ES modules)
- **Auth**: GitHub OAuth2 Authorization Code flow
- **HTTP**: Native `fetch` API, wrapped in `services/api.js`
- **State**: Vue component data (no Pinia — single-component app)

---

## Project Structure

```
ui/
├── index.html          ← Single-page app shell; loads styles and index.js
├── index.js            ← Vue 3 app entry point — wires auth, API, and layout
├── composables/
│   └── useAuth.js      ← OAuth2 flow: token helpers, auth URL, code exchange
├── services/
│   └── api.js          ← Fetch wrapper: base URL, Bearer token, 401 handling
└── styles/
    └── main.css        ← Global stylesheet
```

---

## Running Locally

The UI must be served over HTTP — opening it as `file://` breaks OAuth2 redirect URIs.

**Option A — Python**
```bash
cd ui
python3 -m http.server 5500
# Open http://localhost:5500/index.html
```

**Option B — Node**
```bash
npx serve ui -p 3000
# Open http://localhost:3000/index.html
```

---

## Configuration

Two runtime overrides can be set on `window` before `index.js` loads (e.g., in `index.html`):

| Variable | Default | Description |
|---|---|---|
| `window.__GITHUB_CLIENT_ID__` | hardcoded in `useAuth.js` | GitHub OAuth App client ID |
| `window.__API_BASE__` | `http://local.example.com:8080` | Base URL of the `api-rs` backend |

**Example: override API_BASE in index.html**
```html
<script>
  window.__API_BASE__ = 'http://localhost:8080';
  window.__GITHUB_CLIENT_ID__ = 'your-client-id';
</script>
<script type="module" src="index.js"></script>
```
