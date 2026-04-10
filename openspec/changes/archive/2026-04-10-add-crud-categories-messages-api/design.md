## Context

`api-rs` currently exposes read-only endpoints (`GET /categories`, `GET /messages`). The data model is already normalised — `CATEGORY(id, name, code, description)` and `MESSAGE(id UUID, title, body, category_code FK)`. The Spring Boot layer uses JPA repositories, so adding write operations is a controller + optional DTO concern, not a schema change.

All endpoints are protected by GitHub opaque-token introspection. Write endpoints will follow the same security model — any authenticated user can write.

## Goals / Non-Goals

**Goals:**
- Add `POST`, `PUT`, `DELETE` endpoints for categories and messages
- Return appropriate HTTP status codes (201 Created, 204 No Content, 404 Not Found, 409 Conflict)
- Handle the FK constraint when deleting a category that still has messages
- Extend CORS allowed methods to include `PUT` and `DELETE`

**Non-Goals:**
- No UI changes in this change
- No ownership model — no per-user data isolation
- No PATCH / partial update
- No soft delete

## Decisions

### 1. Request bodies as separate DTO records (not entity classes)

**Decision**: Introduce `CategoryRequest` and `MessageRequest` Java records as request bodies, keeping JPA entities as the persistence model.

**Why**: Exposing JPA entities directly as request bodies lets callers set `id`, FK fields, and other internal state. A thin DTO decouples what the API accepts from what the DB stores. Implementation is trivial — two one-file records.

**Alternative considered**: Use entity directly with `@JsonIgnore` on `id`. Rejected because it leaks the persistence model and is error-prone.

### 2. Category delete: 409 when messages exist, no cascade

**Decision**: If a `DELETE /categories/{code}` is called and the category still has messages, return `409 Conflict` with a clear message. Do not cascade-delete messages.

**Why**: Silently deleting all messages in a category is destructive and surprising. The caller should explicitly delete messages first. This is also safer given H2 FK constraints are enforced (`CONSTRAINT fk_message_category`).

**Alternative considered**: ON DELETE CASCADE in schema. Rejected — too destructive for a default; can be added later.

### 3. PUT replaces mutable fields only

**Decision**: `PUT /categories/{code}` accepts `{name, description}`. The `code` field is immutable — it is the resource identifier and a FK target; renaming it would require cascading updates. Similarly `PUT /messages/{id}` accepts `{title, body, categoryCode}`.

**Why**: Changing `code` would break FK references and require a two-step update (create new category, re-assign messages, delete old). Out of scope.

### 4. No new Spring dependencies

All required building blocks (`@PutMapping`, `@DeleteMapping`, `@PostMapping`, `ResponseEntity`, `@RequestBody`, `@PathVariable`) are already available via `spring-boot-starter-web`.

## Risks / Trade-offs

- **H2 FK enforcement in tests** → Since H2 enforces FK constraints, delete-with-messages correctly returns a DB error we can translate to 409. In production databases this would need an explicit check or a `@Transactional` guard. For this scope (H2 dev-only) the FK error catch is acceptable.
- **No optimistic locking** → Concurrent PUTs can silently overwrite each other. Acceptable for a single-user dev app; can be addressed with `@Version` later.
- **CORS** → Forgetting to add `PUT`/`DELETE` to `setAllowedMethods` would break the UI silently. Noted as an explicit task.
