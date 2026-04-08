# emails-summarizer — Documentation

**emails-summarizer** is a Gradle monorepo email client application where users browse a category sidebar and read messages in a detail panel. Authentication is handled via GitHub OAuth2. The project consists of two independently deployable units — a Vue 3 frontend (`ui`) and a Spring Boot 4 REST API (`api-rs`) — that communicate exclusively through a REST API.

---

## Components

| Component | Description | Docs |
|---|---|---|
| `ui` | Vue 3 single-page application | [ui/index.md](ui/index.md) |
| `api-rs` | Spring Boot 4 REST API | [api-rs/index.md](api-rs/index.md) |
| Auth | End-to-end OAuth2 flow and authorization roles | [auth.md](auth.md) |
| Data model | Database schema and entity relationships | [data-model.md](data-model.md) |
| OpenSpec | Spec-driven development workflow | [openspec.md](openspec.md) |

---

## Getting started

**Prerequisites:** Java 21, Node.js (for `npx serve`), a registered [GitHub OAuth App](https://github.com/settings/developers).

**Step 1 — Set environment variables**

```bash
export GITHUB_CLIENT_ID=<your-client-id>
export GITHUB_CLIENT_SECRET=<your-client-secret>
export READERS_GITHUB_LOGINS=<comma-separated-github-logins>
export EDITORS_GITHUB_LOGINS=<comma-separated-github-logins>
export DELETERS_GITHUB_LOGINS=<comma-separated-github-logins>
```

**Step 2 — Start the backend**

```bash
./gradlew :api-rs:bootRun
# API available at http://localhost:8080
```

**Step 3 — Serve the frontend**

```bash
cd ui && python3 -m http.server 5500
# UI available at http://localhost:5500/index.html
# Or: npx serve . -p 3000
```

**Step 4 — Open the app**

Navigate to `http://localhost:5500/index.html`. The app redirects to GitHub for login on the first visit.

> The UI **must** be served over HTTP — opening `index.html` directly as `file://` breaks OAuth2 redirect URIs.
