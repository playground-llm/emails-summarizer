---
paths:
  - "docs/**"
  - "**/*.md"
  - "**/*.mdx"
---

# Documentation Rules

General rules for documenting the emails-summarizer project.
Apply these rules whenever creating or updating any Markdown documentation.

---

## Brief overview

**emails-summarizer** is a Gradle monorepo containing two subprojects:

- **`ui`** — Vue 3 single-page application (CDN, no bundler) that lets users browse email categories and read messages, authenticated via GitHub OAuth2.
- **`api-rs`** — Spring Boot 4 REST API backed by H2 (in-memory), secured as an OAuth2 Resource Server with GitHub opaque-token introspection and three independent authorization roles (`ROLE_READ`, `ROLE_EDIT`, `ROLE_DEL`).

The two units communicate exclusively through a REST API. All feature work is tracked as OpenSpec change proposals in `openspec/changes/`.

---

## Documentation conventions

### File structure

All project documentation lives under `docs/`. The entry point is `docs/index.md`.
Each component gets its own file: `docs/<component>.md`.

```
docs/
  ├── index.md              ← Main entry point; links to every component doc
  ├── auth.md               ← Authentication & authorization documentation
  ├── data-model.md         ← Database schema and data model documentation
  ├── openspec.md           ← OpenSpec workflow documentation
  ├── ui/                   ← Frontend component documentation
  │   ├── index.md          ← UI overview and entry point
  │   ├── auth.md           ← OAuth2 flow, token management
  │   ├── api-service.md    ← API fetch service (services/api.js)
  │   └── layout.md         ← App layout, sidebar, message panel
  └── api-rs/               ← Backend component documentation
      ├── index.md          ← api-rs overview and entry point
      ├── categories.md     ← Category endpoints and service
      ├── messages.md       ← Message endpoints and service
      ├── security.md       ← Token introspection and role authorization
      └── oauth.md          ← OAuth2 token exchange proxy
```

**Folder rules:**
- All documentation for `ui` components MUST be placed under `docs/ui/`
- All documentation for `api-rs` components MUST be placed under `docs/api-rs/`
- Cross-cutting concerns (auth flow, data model, OpenSpec workflow) live directly under `docs/`
- Each subfolder MUST have its own `index.md` that links to the files within it and is linked from `docs/index.md`

### `docs/index.md` — main entry point

The index file must contain:
1. A one-paragraph project description (what the app does, who uses it)
2. A component table linking to each `<component>.md`
3. A "Getting started" section with the minimum commands to run the project locally

### `docs/<component>.md` — component files

Each component file must contain:
1. **Overview** — one paragraph describing what the component is and its responsibility
2. **Tech stack** — list of key technologies/frameworks used
3. **Structure** — key files/folders with one-line descriptions
4. **Configuration** — environment variables, config files, or runtime overrides
5. **Usage / API** — how to run, call, or interact with the component
6. **Samples** — at least one concrete code or command example per documented concept (see below)

### Writing style

- Use plain English; prefer short sentences
- Use present tense ("The service validates…", not "The service will validate…")
- Use `code blocks` for all commands, file paths, code snippets, and configuration values
- Use tables for reference material (endpoints, env vars, status codes)
- Avoid duplicating content that already exists in `AGENTS.md` or rule files — link to them instead

---

## Samples requirement

Every component documented in the project MUST include concrete samples.
Samples make abstract descriptions actionable.

### What counts as a sample

- A `curl` command demonstrating an API call
- A shell snippet showing how to start the component
- A code excerpt showing a pattern (e.g. how to add a new service method)
- A config block showing how to set an env var or override a property

### Sample format

Always wrap samples in a fenced code block with a language tag and a short caption above it:

```
**Example: create a category**
```bash
curl -X POST http://localhost:8080/categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Work","code":"WORK","description":"Work emails"}'
```
```

### Minimum samples per component

| Component | Required samples |
|---|---|
| `ui` | How to serve the UI locally; how to override `API_BASE` |
| `api-rs` | How to start with env vars; one `curl` per HTTP method group (GET, POST, PUT, DELETE) |
| `auth` | The full OAuth2 redirect-and-exchange flow as a step-by-step shell/browser walkthrough |
| `data-model` | The full DDL for each table |
| `openspec` | A complete propose → apply → archive command sequence |

