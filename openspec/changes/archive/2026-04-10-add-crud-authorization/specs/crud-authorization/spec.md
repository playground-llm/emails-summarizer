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

### Requirement: Roles stored in the database and assigned at introspection time
The system SHALL assign `ROLE_READ`, `ROLE_EDIT`, and `ROLE_DEL` to an authenticated principal based on rows in the `ROLES` table. The introspector SHALL call `UserService.findOrRegister` to retrieve role strings after resolving the GitHub login. A user MAY hold multiple roles as separate rows. Role lookup SHALL use the lowercase form of the GitHub login. An absent login results in automatic registration with `ROLE_READ` only.

#### Scenario: New user gets ROLE_READ on first login
- **GIVEN** a GitHub user `alice` has never authenticated before
- **WHEN** `alice` presents a valid token to any endpoint
- **THEN** the system SHALL create a row in `USERS` and a row in `ROLES(login='alice', role='ROLE_READ')`, and the principal SHALL carry `ROLE_READ`

#### Scenario: Returning user gets their stored roles
- **GIVEN** `alice` already has rows `ROLE_READ` and `ROLE_EDIT` in the `ROLES` table
- **WHEN** `alice` presents a valid token
- **THEN** the principal SHALL carry `ROLE_READ` and `ROLE_EDIT` without inserting new rows

#### Scenario: User with ROLE_READ only cannot write
- **GIVEN** `alice` has only `ROLE_READ` in the `ROLES` table
- **WHEN** `alice` calls `POST /categories`
- **THEN** the API SHALL respond with `403 Forbidden`

#### Scenario: User holding multiple roles
- **GIVEN** `alice` has `ROLE_READ`, `ROLE_EDIT`, and `ROLE_DEL` in the `ROLES` table
- **THEN** `alice` SHALL be granted all three authorities and may call GET, POST/PUT, and DELETE endpoints

#### Scenario: Case-insensitive login matching
- **GIVEN** `USERS` contains a row with login `alice`
- **WHEN** the introspected GitHub login value is `Alice`
- **THEN** the system SHALL normalise to `alice` and match the existing row

#### Scenario: All lists empty blocks all data endpoints
- **WHEN** all three env vars are unset and any authenticated user calls any data endpoint
- **THEN** the API SHALL respond with `403 Forbidden`
