## Why

The `ui` subproject currently has no frontend implementation. Users need a working main page that fetches live data from the `api-rs` backend, authenticates via OAuth2 (Authorization Code flow), displays email categories in the left sidebar, and shows the messages of the selected category on the right — replacing the previous inline mock-data approach.

## What Changes

- Add `ui/index.html` as the application entry point with a Vue 3 (CDN) setup.
- Implement a two-panel layout: scrollable category list on the left, message list on the right.
- Left sidebar fetches categories from `GET /categories` (api-rs) and renders them; clicking a category selects it.
- Right panel fetches messages from `GET /messages?categoryCode={code}` (api-rs) for the selected category and renders them.
- Authenticate using OAuth2 Authorization Code flow against the embedded Spring Authorization Server in `api-rs`; store the access token in `sessionStorage` and attach it as a Bearer token on all API requests.
- Handle the OAuth2 redirect callback in the same `index.html`.
- Update `ui/build.gradle` to reflect the Vue.js frontend technology choice.

## Capabilities

### New Capabilities

- `category-sidebar`: Fetch and display email categories from the API in the left sidebar; supports single-item selection.
- `message-list-panel`: Fetch and display messages for the selected category from the API in the right panel.
- `oauth2-client`: OAuth2 Authorization Code flow initiated from the browser; token acquisition, storage, and attachment to API requests.

### Modified Capabilities

*(none — no existing specs)*

## Non-Goals

- Email compose, reply, or send functionality.
- Build tooling (Vite, webpack, etc.) — CDN only at this stage.
- Refresh token handling — session token only; re-login on expiry is acceptable.
- Responsive/mobile breakpoints beyond basic desktop layout.
- Pagination of categories or messages.

## Impact

- **Files added**: `ui/index.html`
- **Files modified**: `ui/build.gradle` (technology annotation/comment)
- **Runtime dependencies**: Vue 3 via CDN; OAuth2 redirect requires the page to be served over HTTP (not `file://`)
- **API dependency**: `api-rs` must be running and reachable; its OAuth2 `redirect_uri` must include the `ui` origin
- **No breaking changes** to `api-rs` or root build files
