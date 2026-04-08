# AGENTS.md

Guidance for AI agents working in this repository.

## Project overview

**emails-summarizer** is an email client application where users browse a category sidebar and read messages in a detail panel. It is a Gradle multi-project monorepo with two subprojects — a Vue 3 frontend and a Spring Boot 4 backend — connected via a REST API secured with GitHub OAuth2.

```
emails-summarizer/
├── build.gradle          # Root: shared group/version/repos for all subprojects
├── settings.gradle       # Declares subprojects: ui, api-rs
├── setup_env_template.sh # Template for required env vars
├── gradle/wrapper/       # Gradle 8.13 wrapper
├── local/                # Local dev secrets (gitignored)
│   └── setup_env.sh      # Actual env vars (not committed)
├── ui/                   # Frontend — Vue 3 (CDN, no bundler)
│   ├── build.gradle
│   ├── index.html        # Single-page app entry point (template only)
│   ├── index.js          # Vue 3 app entry point
│   ├── composables/
│   │   └── useAuth.js    # OAuth2 flow: token helpers, GitHub auth URL, code exchange
│   ├── services/
│   │   └── api.js        # API fetch wrapper with Bearer token injection
│   └── styles/
│       └── main.css      # Global stylesheet
├── api-rs/               # Backend API — Java, Spring Boot 4
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/emailssummarizer/apirs/
│       │   ├── ApiRsApplication.java
│       │   ├── category/        # Category JPA entity, repo, controller, request DTO
│       │   ├── message/         # Message JPA entity, repo, controller, request DTO
│       │   ├── oauth/           # OAuthController — GitHub token exchange proxy
│       │   └── security/        # ResourceServerConfig, GitHubOpaqueTokenIntrospector
│       └── resources/
│           ├── application.yml  # H2 datasource, JPA, OAuth2 config, role allow-lists
│           ├── schema.sql       # CATEGORY and MESSAGE DDL
│           └── data.sql         # Sample seed data
├── openspec/             # Spec-driven development workflow
│   ├── config.yaml       # OpenSpec project config and context
│   ├── changes/          # Active change proposals
│   │   ├── add-crud-categories-messages-api/  # CRUD endpoints (implemented)
│   │   ├── add-crud-authorization/            # Three-role authorization (proposed)
│   │   └── archive/      # Completed changes
│   └── specs/            # Promoted capability specs
└── .github/
    ├── prompts/          # Copilot prompt files (.prompt.md)
    └── skills/           # Copilot skill files (SKILL.md)
```

## Code Layout Skills

Before writing any code, read and follow:

- `.agents/skills/ui-code-layout/SKILL.md` for frontend changes
- `.agents/skills/api-rs-code-layout/SKILL.md` for backend changes


## Build system

- **Tool**: Gradle 8.13 (via wrapper — always use `./gradlew`, never a system `gradle`)
- **Group**: `com.emails-summarizer`
- **Version**: `0.1.0`
- Shared config (repositories, group, version) lives in the root `build.gradle`.
- Subproject-specific config goes in `ui/build.gradle` and `api-rs/build.gradle`.

Common commands:
```bash
./gradlew build          # Build all subprojects
./gradlew :ui:build      # Build frontend only
./gradlew :api-rs:build  # Build backend only
./gradlew :api-rs:bootRun  # Start the Spring Boot API (requires env vars below)
./gradlew tasks          # List available tasks
```

## Subprojects

### `ui` — Frontend

- **Tech stack**: Vue 3 loaded from unpkg CDN (`vue.esm-browser.js`). No bundler yet.
- **Entry point**: `ui/index.html` loads `ui/styles/main.css` and `ui/index.js` (module).
- **Serving**: Must be served over HTTP (not `file://`). Use `npx serve ui` (port 3000) or `python3 -m http.server 5500` from inside `ui/`.
- **Layout**: Two-panel single-page app:
  - Left sidebar — category list fetched from `GET /categories`.
  - Right panel — message list fetched from `GET /messages?categoryCode=<code>` on category selection.
- **Auth**: OAuth2 Authorization Code flow via GitHub. Token stored in `sessionStorage`. Managed by `composables/useAuth.js` — checks for existing token or incoming `?code=` callback; exchanges the code via `POST /oauth2/token` on api-rs (GitHub blocks direct browser CORS). Redirects to GitHub login if unauthenticated.
- **Config** (constants in `composables/useAuth.js` and `services/api.js`):
  - `GITHUB_CLIENT_ID` — set `window.__GITHUB_CLIENT_ID__` before the script or replace the placeholder.
  - `API_BASE` — set `window.__API_BASE__` or defaults to `http://local.example.com:8080`.
  - `REDIRECT_URI` — derived from `window.location.origin + pathname`; must match the GitHub OAuth App callback URL exactly.
- **Code layout** — follow the `ui-code-layout` skill before writing any UI code.

### `api-rs` — Backend API

- **Tech stack**: Java 21, Spring Boot 4.0.0-M1, Spring Data JPA, Spring Security, OAuth2 Resource Server.
- **Storage**: H2 in-memory database (`jdbc:h2:mem:emailsdb`); schema initialized from `schema.sql`, seed data from `data.sql`. H2 console available at `http://localhost:8080/h2-console` in dev.
- **Port**: 8080.
- **REST endpoints**:
  - `GET /categories` — returns `List<Category>` (id, name, code, description). Requires `ROLE_READ`.
  - `GET /messages?categoryCode=<code>` — returns `List<Message>` (id UUID, title, body, categoryCode). Requires `ROLE_READ`. Returns 400 if `categoryCode` param is missing.
  - `POST /categories` — create a category. Requires `ROLE_EDIT`. Returns 409 if code already exists.
  - `PUT /categories/{code}` — update name/description. Requires `ROLE_EDIT`. Returns 404 if not found.
  - `DELETE /categories/{code}` — delete a category. Requires `ROLE_DEL`. Returns 409 if category has messages, 404 if not found.
  - `POST /messages` — create a message. Requires `ROLE_EDIT`. Returns 409 if categoryCode unknown.
  - `PUT /messages/{id}` — update title/body/categoryCode. Requires `ROLE_EDIT`. Returns 404 if not found, 409 if categoryCode unknown.
  - `DELETE /messages/{id}` — delete a message. Requires `ROLE_DEL`. Returns 404 if not found.
  - `POST /oauth2/token` — token exchange proxy. Accepts `{ code, redirectUri }`, calls GitHub's token endpoint server-side (keeping `client_secret` off the browser), returns `{ accessToken }`. Permitted without authentication.
- **Security**: Resource Server mode. GitHub opaque tokens validated via `GET https://api.github.com/user`. Three independent roles assigned at introspection time based on allow-lists. CORS configured to allow the `ui` origin (e.g. `http://localhost:5500`).
- **Required environment variables**:

  ```bash
  export GITHUB_CLIENT_ID=<your-github-oauth-app-client-id>
  export GITHUB_CLIENT_SECRET=<your-github-oauth-app-client-secret>
  export READERS_GITHUB_LOGINS=<comma-separated-logins>   # grants ROLE_READ (GET)
  export EDITORS_GITHUB_LOGINS=<comma-separated-logins>   # grants ROLE_EDIT (POST, PUT)
  export DELETERS_GITHUB_LOGINS=<comma-separated-logins>  # grants ROLE_DEL (DELETE)
  ```

- **GitHub OAuth App**: Register at <https://github.com/settings/developers>. Set "Authorization callback URL" to the `ui` origin (e.g. `http://localhost:5500/index.html`).
- **Code layout** — follow the `api-rs-code-layout` skill before writing any backend code.

## Data model

```sql
CATEGORY (id BIGINT PK, name VARCHAR, code VARCHAR UNIQUE, description VARCHAR)
MESSAGE  (id UUID PK DEFAULT RANDOM_UUID(), title VARCHAR, body CLOB,
          category_code VARCHAR FK → CATEGORY.code)
```

Sample seed categories: INBOX, WORK, PERSONAL (see `api-rs/src/main/resources/data.sql`).

## Running locally

```bash
# Terminal 1 — start api-rs
export GITHUB_CLIENT_ID=<id>
export GITHUB_CLIENT_SECRET=<secret>
export READERS_GITHUB_LOGINS=<your-login>
export EDITORS_GITHUB_LOGINS=<your-login>
export DELETERS_GITHUB_LOGINS=<your-login>
./gradlew :api-rs:bootRun

# Terminal 2 — serve ui
cd ui && python3 -m http.server 5500
# or: npx serve . -p 3000

# Then open http://localhost:5500/index.html
```

## Development workflow (OpenSpec)

This project uses an OpenSpec spec-driven workflow. All non-trivial feature work flows through:

1. **Explore** — understand the problem space (`/opsx explore` or `opsx:explore` skill)
2. **Propose** — draft a spec + tasks for a change (`/opsx propose` or `opsx:propose` skill)
3. **Apply** — implement tasks from a change (`/opsx apply` or `opsx:apply` skill)
4. **Archive** — mark a completed change as done (`/opsx archive` or `opsx:archive` skill)

Change proposals live in `openspec/changes/` while active; completed ones move to `openspec/changes/archive/`.
Capability specs are promoted to `openspec/specs/` after archiving.

Project-level context (tech stack, conventions) is maintained in `openspec/config.yaml` under the `context:` key.

Completed changes (both archived):

- `2026-04-07-add-categories-messages-api` — Spring Boot API with Category/Message endpoints and GitHub OAuth2 security.
- `2026-04-07-add-ui-main-page` — Vue 3 single-page UI with OAuth2 login flow, category sidebar, and message panel.

Active changes (proposed, partially or fully implemented):

- `add-crud-categories-messages-api` — CRUD endpoints for categories and messages (implemented).
- `add-crud-authorization` — Three-role authorization (`ROLE_READ`/`ROLE_EDIT`/`ROLE_DEL`) backed by separate GitHub login allow-lists (proposed).

## Key conventions

- Use `./gradlew` (never bare `gradle`).
- Don't add shared dependencies directly in subproject build files if they belong in the root `build.gradle` `allprojects`/`subprojects` block.
- Follow the OpenSpec workflow for all non-trivial changes — don't skip straight to implementation without a proposal.
- Commit messages should be descriptive; the initial commit style is plain prose.
- The `ui` must be served over HTTP (not opened as `file://`) — OAuth2 redirect URIs require a real origin.
- Never embed `GITHUB_CLIENT_SECRET` in frontend code or commit it to the repo.
- **Code layout skills**: always read the `api-rs-code-layout` skill before writing backend code, and the `ui-code-layout` skill before writing frontend code.
- Authorization roles are independent — a user must appear in each allow-list (`READERS_`, `EDITORS_`, `DELETERS_GITHUB_LOGINS`) separately to hold multiple roles.
