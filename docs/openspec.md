# OpenSpec Workflow

This document describes the spec-driven development workflow used in emails-summarizer.

---

## Overview

All non-trivial feature work follows the **OpenSpec** workflow: Explore → Propose → Apply → Archive. Changes are tracked as structured proposals in `openspec/changes/`. Completed changes are archived in `openspec/changes/archive/`, and their capability specs are promoted to `openspec/specs/`.

This workflow keeps intent, design decisions, and implementation tasks co-located so any contributor (human or AI agent) can understand why something was built and how to continue the work.

---

## Directory Structure

```
openspec/
├── config.yaml          ← Project context and workflow rules for AI agents
├── changes/             ← Active change proposals
│   ├── <change-name>/   ← One folder per active change
│   │   ├── .openspec.yaml        ← Change metadata (id, status, created)
│   │   ├── proposal.md           ← What and why (goals, non-goals, capabilities)
│   │   ├── design.md             ← How (architectural decisions and rationale)
│   │   ├── tasks.md              ← Ordered implementation tasks
│   │   └── specs/                ← Capability specs (one per capability)
│   │       └── <capability>/
│   │           └── spec.md       ← Requirements in Given/When/Then format
│   └── archive/         ← Completed changes (moved here after archiving)
└── specs/               ← Promoted capability specs (source of truth after archiving)
    └── <capability>/
        └── spec.md
```

---

## Stages

### 1. Explore

Use explore mode to think through a problem before committing to a proposal. This stage is optional but recommended for unclear or ambiguous requirements.

```bash
# Invoke via skill
/opsx:explore
# or
/openspec-explore
```

Explore mode is a thinking partner — it helps clarify goals, surface constraints, and identify edge cases before writing specs.

---

### 2. Propose

Create a full change proposal with design, specs, and tasks in one step.

```bash
/opsx:propose
# or
/openspec-propose
```

The agent generates:
- `proposal.md` — what the change does, why, and explicit non-goals
- `design.md` — architectural decisions and rationale
- `specs/<capability>/spec.md` — normative requirements with Given/When/Then scenarios
- `tasks.md` — ordered implementation tasks (~1–2 hours each)

**Example proposal structure** (`proposal.md`):
```markdown
## Goal
Add CRUD endpoints for categories and messages.

## Capabilities
- `category-management` — create, update, delete categories
- `message-management` — create, update, delete messages

## Non-Goals
- Bulk operations
- Soft deletes
```

**Example spec requirement** (`specs/category-management/spec.md`):
```markdown
## REQ-001: Create category

The API SHALL accept a POST request to `/categories` and persist the new category.

#### Scenario: successful creation
Given a valid `CategoryRequest` with name "Work" and code "WORK"
When POST /categories is called with a valid ROLE_EDIT token
Then the response status is 201
And the response body contains the created category with id, name, code, description
```

---

### 3. Apply

Implement the tasks from an active change proposal.

```bash
/opsx:apply
# or
/openspec-apply-change
```

The agent reads `tasks.md` and implements each task in order, following specs in `specs/`. Each task is marked as done when complete.

**Example tasks.md structure**:
```markdown
## Task 1: Add CategoryRequest DTO
- Create `CategoryRequest.java` as a Java record with `name`, `code`, `description` fields.

## Task 2: Implement CategoryService CRUD
- Add `create(CategoryRequest)`, `update(String code, CategoryRequest)`, `delete(String code)` methods.
- `create` throws 409 if code already exists.
- `delete` throws 409 if category has messages, 404 if not found.

## Task 3: Verification
- [ ] POST /categories creates a category and returns 201
- [ ] DELETE /categories/INBOX returns 409 when messages exist
```

---

### 4. Archive

Mark a completed change as done and promote its specs.

```bash
/opsx:archive
# or
/openspec-archive-change
```

This moves the change folder from `openspec/changes/` to `openspec/changes/archive/` and promotes the capability specs to `openspec/specs/`.

---

## Complete Workflow Example

```bash
# 1. Explore the problem (optional)
/opsx:explore
# → discuss requirements, surface constraints

# 2. Propose the change
/opsx:propose
# → generates proposal.md, design.md, specs/, tasks.md

# 3. Implement
/opsx:apply
# → works through tasks.md step by step

# 4. Archive when done
/opsx:archive
# → moves to openspec/changes/archive/
# → promotes specs to openspec/specs/
```

---

## Active Changes

| Change | Status | Description |
|---|---|---|
| `add-crud-categories-messages-api` | Implemented | CRUD endpoints for categories and messages |
| `add-crud-authorization` | Proposed | Three-role authorization (ROLE_READ/EDIT/DEL) |

## Archived Changes

| Change | Description |
|---|---|
| `2026-04-07-add-categories-messages-api` | Spring Boot API with Category/Message GET endpoints and GitHub OAuth2 security |
| `2026-04-07-add-ui-main-page` | Vue 3 SPA with OAuth2 login, category sidebar, and message panel |

---

## Spec Writing Rules

- Use `SHALL`/`MUST` for normative requirements; avoid `should` or `may`
- Every requirement must have at least one testable scenario in `Given/When/Then` format
- Scenarios use exactly 4 hashtags (`####`) as headings
- Keep proposals concise (1–2 pages maximum)
- Always include a "Non-Goals" section so scope is explicit
- Focus `design.md` on *why* (architectural decisions and rationale), not line-by-line implementation
