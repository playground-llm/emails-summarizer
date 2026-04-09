# api-rs — Categories

The `category` feature slice provides CRUD endpoints for email categories. Categories are the top-level organisational unit — messages belong to a category via `category_code`.

---

## Overview

| Layer | Class | Responsibility |
|---|---|---|
| Controller | `CategoryController` | HTTP mapping, status codes, request/response bodies |
| Service | `CategoryService` | Business logic, validation, conflict detection |
| Repository | `CategoryRepository` | DB access via Spring Data JPA |
| DTO | `CategoryRequest` | Request body record |
| Entity | `Category` | JPA entity (`CATEGORY` table) |

---

## Endpoints

### `GET /categories`

Returns all categories in insertion order. Requires `ROLE_READ`.

**Example**
```bash
curl http://localhost:8080/categories \
  -H "Authorization: Bearer <token>"
```

Response (`200 OK`):
```json
[
  { "id": 1, "name": "Inbox",    "code": "INBOX",    "description": "General incoming messages" },
  { "id": 2, "name": "Work",     "code": "WORK",     "description": "Work-related emails" },
  { "id": 3, "name": "Personal", "code": "PERSONAL", "description": "Personal correspondence" }
]
```

---

### `POST /categories`

Creates a new category. Requires `ROLE_EDIT`.

**Request body**
```json
{ "name": "Archive", "code": "ARCHIVE", "description": "Archived emails" }
```

**Example**
```bash
curl -X POST http://localhost:8080/categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Archive","code":"ARCHIVE","description":"Archived emails"}'
```

Response (`201 Created`):
```json
{ "id": 4, "name": "Archive", "code": "ARCHIVE", "description": "Archived emails" }
```

Error — duplicate code (`409 Conflict`):
```json
{ "status": 409, "error": "Conflict", "message": "Category code already exists: ARCHIVE" }
```

---

### `PUT /categories/{code}`

Updates the `name` and `description` of an existing category. The `code` is immutable and is used only as the URL path parameter. Requires `ROLE_EDIT`.

**Example**
```bash
curl -X PUT http://localhost:8080/categories/ARCHIVE \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Old Archive","code":"ARCHIVE","description":"Old archived emails"}'
```

Response (`200 OK`):
```json
{ "id": 4, "name": "Old Archive", "code": "ARCHIVE", "description": "Old archived emails" }
```

Error — not found (`404 Not Found`):
```json
{ "status": 404, "error": "Not Found", "message": "Category not found: ARCHIVE" }
```

---

### `DELETE /categories/{code}`

Deletes a category by code. Requires `ROLE_DEL`. Returns `409 Conflict` if the category still has messages — delete the messages first.

**Example**
```bash
curl -X DELETE http://localhost:8080/categories/ARCHIVE \
  -H "Authorization: Bearer <token>"
# → 204 No Content
```

Error — category has messages (`409 Conflict`):
```bash
curl -X DELETE http://localhost:8080/categories/INBOX \
  -H "Authorization: Bearer <token>"
# → 409 Conflict: "Cannot delete category with existing messages: INBOX"
```

**Full delete flow: delete messages first, then the category**
```bash
# 1. List messages in the category
curl "http://localhost:8080/messages?categoryCode=INBOX" \
  -H "Authorization: Bearer <token>"

# 2. Delete each message by UUID
curl -X DELETE "http://localhost:8080/messages/<uuid>" \
  -H "Authorization: Bearer <token>"

# 3. Now delete the category
curl -X DELETE http://localhost:8080/categories/INBOX \
  -H "Authorization: Bearer <token>"
# → 204 No Content
```

---

## Business Rules

| Rule | Behaviour |
|---|---|
| Duplicate `code` on create | `409 Conflict` |
| Category not found on update/delete | `404 Not Found` |
| Category has messages on delete | `409 Conflict` |
| `code` is immutable | `PUT` only updates `name` and `description` |

---

## Adding a New Service Method

Follow the layering rule: put all logic in `CategoryService`, keep `CategoryController` thin.

**Example: add a `findByCode` lookup method**
```java
// CategoryRepository.java
Optional<Category> findByCode(String code); // already exists

// CategoryService.java
@Transactional(readOnly = true)
public Category getByCode(String code) {
    return categoryRepository.findByCode(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Category not found: " + code));
}

// CategoryController.java
@GetMapping("/{code}")
public Category getCategory(@PathVariable String code) {
    return categoryService.getByCode(code);
}
```
