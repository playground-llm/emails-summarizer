# api-rs — Messages

The `message` feature slice provides CRUD endpoints for email messages. Each message belongs to a category identified by `categoryCode`.

---

## Overview

| Layer | Class | Responsibility |
|---|---|---|
| Controller | `MessageController` | HTTP mapping, status codes, request/response bodies |
| Service | `MessageService` | Business logic, FK validation, conflict detection |
| Repository | `MessageRepository` | DB access via Spring Data JPA |
| DTO | `MessageRequest` | Request body record |
| Entity | `Message` | JPA entity (`MESSAGE` table, UUID PK) |

---

## Endpoints

### `GET /messages?categoryCode={code}`

Returns all messages in the specified category. Requires `ROLE_READ`. The `categoryCode` query parameter is required — omitting it returns `400 Bad Request`.

**Example**
```bash
curl "http://localhost:8080/messages?categoryCode=INBOX" \
  -H "Authorization: Bearer <token>"
```

Response (`200 OK`):
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "title": "Welcome to Emails Summarizer",
    "body": "Thank you for trying Emails Summarizer.",
    "categoryCode": "INBOX"
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f01234567891",
    "title": "System Notification",
    "body": "Your account has been successfully set up.",
    "categoryCode": "INBOX"
  }
]
```

---

### `POST /messages`

Creates a new message in the specified category. Requires `ROLE_EDIT`. Returns `404 Not Found` if the `categoryCode` does not exist.

**Request body**
```json
{
  "title": "Project Update",
  "body": "Sprint 1 completed successfully.",
  "categoryCode": "WORK"
}
```

**Example**
```bash
curl -X POST http://localhost:8080/messages \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Project Update","body":"Sprint 1 completed.","categoryCode":"WORK"}'
```

Response (`201 Created`):
```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-012345678902",
  "title": "Project Update",
  "body": "Sprint 1 completed.",
  "categoryCode": "WORK"
}
```

Error — category not found (`404 Not Found`):
```json
{ "status": 404, "error": "Not Found", "message": "Category not found: INVALID" }
```

---

### `PUT /messages/{id}`

Updates the `title` and `body` of an existing message. The `categoryCode` is not updatable. Requires `ROLE_EDIT`.

**Example**
```bash
curl -X PUT "http://localhost:8080/messages/c3d4e5f6-a7b8-9012-cdef-012345678902" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Project Update — Final","body":"Sprint 1 completed and reviewed.","categoryCode":"WORK"}'
```

Response (`200 OK`):
```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-012345678902",
  "title": "Project Update — Final",
  "body": "Sprint 1 completed and reviewed.",
  "categoryCode": "WORK"
}
```

Error — message not found (`404 Not Found`):
```json
{ "status": 404, "error": "Not Found", "message": "Message not found: c3d4e5f6-..." }
```

---

### `DELETE /messages/{id}`

Deletes a message by UUID. Requires `ROLE_DEL`.

**Example**
```bash
curl -X DELETE "http://localhost:8080/messages/c3d4e5f6-a7b8-9012-cdef-012345678902" \
  -H "Authorization: Bearer <token>"
# → 204 No Content
```

Error — not found (`404 Not Found`):
```json
{ "status": 404, "error": "Not Found", "message": "Message not found: c3d4e5f6-..." }
```

---

## Business Rules

| Rule | Behaviour |
|---|---|
| Missing `categoryCode` query param on GET | `400 Bad Request` |
| Unknown `categoryCode` on POST | `404 Not Found` |
| Message UUID not found on PUT/DELETE | `404 Not Found` |
| `categoryCode` on PUT | Ignored — messages cannot be moved between categories |

---

## Adding a New Service Method

**Example: add a method to count messages per category**
```java
// MessageRepository.java
long countByCategoryCode(String categoryCode);

// MessageService.java
@Transactional(readOnly = true)
public long countByCategoryCode(String categoryCode) {
    return messageRepository.countByCategoryCode(categoryCode);
}

// MessageController.java
@GetMapping("/count")
public Map<String, Long> countMessages(@RequestParam String categoryCode) {
    return Map.of("count", messageService.countByCategoryCode(categoryCode));
}
```
