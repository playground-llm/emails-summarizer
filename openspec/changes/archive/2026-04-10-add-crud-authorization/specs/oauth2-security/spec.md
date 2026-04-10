## MODIFIED Requirements

### Requirement: Resource server validates GitHub Bearer tokens via user-info introspection
All protected API endpoints on `api-rs` SHALL require a valid GitHub access token supplied as a Bearer token. The resource server SHALL validate each token by calling `GET https://api.github.com/user` with the token; a 200 response confirms validity and the GitHub `login` is used as the principal name. After successful introspection, the principal SHALL be assigned granted authorities from the `ROLES` table via `UserService.findOrRegister`. A login absent from the `USERS` table is registered automatically with `ROLE_READ`.

#### Scenario: Valid GitHub token grants access
- **WHEN** a client sends a request to a protected endpoint with a valid GitHub Bearer token in the `Authorization` header
- **THEN** `api-rs` SHALL call `GET https://api.github.com/user` with the token, receive HTTP 200, and allow the request to proceed

#### Scenario: Invalid or expired GitHub token is rejected
- **WHEN** a client sends a request with a revoked, expired, or malformed GitHub token
- **THEN** `api-rs` SHALL receive a non-200 response from GitHub user-info, and respond with HTTP 401 Unauthorized to the client

#### Scenario: Missing token results in 401
- **WHEN** a client sends a request to a protected endpoint with no `Authorization` header
- **THEN** `api-rs` SHALL respond with HTTP 401 Unauthorized without calling the GitHub user-info endpoint

#### Scenario: New user gets ROLE_READ on first login
- **GIVEN** the GitHub login `alice` does not exist in `USERS`
- **WHEN** `alice` presents a valid token
- **THEN** the principal SHALL carry `ROLE_READ` and rows SHALL be inserted into `USERS` and `ROLES`

#### Scenario: Returning user gets roles from database
- **GIVEN** `alice` has `ROLE_READ` and `ROLE_EDIT` in the `ROLES` table
- **WHEN** `alice` presents a valid token
- **THEN** the principal SHALL carry `ROLE_READ` and `ROLE_EDIT`

#### Scenario: Login absent from all role rows carries no data-access authorities
- **GIVEN** `alice` exists in `USERS` but has no rows in `ROLES`
- **WHEN** `alice` presents a valid token
- **THEN** the resulting principal SHALL carry no `ROLE_READ`, `ROLE_EDIT`, or `ROLE_DEL` authority and SHALL receive `403 Forbidden` on any data endpoint
