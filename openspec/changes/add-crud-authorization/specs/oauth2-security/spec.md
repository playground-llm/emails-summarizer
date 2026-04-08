## MODIFIED Requirements

### Requirement: Resource server validates GitHub Bearer tokens via user-info introspection
All protected API endpoints on `api-rs` SHALL require a valid GitHub access token supplied as a Bearer token. The resource server SHALL validate each token by calling `GET https://api.github.com/user` with the token; a 200 response confirms validity and the GitHub `login` is used as the principal name. After successful introspection, the principal SHALL be assigned granted authorities based on three independent allow-lists: `ROLE_READ` if the login is in the readers list, `ROLE_EDIT` if in the editors list, and `ROLE_DEL` if in the deleters list. A login MAY appear in multiple lists.

#### Scenario: Valid GitHub token grants access
- **WHEN** a client sends a request to a protected endpoint with a valid GitHub Bearer token in the `Authorization` header
- **THEN** `api-rs` SHALL call `GET https://api.github.com/user` with the token, receive HTTP 200, and allow the request to proceed

#### Scenario: Invalid or expired GitHub token is rejected
- **WHEN** a client sends a request with a revoked, expired, or malformed GitHub token
- **THEN** `api-rs` SHALL receive a non-200 response from GitHub user-info, and respond with HTTP 401 Unauthorized to the client

#### Scenario: Missing token results in 401
- **WHEN** a client sends a request to a protected endpoint with no `Authorization` header
- **THEN** `api-rs` SHALL respond with HTTP 401 Unauthorized without calling the GitHub user-info endpoint

#### Scenario: Login in readers list grants ROLE_READ
- **WHEN** the introspected GitHub `login` is present in the `READERS_GITHUB_LOGINS` allow-list
- **THEN** the resulting principal SHALL carry `ROLE_READ`

#### Scenario: Login in editors list grants ROLE_EDIT
- **WHEN** the introspected GitHub `login` is present in the `EDITORS_GITHUB_LOGINS` allow-list
- **THEN** the resulting principal SHALL carry `ROLE_EDIT`

#### Scenario: Login in deleters list grants ROLE_DEL
- **WHEN** the introspected GitHub `login` is present in the `DELETERS_GITHUB_LOGINS` allow-list
- **THEN** the resulting principal SHALL carry `ROLE_DEL`

#### Scenario: Login absent from all lists carries no data-access authorities
- **WHEN** the introspected GitHub `login` is absent from all three allow-lists
- **THEN** the resulting principal SHALL carry no `ROLE_READ`, `ROLE_EDIT`, or `ROLE_DEL` authority and SHALL receive `403 Forbidden` on any data endpoint
