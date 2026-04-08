## 1. Project Setup

- [x] 1.1 Add a comment in `ui/build.gradle` documenting Vue.js as the chosen frontend technology
- [x] 1.2 Confirm `api-rs` has a registered GitHub OAuth App callback — `redirect_uri` matches the local origin where `ui/index.html` is served (e.g. `http://localhost:5500/index.html`)

## 2. HTML Shell & Layout

- [x] 2.1 Create `ui/index.html` with the HTML5 boilerplate (`<!DOCTYPE html>`, `<meta charset>`, `<meta viewport>`, `<title>Emails Summarizer`)
- [x] 2.2 Add a `<style>` block implementing the full-viewport flex layout: sidebar fixed-width (220 px) + detail panel `flex: 1`
- [x] 2.3 Style the sidebar (background color, border-right, `overflow-y: auto`, independent scroll)
- [x] 2.4 Style the message panel (`overflow-y: auto`, padding, independent scroll)

## 3. Vue App Bootstrap

- [x] 3.1 Add `<script type="module">` importing Vue 3 from the unpkg CDN (`vue.esm-browser.js`)
- [x] 3.2 Define `createApp({})` with `data()` returning: `categories`, `selectedCategoryCode`, `messages`, `isAuthenticated`, `authError`, `categoriesLoading`, `categoriesError`, `messagesLoading`, `messagesError`
- [x] 3.3 Mount the app to `<div id="app">`

## 4. OAuth2 Authorization Code Flow

- [x] 4.1 In app `created()`, read `sessionStorage.getItem('access_token')` — if present, set `isAuthenticated = true` and skip the OAuth2 flow
- [x] 4.2 Check for a `code` query parameter in `window.location.search`; if present, exchange it via `POST /oauth2/token` on api-rs (proxy — GitHub's endpoint blocks browser CORS) with `{ code, redirectUri }`
- [x] 4.3 On successful token exchange, store `access_token` in `sessionStorage`, clear the `code` from the URL via `history.replaceState`, then proceed to load data
- [x] 4.4 If neither a token nor a code is present, build a GitHub authorization URL with `response_type=code`, `client_id`, `redirect_uri`, `scope=read:user`, and a random CSRF `state`; render the "Sign in with GitHub" button pointing to it
- [x] 4.5 If the token exchange request fails, set `authError` state and display an inline error message; do not proceed to API calls

## 5. API Fetch Helper

- [x] 5.1 Implement `apiFetch(path)` method that reads the token from `sessionStorage` and calls `fetch(API_BASE + path, { headers: { Authorization: 'Bearer <token>' } })`
- [x] 5.2 In `apiFetch`, detect HTTP 401 responses, clear `sessionStorage`, and redirect to the GitHub authorization URL for re-authentication

## 6. Category Sidebar

- [x] 6.1 In `loadCategories()`, call `apiFetch('/categories')`, parse the JSON response, and store results in `categories`
- [x] 6.2 Render the sidebar with `v-for="cat in categories"` showing `cat.name` per item
- [x] 6.3 `loadCategories()` is called immediately when `isAuthenticated` becomes true (end of `created()`)
- [x] 6.4 Apply a selected highlight using `:class="{ active: cat.code === selectedCategoryCode }"`
- [x] 6.5 Handle click on a category item via `@click="selectCategory(cat)"` — sets `selectedCategoryCode` and calls `apiFetch` for messages
- [x] 6.6 Display a "No categories found." empty-state message when `categories` is empty after a successful fetch

## 7. Message List Panel

- [x] 7.1 Implement `selectCategory(cat)` method that calls `apiFetch('/messages?categoryCode=...')` and stores results in `messages`
- [x] 7.2 Render the message list with `v-for="msg in messages"` showing `msg.title` and `msg.body`
- [x] 7.3 Display a loading indicator while messages are being fetched (`v-else-if="messagesLoading"`)
- [x] 7.4 Display an empty-state message when `messages` is empty after a successful fetch (`v-else-if="messages.length === 0"`)
- [x] 7.5 Display an error message in the panel when `apiFetch` returns a non-2xx response (`v-else-if="messagesError"`)

## 8. Verification (manual)

- [ ] 8.1 Start `api-rs` with `GITHUB_CLIENT_ID=<id> GITHUB_CLIENT_SECRET=<secret> ./gradlew :api-rs:bootRun` and serve `ui/` on a local HTTP server (e.g. `cd ui && python3 -m http.server 5500`)
- [ ] 8.2 Open `http://localhost:5500/index.html` — confirm the "Sign in with GitHub" card is shown
- [ ] 8.3 Click "Sign in with GitHub", log in on GitHub, confirm the browser redirects back to `index.html` and the `?code=` param disappears from the URL
- [ ] 8.4 Confirm the category sidebar renders all three categories (Inbox, Work, Personal)
- [ ] 8.5 Click each category and verify the message panel updates with that category's messages
- [ ] 8.6 Verify the active highlight moves to the clicked category
- [ ] 8.7 Call `GET http://localhost:8080/categories` without a token (e.g. in a new browser tab) and confirm HTTP 401 is returned
- [ ] 8.8 Manually clear `sessionStorage` in DevTools, refresh the page, and confirm the OAuth2 redirect flow starts again
