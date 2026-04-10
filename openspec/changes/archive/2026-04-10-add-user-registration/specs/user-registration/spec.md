## Requirement: GitHub users are automatically registered on first login

On the first successful token introspection for a given GitHub login, the system SHALL create a row in the `USERS` table containing the user's `login`, `github_id`, `name`, and `avatar_url`, and a corresponding row in the `ROLES` table granting `ROLE_READ`. Subsequent introspections for the same login SHALL NOT insert additional rows.

#### Scenario: New user is registered with ROLE_READ
- **GIVEN** no row exists in `USERS` with login `alice`
- **WHEN** `alice` presents a valid GitHub access token to any protected endpoint
- **THEN** a row SHALL be inserted into `USERS` with `login = 'alice'`
- **AND** a row SHALL be inserted into `ROLES` with `login = 'alice'` and `role = 'ROLE_READ'`
- **AND** the returned principal SHALL carry `ROLE_READ`

#### Scenario: Returning user is not re-registered
- **GIVEN** a row already exists in `USERS` with login `alice`
- **WHEN** `alice` presents a valid GitHub access token
- **THEN** no new row SHALL be inserted into `USERS` or `ROLES`
- **AND** the principal SHALL carry the roles currently in the `ROLES` table for `alice`

#### Scenario: Returning user with multiple roles gets all of them
- **GIVEN** `alice` has `ROLE_READ` and `ROLE_EDIT` in the `ROLES` table
- **WHEN** `alice` presents a valid GitHub access token
- **THEN** the principal SHALL carry both `ROLE_READ` and `ROLE_EDIT`

---

## Requirement: User profile data is captured at registration time

The system SHALL store the GitHub `login`, numeric `github_id`, display `name`, and `avatar_url` in the `USERS` table. Fields `github_id`, `name`, and `avatar_url` MAY be `null` if GitHub does not return them.

#### Scenario: Profile fields populated from GitHub user-info response
- **GIVEN** GitHub's user-info endpoint returns `login`, `id`, `name`, and `avatar_url`
- **WHEN** a new user is registered
- **THEN** all four fields SHALL be persisted in the `USERS` row

#### Scenario: Null profile fields tolerated
- **GIVEN** GitHub's user-info endpoint returns `login` and `id` but no `name` or `avatar_url`
- **WHEN** a new user is registered
- **THEN** the `USERS` row SHALL be created with `name` and `avatar_url` as `NULL` without error

---

## Requirement: USERS and ROLES tables initialised by schema.sql

The `USERS` and `ROLES` DDL SHALL be present in `api-rs/src/main/resources/schema.sql` and executed at startup via Spring's SQL init mechanism. The `ROLES` table SHALL have a foreign key from `login` to `USERS.login` and a unique constraint on `(login, role)`.

#### Scenario: Schema created on startup
- **GIVEN** the application starts with an empty H2 in-memory database
- **WHEN** the datasource is initialised
- **THEN** the `USERS` and `ROLES` tables SHALL exist and be queryable

#### Scenario: Duplicate role row rejected by constraint
- **GIVEN** `ROLES` already has `(login='alice', role='ROLE_READ')`
- **WHEN** an attempt is made to insert a second `(login='alice', role='ROLE_READ')` row
- **THEN** the database SHALL reject the insert with a constraint violation
