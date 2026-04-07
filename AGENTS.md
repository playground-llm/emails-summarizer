# AGENTS.md

Guidance for AI agents working in this repository.

## Project overview

**emails-summarizer** is a Gradle multi-project monorepo. It is in early scaffolding stage — the tech stacks for both subprojects are not yet locked in.

```
emails-summarizer/
├── build.gradle          # Root: shared group/version/repos for all subprojects
├── settings.gradle       # Declares subprojects: ui, api-rs
├── gradle/wrapper/       # Gradle 8.13 wrapper
├── ui/                   # Frontend subproject (technology TBD)
│   └── build.gradle
├── api-rs/               # Backend API subproject (technology TBD)
│   └── build.gradle
└── openspec/             # Spec-driven development workflow
    ├── config.yaml       # OpenSpec project config
    ├── changes/          # Active change proposals
    └── specs/            # Capability specs
```

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
./gradlew tasks          # List available tasks
```

## Subprojects

### `ui` — Frontend
- Technology not yet decided.
- Config stub: `ui/build.gradle`.
- When a tech stack is chosen, update `ui/build.gradle` with the appropriate Gradle plugin and dependencies.

### `api-rs` — Backend API
- Technology not yet decided (name suggests Rust but not confirmed).
- Config stub: `api-rs/build.gradle`.
- When a tech stack is chosen, update `api-rs/build.gradle` accordingly.

## Development workflow (OpenSpec)

This project uses an OpenSpec spec-driven workflow. All feature work flows through:

1. **Explore** — understand the problem space (`/opsx explore` or `opsx:explore` skill)
2. **Propose** — draft a spec + tasks for a change (`/opsx propose` or `opsx:propose` skill)
3. **Apply** — implement tasks from a change (`/opsx apply` or `opsx:apply` skill)
4. **Archive** — mark a completed change as done (`/opsx archive` or `opsx:archive` skill)

Change proposals live in `openspec/changes/` while active; completed ones move to `openspec/changes/archive/`.
Capability specs live in `openspec/specs/`.

Project-level context (tech stack, conventions) can be added to `openspec/config.yaml` under the `context:` key — do this as technology choices are made.

## Key conventions

- Use `./gradlew` (never bare `gradle`).
- Don't add shared dependencies directly in subproject build files if they belong in the root `build.gradle` `allprojects`/`subprojects` block.
- Follow the OpenSpec workflow for all non-trivial changes — don't skip straight to implementation without a proposal.
- Commit messages should be descriptive; the initial commit style is plain prose.
