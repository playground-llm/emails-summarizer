---
paths:
  - "ui/**"
---

# UI Technical Specifications

Technical specifications for the `ui` subproject.
Apply these rules to every frontend code change.

---

## Tech stack

| Concern | Technology |
|---|---|
| Framework | Vue 3 (`vue.esm-browser.js` from unpkg CDN) |
| Language | JavaScript (ES modules) — no TypeScript |
| Bundler | None — ES modules served directly by an HTTP server |
| Styles | Plain CSS (`ui/styles/main.css`) |
| State | Component-local `data()` — no Pinia yet |
| Routing | None — single-page, single view |
| HTTP | Native `fetch` API |

---

## Project layout

```
ui/
  ├── index.html        ← HTML shell; loads main.css and index.js (module)
  ├── index.js          ← Vue 3 app entry point; wires auth + API + template
  ├── styles/
  │   └── main.css      ← Global stylesheet
  ├── composables/
  │   └── useAuth.js    ← OAuth2 flow: token helpers, auth URL, code exchange
  ├── services/
  │   └── api.js        ← Fetch wrapper: base URL, Bearer token injection, 401 handling
  ├── components/       ← Reusable UI components (buttons, inputs, etc.)
  ├── views/            ← Page-level components (used with Vue Router when added)
  ├── router/           ← Vue Router config (index.js) — when routing is added
  ├── stores/           ← Pinia state management — when global state is added
  ├── assets/           ← Images, fonts, static assets
  ├── utils/            ← Utility/helper functions
  └── scripts/          ← Build scripts or configuration files
```

New code MUST be placed in the appropriate folder. The `types/` folder is not used (no TypeScript).

---

## File responsibilities

### `index.html`
- HTML shell only — no inline `<style>` or `<script>` logic
- Loads `./styles/main.css` via `<link>`
- Loads `./index.js` via `<script type="module">`
- Contains the Vue template markup (`#app`)

### `index.js`
- Creates and mounts the Vue 3 app
- Handles the OAuth2 lifecycle in `created()` (code callback, existing token, unauthenticated)
- Calls `loadCategories()` and `selectCategory()` methods
- Does NOT contain raw `fetch` calls — all HTTP goes through `services/api.js`
- Does NOT contain OAuth2 logic — that belongs in `composables/useAuth.js`

### `composables/useAuth.js`
- Exports `token` (get / save / clear helpers over `sessionStorage`)
- Exports `REDIRECT_URI` (derived from `window.location.origin + pathname`)
- Exports `buildGitHubAuthUrl()` — constructs the GitHub authorize URL with a random CSRF state
- Exports `exchangeCode(code)` — POSTs to `api-rs /oauth2/token`, returns the access token
- Does NOT make any other API calls

### `services/api.js`
- Exports `API_BASE` — defaults to `window.__API_BASE__ || 'http://local.example.com:8080'`
- Exports `apiFetch(path, getToken)` — attaches `Authorization: Bearer <token>`, throws on HTTP error, sets `err.status = 401` for unauthorised responses
- Has no knowledge of OAuth2 flow, GitHub, or session state

### `styles/main.css`
- Contains all global CSS (reset, layout, typography, components)
- No inline styles in HTML or `<style>` blocks in JS

---

## Configuration

All config is runtime-injectable via `window` globals — no build step required:

| Variable | Where | Default | Override |
|---|---|---|---|
| `GITHUB_CLIENT_ID` | `useAuth.js` | hardcoded placeholder | `window.__GITHUB_CLIENT_ID__` |
| `API_BASE` | `api.js` | `http://local.example.com:8080` | `window.__API_BASE__` |
| `REDIRECT_URI` | `useAuth.js` | `window.location.origin + pathname` | (derived, no override) |

---

## Serving

- MUST be served over HTTP — `file://` breaks OAuth2 redirect URIs
- Development: `python3 -m http.server 5500` from inside `ui/`, or `npx serve . -p 3000`
- The registered GitHub OAuth App callback URL must exactly match `REDIRECT_URI`

---

## OAuth2 flow (Authorization Code)

1. On page load, `index.js` checks `sessionStorage` for an existing token
2. If no token, `buildGitHubAuthUrl()` generates the GitHub authorize URL (stores random CSRF state in `sessionStorage`)
3. After GitHub redirects back with `?code=`, the CSRF state is verified, then `exchangeCode(code)` is called
4. `exchangeCode` POSTs `{ code, redirectUri }` to `api-rs POST /oauth2/token` (avoids CORS on GitHub's own token endpoint)
5. The returned `accessToken` is saved to `sessionStorage` via `token.save()`
6. On subsequent loads the token is read from `sessionStorage` and attached to every API request

---

## Coding rules

- Use JavaScript only — no TypeScript, no `.ts` files
- Use ES module syntax (`import` / `export`) — no CommonJS (`require`)
- Import Vue 3 from the CDN: `import { createApp } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js'`
- All CSS changes go into `ui/styles/main.css` — never add inline styles
- New reusable UI pieces go in `components/`; new page-level views go in `views/`
- `index.js` MUST remain the single entry point — do not add additional `<script>` tags to `index.html`
- Do not add a bundler or build step without a proposal through the OpenSpec workflow
