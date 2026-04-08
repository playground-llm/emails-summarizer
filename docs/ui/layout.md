# UI — Layout (`index.js`, `index.html`)

`index.js` is the Vue 3 application entry point. It wires together the auth composable, API service, and two-panel layout into a single-page application.

---

## Overview

The app renders a two-panel layout:
- **Left sidebar** — list of categories fetched from `GET /categories`
- **Right panel** — list of messages fetched from `GET /messages?categoryCode=<code>` when a category is selected

On first load, the app checks for an existing token or an incoming OAuth2 callback. If neither is present, it shows a GitHub login button.

---

## Layout Structure

```
┌─────────────────────────────────────────────┐
│  emails-summarizer                           │
├──────────────────┬──────────────────────────┤
│  Categories      │  Messages                 │
│  ─────────────   │  ─────────────────────    │
│  > Inbox         │  Welcome to Emails…       │
│    Work          │  System Notification      │
│    Personal      │  Newsletter: Tech Weekly  │
│                  │                           │
└──────────────────┴──────────────────────────┘
```

---

## App States

The app has three rendering states managed by `isAuthenticated` and `githubAuthUrl`:

| State | Condition | What is shown |
|---|---|---|
| Login | `!isAuthenticated` | GitHub login button |
| Loading | `isAuthenticated && categoriesLoading` | Loading indicator |
| App | `isAuthenticated && categories.length > 0` | Two-panel layout |

---

## Lifecycle — `created()` Hook

The `created()` hook runs on every page load and handles three cases:

```javascript
// Case 1: OAuth2 callback — ?code= is in the URL
if (code) {
  // verify CSRF state, exchange code for token, save to sessionStorage
}

// Case 2: Token already in sessionStorage — show the app
if (token.get()) {
  this.isAuthenticated = true;
  await this.loadCategories();
}

// Case 3: No token, no code — show the login screen
this.githubAuthUrl = buildGitHubAuthUrl();
```

---

## Key Methods

| Method | Description |
|---|---|
| `loadCategories()` | Fetches `GET /categories` and populates the sidebar |
| `selectCategory(cat)` | Sets selected category and fetches `GET /messages?categoryCode=<code>` |
| `apiFetch(path)` | Wrapper that auto-redirects to GitHub login on `401` |

---

## Serving the UI

**Example: serve locally with Python**
```bash
cd ui
python3 -m http.server 5500
# Open http://localhost:5500/index.html
```

**Example: serve locally with Node**
```bash
npx serve ui -p 3000
# Open http://localhost:3000/index.html
```

---

## Extending the Layout

To add a new panel or view, follow the `ui-code-layout` skill:

1. Create a new component file under `ui/components/` (e.g., `MessageDetail.js`)
2. Import and register it in `index.js`
3. Add the template markup in `index.html` using the `v-if`/`v-show` pattern

**Example: adding a message detail panel**
```javascript
// In index.js data():
selectedMessage: null,

// In index.js methods:
selectMessage(msg) {
  this.selectedMessage = msg;
}
```

```html
<!-- In index.html template -->
<div v-if="selectedMessage" class="message-detail">
  <h2>{{ selectedMessage.title }}</h2>
  <p>{{ selectedMessage.body }}</p>
</div>
```
