## ADDED Requirements

### Requirement: GET endpoints require ROLE_READ
The system SHALL restrict `GET /categories` and `GET /messages` to users who hold `ROLE_READ`. Authenticated users without `ROLE_READ` SHALL receive `403 Forbidden`. Unauthenticated users SHALL receive `401 Unauthorized`.

#### Scenario: ROLE_READ user reads categories
- **WHEN** a user with a valid GitHub token and `ROLE_READ` calls `GET /categories`
- **THEN** the API SHALL respond with `200 OK` and the list of categories

#### Scenario: ROLE_READ user reads messages
- **WHEN** a user with a valid GitHub token and `ROLE_READ` calls `GET /messages?categoryCode=INBOX`
- **THEN** the API SHALL respond with `200 OK` and the list of messages

#### Scenario: Authenticated user without ROLE_READ is denied on GET
- **WHEN** a user with a valid GitHub token but without `ROLE_READ` calls `GET /categories`
- **THEN** the API SHALL respond with `403 Forbidden`

#### Scenario: Unauthenticated request to GET endpoint is rejected
- **WHEN** a request to `GET /categories` is made with no `Authorization` header
- **THEN** the API SHALL respond with `401 Unauthorized`

---

### Requirement: POST and PUT endpoints require ROLE_EDIT
The system SHALL restrict `POST /categories`, `POST /messages`, `PUT /categories/{code}`, and `PUT /messages/{id}` to users who hold `ROLE_EDIT`. Authenticated users without `ROLE_EDIT` SHALL receive `403 Forbidden`. Unauthenticated users SHALL receive `401 Unauthorized`.

#### Scenario: ROLE_EDIT user creates a category
- **WHEN** a user with a valid GitHub token and `ROLE_EDIT` calls `POST /categories` with a valid body
- **THEN** the API SHALL process the request and NOT return `403`

#### Scenario: Authenticated user without ROLE_EDIT is denied on POST
- **WHEN** a user with a valid GitHub token but without `ROLE_EDIT` calls `POST /categories`
- **THEN** the API SHALL respond with `403 Forbidden`

#### Scenario: ROLE_EDIT user updates a message
- **WHEN** a user with a valid GitHub token and `ROLE_EDIT` calls `PUT /messages/{id}` with a valid body
- **THEN** the API SHALL process the request and NOT return `403`

#### Scenario: Authenticated user without ROLE_EDIT is denied on PUT
- **WHEN** a user with a valid GitHub token but without `ROLE_EDIT` calls `PUT /categories/{code}`
- **THEN** the API SHALL respond with `403 Forbidden`

#### Scenario: Unauthenticated request to POST endpoint is rejected
- **WHEN** a request to `POST /messages` is made with no `Authorization` header
- **THEN** the API SHALL respond with `401 Unauthorized`

---

### Requirement: DELETE endpoints require ROLE_DEL
The system SHALL restrict `DELETE /categories/{code}` and `DELETE /messages/{id}` to users who hold `ROLE_DEL`. Authenticated users without `ROLE_DEL` SHALL receive `403 Forbidden`. Unauthenticated users SHALL receive `401 Unauthorized`.

#### Scenario: ROLE_DEL user deletes a category
- **WHEN** a user with a valid GitHub token and `ROLE_DEL` calls `DELETE /categories/{code}`
- **THEN** the API SHALL process the request and NOT return `403`

#### Scenario: Authenticated user without ROLE_DEL is denied on DELETE
- **WHEN** a user with a valid GitHub token but without `ROLE_DEL` calls `DELETE /messages/{id}`
- **THEN** the API SHALL respond with `403 Forbidden`

#### Scenario: Unauthenticated request to DELETE endpoint is rejected
- **WHEN** a request to `DELETE /categories/{code}` is made with no `Authorization` header
- **THEN** the API SHALL respond with `401 Unauthorized`

---

### Requirement: Role allow-lists configured independently via environment variables
The system SHALL assign `ROLE_READ`, `ROLE_EDIT`, and `ROLE_DEL` based on three separate comma-separated env vars: `READERS_GITHUB_LOGINS`, `EDITORS_GITHUB_LOGINS`, and `DELETERS_GITHUB_LOGINS` respectively. A GitHub login MAY appear in multiple lists to hold multiple roles. Matching SHALL be case-insensitive. An absent or empty env var means no user holds that role.

#### Scenario: User with ROLE_READ only cannot write
- **WHEN** `READERS_GITHUB_LOGINS=alice` and `EDITORS_GITHUB_LOGINS=` are set, and `alice` calls `POST /categories`
- **THEN** the API SHALL respond with `403 Forbidden`

#### Scenario: User holding multiple roles
- **WHEN** `READERS_GITHUB_LOGINS=alice` and `EDITORS_GITHUB_LOGINS=alice` and `DELETERS_GITHUB_LOGINS=alice` are set
- **THEN** `alice` SHALL be granted `ROLE_READ`, `ROLE_EDIT`, and `ROLE_DEL`

#### Scenario: Case-insensitive match for readers
- **WHEN** `READERS_GITHUB_LOGINS=Alice` is configured and the introspected login is `alice`
- **THEN** the API SHALL recognise the user as holding `ROLE_READ`

#### Scenario: All lists empty blocks all data endpoints
- **WHEN** all three env vars are unset and any authenticated user calls any data endpoint
- **THEN** the API SHALL respond with `403 Forbidden`
