## Why

The existing API only supports read operations — users can browse categories and messages but cannot create or update them. Adding write capabilities to `api-rs` makes the application useful beyond a static demo.

## What Changes

- `POST /categories` — create a new category
- `PUT /categories/{code}` — update an existing category's name and description
- `DELETE /categories/{code}` — delete a category (and optionally its messages)
- `POST /messages` — create a new message in a category
- `PUT /messages/{id}` — update a message's title and/or body
- `DELETE /messages/{id}` — delete a message
- All write endpoints require a valid GitHub Bearer token (same as read endpoints)

## Non-Goals

- UI changes — no frontend work in this change; write endpoints are API-only
- Bulk operations — no batch create/delete
- Partial updates (PATCH) — PUT replaces the mutable fields
- Soft delete / trash / archive — deletions are permanent
- Authorization beyond authentication — all authenticated users can write

## Capabilities

### New Capabilities

- `category-management`: Create, update, and delete categories via REST endpoints
- `message-management`: Create, update, and delete messages via REST endpoints

### Modified Capabilities

- `category-listing`: Response shape unchanged; no requirement changes
- `message-listing`: Response shape unchanged; no requirement changes

## Impact

- `api-rs/src/main/java/com/emailssummarizer/apirs/category/` — extend `CategoryController`, possibly add request DTOs
- `api-rs/src/main/java/com/emailssummarizer/apirs/message/` — extend `MessageController`, possibly add request DTOs
- `ResourceServerConfig` — CORS `setAllowedMethods` must include `PUT` and `DELETE`
- `schema.sql` / FK constraint — deleting a category with messages must be handled (cascade or 409 response)
- No new dependencies required
