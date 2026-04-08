## Context

The `ui` subproject is a bare Gradle stub. The emails-summarizer application needs a working main page that connects to the `api-rs` Spring Boot backend for live data and authenticates via OAuth2 Authorization Code flow. The layout is a two-panel master-detail pattern: categories on the left, messages on the right. There is no bundler yet — the page must run as a single HTML file loaded from an HTTP server.

## Goals / Non-Goals

**Goals:**
- Deliver a functional `ui/index.html` using Vue 3 (CDN, `<script type="module">`).
- Implement OAuth2 Authorization Code flow: redirect to the Spring Authorization Server, receive the auth code, exchange it for a token, store in `sessionStorage`.
- Two-panel layout: left sidebar shows categories fetched from `GET /categories`; right panel shows messages fetched from `GET /messages?categoryCode={code}`.
- Attach the Bearer token on all API requests via `Authorization: Bearer <token>` header.
- Auto-select the first category on load and immediately fetch its messages.
- Document the Vue.js technology choice in `ui/build.gradle`.

**Non-Goals:**
- Refresh token handling — redirect to login on token expiry.
- Build tooling (Vite, webpack, etc.).
- Pagination or filtering.
- Responsive/mobile layout.
- PKCE (Proof Key for Code Exchange) — omitted for simplicity at this stage; add in a follow-up when the page is deployed with TLS.

## Decisions

### Decision: Vue 3 via CDN (no bundler)

**Choice**: Load Vue 3 from `https://unpkg.com/vue@3/dist/vue.esm-browser.js` using `<script type="module">`.

**Rationale**: No Gradle JS toolchain exists yet. CDN approach keeps the page self-contained and immediately runnable from any HTTP server. This is scaffolding — the build pipeline is a follow-up change.

**Alternative considered**: Vite — more powerful but requires Node.js toolchain setup, which is out of scope now.

---

### Decision: OAuth2 Authorization Code flow (no PKCE for now)

**Choice**: Implement the Authorization Code flow by redirecting to `/oauth2/authorize`, receiving the `code` in the callback URL, exchanging it for a token via `POST /oauth2/token` (with `client_id` + `client_secret` in the request body), and storing the `access_token` in `sessionStorage`.

**Rationale**: The Spring Authorization Server in `api-rs` supports Authorization Code natively. The page needs a real Bearer token to call the secured endpoints. `sessionStorage` clears on tab close, limiting token leakage window.

**Important constraint**: The page MUST be served over HTTP (e.g. `python3 -m http.server` or a Spring Boot static-resource route), not opened as `file://`, because OAuth2 redirects require a registered `redirect_uri` with a real origin.

**Alternative considered**: PKCE — adds security for public clients but requires additional Spring Authorization Server config changes. Deferred to a future change.

**Alternative considered**: Implicit flow — deprecated; not used.

---

### Decision: Token stored in `sessionStorage`, attached via fetch wrapper

**Choice**: On every API call use a shared `apiFetch(url)` helper function that reads `sessionStorage.getItem('access_token')` and injects `Authorization: Bearer <token>` into the request headers.

**Rationale**: Centralizes token attachment logic so both the categories and messages calls benefit consistently. `sessionStorage` is scoped to the tab — no cross-tab token sharing, which reduces risk compared to `localStorage`.

---

### Decision: Categories and messages fetched on-demand (no caching)

**Choice**: Categories are fetched once on `mounted()`. Messages are re-fetched every time the user selects a category via the `selectCategory(code)` method.

**Rationale**: Simplest correct implementation. The api-rs service is local, so latency is negligible. Caching can be added later.

---

### Decision: Layout using CSS Flexbox

**Choice**: Full-viewport flex container — fixed-width left sidebar (~280 px) + flex-grow right panel. Both panels scroll independently with `overflow-y: auto`.

**Rationale**: Straightforward, well-supported, matches the master-detail mockup reference.

## Risks / Trade-offs

- **CDN dependency at runtime** → If `unpkg.com` is unavailable the page won't load. Mitigation: vendor `vue.esm-browser.js` locally once a build pipeline exists.
- **No PKCE** → The `client_secret` is visible in the browser's network tab. Acceptable for a local dev scaffold; PKCE must be added before any public deployment.
- **`redirect_uri` must match exactly** → The Spring Authorization Server `api-rs` registered client must have the `ui` origin registered as an allowed `redirect_uri`. Misconfiguration produces an OAuth2 error before the user sees any data.
- **Token expiry results in silent 401** → Without refresh token handling, expired tokens cause API calls to fail. The page should detect 401 responses and redirect to login.
- **Page must be served over HTTP** → Cannot be opened as `file://`; developers must run a local HTTP server.
