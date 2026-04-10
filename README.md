# emails-summarizer

An email client application where users browse a category sidebar and read messages in a detail panel. Authentication is handled via GitHub OAuth2.

The project is a **Gradle multi-project monorepo** consisting of two independently deployable subprojects:

| Subproject | Description | Port |
|---|---|---|
| `ui` | Vue 3 single-page application (no bundler, CDN) | `5500` |
| `api-rs` | Spring Boot 4 REST API backed by H2 in-memory database | `8080` |

---

## Project Structure

```
emails-summarizer/
├── build.gradle          # Root: shared group/version/repos for all subprojects
├── settings.gradle       # Declares subprojects: ui, api-rs
├── setup_env_template.sh # Template for required environment variables
├── gradle/wrapper/       # Gradle 8.13 wrapper
├── local/                # Local dev secrets (gitignored)
│   └── setup_env.sh      # Actual env vars (not committed)
├── ui/                   # Frontend — Vue 3 (CDN, no bundler)
│   ├── index.html        # Single-page app entry point
│   ├── index.js          # Vue 3 app entry point
│   ├── composables/
│   │   └── useAuth.js    # OAuth2 flow helpers
│   ├── services/
│   │   └── api.js        # API fetch wrapper with Bearer token injection
│   └── styles/
│       └── main.css      # Global stylesheet
├── api-rs/               # Backend API — Java 21, Spring Boot 4
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/emailssummarizer/apirs/
│       │   ├── category/  # Category JPA entity, repo, controller, service
│       │   ├── message/   # Message JPA entity, repo, controller, service
│       │   ├── oauth/     # GitHub token exchange proxy
│       │   └── security/  # OAuth2 resource server configuration
│       └── resources/
│           ├── application.yml  # App configuration
│           ├── schema.sql       # Database DDL
│           └── data.sql         # Seed data
└── docs/                 # Project documentation
```

---

## Prerequisites

- **Java 21**
- **Python 3** (to serve the UI) or **Node.js** (for `npx serve`)
- A registered [GitHub OAuth App](https://github.com/settings/developers)
  - Set the **Authorization callback URL** to your UI origin (e.g. `http://localhost:5500/index.html`)

---

## Environment Variables

Before running the application, export the following environment variables. You can copy `setup_env_template.sh` to `local/setup_env.sh` and fill in your values:

```bash
export GITHUB_CLIENT_ID=<your-github-oauth-app-client-id>
export GITHUB_CLIENT_SECRET=<your-github-oauth-app-client-secret>
export READERS_GITHUB_LOGINS=<comma-separated-github-logins>   # grants ROLE_READ (GET)
export EDITORS_GITHUB_LOGINS=<comma-separated-github-logins>   # grants ROLE_EDIT (POST, PUT)
export DELETERS_GITHUB_LOGINS=<comma-separated-github-logins>  # grants ROLE_DEL (DELETE)
```

> **Note:** Authorization roles are independent — a user must appear in each allow-list separately to hold multiple roles.

---

## Building the Project

All build commands use the Gradle wrapper (`./gradlew`). Never use a system-installed `gradle` directly.

### Build all subprojects

```bash
./gradlew build
```

### Build individual subprojects

```bash
./gradlew :ui:build       # Build frontend only
./gradlew :api-rs:build   # Build backend only
```

### List all available tasks

```bash
./gradlew tasks
```

---

## Running the Application

### Running both subprojects

Open two terminal windows and start each subproject separately (see below), then navigate to `http://localhost:5500/index.html`.

---

### Running the Backend (`api-rs`)

The Spring Boot API runs on port **8080**.

**Option 1 — Gradle convenience task (from project root):**

```bash
./gradlew runApi
```

**Option 2 — Subproject task directly:**

```bash
./gradlew :api-rs:bootRun
```

Once started, the API is available at `http://localhost:8080`.  
The H2 console is available at `http://localhost:8080/h2-console` (dev only).

---

### Running the Frontend (`ui`)

The UI **must be served over HTTP** — opening `index.html` as `file://` breaks OAuth2 redirect URIs.

**Option 1 — Gradle convenience task (from project root, requires Python 3):**

```bash
./gradlew runUi
```

**Option 2 — Python built-in HTTP server:**

```bash
cd ui && python3 -m http.server 5500
```

**Option 3 — Node.js `serve`:**

```bash
cd ui && npx serve . -p 3000
```

Once started, open your browser at `http://localhost:5500/index.html` (or the port you chose). On first visit, the app redirects to GitHub for login.

---

## REST API Endpoints

| Method | Path | Description | Required Role |
|---|---|---|---|
| `GET` | `/categories` | List all categories | `ROLE_READ` |
| `GET` | `/messages?categoryCode=<code>` | List messages for a category | `ROLE_READ` |
| `POST` | `/categories` | Create a category | `ROLE_EDIT` |
| `PUT` | `/categories/{code}` | Update a category | `ROLE_EDIT` |
| `DELETE` | `/categories/{code}` | Delete a category | `ROLE_DEL` |
| `POST` | `/messages` | Create a message | `ROLE_EDIT` |
| `PUT` | `/messages/{id}` | Update a message | `ROLE_EDIT` |
| `DELETE` | `/messages/{id}` | Delete a message | `ROLE_DEL` |
| `POST` | `/oauth2/token` | GitHub token exchange proxy | Public |

---

## Data Model

```sql
CATEGORY (id BIGINT PK, name VARCHAR, code VARCHAR UNIQUE, description VARCHAR)
MESSAGE  (id UUID PK DEFAULT RANDOM_UUID(), title VARCHAR, body CLOB,
          category_code VARCHAR FK → CATEGORY.code)
```

Sample seed categories: **INBOX**, **WORK**, **PERSONAL**.

---

## Documentation

Full project documentation is available in the [`docs/`](docs/index.md) directory:

- [`docs/index.md`](docs/index.md) — Getting started
- [`docs/auth.md`](docs/auth.md) — End-to-end OAuth2 flow and authorization roles
- [`docs/data-model.md`](docs/data-model.md) — Database schema and entity relationships
- [`docs/api-rs/`](docs/api-rs/index.md) — Backend API reference
- [`docs/ui/`](docs/ui/index.md) — Frontend reference
- [`docs/javadocs/`](docs/javadocs/index.html) — Generated Javadoc

---

## Cline Commands History

The following commands from the shell history are related to Cline (retrieved via `history | grep 'cline'`):

```bash
 cline -y "Generate README.md file, where to describe an overview of project, how to build the project and how to run the subprojects"
 cline "commit the current changes, with a descriptive message and push the changes to current branch"
 cline -y --json "retrives all the subprojects; foreach returns project name, short description, build command, run command"
 client -y "add in README.md file a new section, where to enumerates the last commands from bash (call history), that their names start with cline: history | grep 'cline'"
 cline -y "add in README.md file a new section, where to enumerates the last commands from bash (call history), that their names start with cline: history | grep 'cline'"
```

Run cross-worktree context piping

```bash
git branch feature/auth
git push origin feature/auth
git worktree add ../emails-summarizer-auth feature/auth

cline --cwd ../emails-summarizer-auth -y "now users' roles are setup using env vars like READERS_GITHUB_LOGINS, EDITORS_GITHUB_LOGINS, DELETERS_GITHUB_LOGINS; change like follow: create DB tables users - containing user informations (login, and others infos that can be extracted from github), roles - link each user to a list of roles (id, login - pk of users tables, role); create Repository and Service class that implement the functionality; when user login search his login into the users tables and if it is found retrieves his roles, otherwise creates an entry to users and assign ROLE_READ in roles table." 

git worktree remove ../emails-summarizer-auth
```
