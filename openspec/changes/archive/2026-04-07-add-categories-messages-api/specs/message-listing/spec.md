## ADDED Requirements

### Requirement: List messages by category code
The API SHALL expose a `GET /messages` endpoint that accepts a `categoryCode` query parameter and returns all messages belonging to that category. Each message object SHALL include `id` (UUID), `title` (string), and `body` (text) fields.

#### Scenario: Successful retrieval of messages for a valid category code
- **WHEN** an authenticated client sends `GET /messages?categoryCode={code}` with a valid Bearer token and a category code that exists
- **THEN** the API SHALL respond with HTTP 200 and a JSON array of message objects each containing `id`, `title`, and `body`

#### Scenario: No messages found for a valid category code
- **WHEN** an authenticated client sends `GET /messages?categoryCode={code}` and the category exists but has no messages
- **THEN** the API SHALL respond with HTTP 200 and an empty JSON array `[]`

#### Scenario: Missing categoryCode parameter
- **WHEN** an authenticated client sends `GET /messages` without the `categoryCode` query parameter
- **THEN** the API SHALL respond with HTTP 400 Bad Request

#### Scenario: Message id is a UUID
- **WHEN** the API returns a message object
- **THEN** the `id` field SHALL be a valid UUID string (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

#### Scenario: Unauthenticated request is rejected
- **WHEN** a client sends `GET /messages?categoryCode={code}` without a Bearer token
- **THEN** the API SHALL respond with HTTP 401 Unauthorized
