## ADDED Requirements

### Requirement: Create a category
The API SHALL allow authenticated users to create a new category by providing a name, a unique code, and an optional description. The response SHALL be `201 Created` with the created category body.

#### Scenario: Successful creation
- **WHEN** an authenticated user sends `POST /categories` with `{"name":"Work","code":"WORK","description":"Work emails"}`
- **THEN** the API returns `201 Created` with the new category object including its generated `id`

#### Scenario: Duplicate code rejected
- **WHEN** an authenticated user sends `POST /categories` with a `code` that already exists
- **THEN** the API returns `409 Conflict`

#### Scenario: Unauthenticated request rejected
- **WHEN** a request is made to `POST /categories` without a Bearer token
- **THEN** the API returns `401 Unauthorized`

### Requirement: Update a category
The API SHALL allow authenticated users to update the `name` and `description` of an existing category identified by its `code`. The `code` itself SHALL be immutable. The response SHALL be `200 OK` with the updated category body.

#### Scenario: Successful update
- **WHEN** an authenticated user sends `PUT /categories/WORK` with `{"name":"Work Updated","description":"Updated desc"}`
- **THEN** the API returns `200 OK` with the updated category

#### Scenario: Category not found
- **WHEN** an authenticated user sends `PUT /categories/NONEXISTENT`
- **THEN** the API returns `404 Not Found`

### Requirement: Delete a category
The API SHALL allow authenticated users to delete a category by its `code`. The response SHALL be `204 No Content` on success. If the category still has associated messages, the API SHALL return `409 Conflict` and SHALL NOT delete the category.

#### Scenario: Successful deletion
- **WHEN** an authenticated user sends `DELETE /categories/EMPTY_CAT` and the category has no messages
- **THEN** the API returns `204 No Content` and the category no longer appears in `GET /categories`

#### Scenario: Deletion blocked by existing messages
- **WHEN** an authenticated user sends `DELETE /categories/INBOX` and the category has messages
- **THEN** the API returns `409 Conflict` and the category remains

#### Scenario: Category not found
- **WHEN** an authenticated user sends `DELETE /categories/NONEXISTENT`
- **THEN** the API returns `404 Not Found`
