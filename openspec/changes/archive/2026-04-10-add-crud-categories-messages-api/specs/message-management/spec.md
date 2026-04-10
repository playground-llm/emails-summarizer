## ADDED Requirements

### Requirement: Create a message
The API SHALL allow authenticated users to create a new message by providing a title, body, and a valid `categoryCode`. The response SHALL be `201 Created` with the created message body including the generated UUID `id`.

#### Scenario: Successful creation
- **WHEN** an authenticated user sends `POST /messages` with `{"title":"Hello","body":"World","categoryCode":"INBOX"}`
- **THEN** the API returns `201 Created` with the new message object including its `id` (UUID)

#### Scenario: Unknown category code rejected
- **WHEN** an authenticated user sends `POST /messages` with a `categoryCode` that does not exist
- **THEN** the API returns `409 Conflict`

#### Scenario: Unauthenticated request rejected
- **WHEN** a request is made to `POST /messages` without a Bearer token
- **THEN** the API returns `401 Unauthorized`

### Requirement: Update a message
The API SHALL allow authenticated users to update the `title`, `body`, and `categoryCode` of an existing message identified by its UUID `id`. The response SHALL be `200 OK` with the updated message body.

#### Scenario: Successful update
- **WHEN** an authenticated user sends `PUT /messages/{id}` with `{"title":"Updated","body":"New body","categoryCode":"WORK"}`
- **THEN** the API returns `200 OK` with the updated message

#### Scenario: Message not found
- **WHEN** an authenticated user sends `PUT /messages/{id}` with a UUID that does not exist
- **THEN** the API returns `404 Not Found`

#### Scenario: Unknown category code rejected
- **WHEN** an authenticated user sends `PUT /messages/{id}` with a `categoryCode` that does not exist
- **THEN** the API returns `409 Conflict`

### Requirement: Delete a message
The API SHALL allow authenticated users to delete a message by its UUID `id`. The response SHALL be `204 No Content` on success.

#### Scenario: Successful deletion
- **WHEN** an authenticated user sends `DELETE /messages/{id}` with an existing UUID
- **THEN** the API returns `204 No Content` and the message no longer appears in `GET /messages?categoryCode=...`

#### Scenario: Message not found
- **WHEN** an authenticated user sends `DELETE /messages/{id}` with a UUID that does not exist
- **THEN** the API returns `404 Not Found`
