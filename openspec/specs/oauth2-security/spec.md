## ADDED Requirements

### Requirement: GitHub OAuth2 Authorization Code flow
The application SHALL use GitHub as the OAuth2 identity provider via a registered GitHub OAuth App named "emails-summarizer". The Authorization Code flow SHALL be used to authenticate users; the resulting GitHub access token is issued by GitHub and used by clients to call protected `api-rs` endpoints.

#### Scenario: Client obtains a GitHub access token via Authorization Code flow
- **WHEN** a client completes the GitHub Authorization Code flow with valid GitHub credentials
- **THEN** GitHub SHALL issue an opaque access token that can be used as a Bearer token on `api-rs` endpoints

#### Scenario: Unregistered GitHub OAuth App is rejected
- **WHEN** a client initiates the Authorization Code flow with an incorrect `client_id`
- **THEN** GitHub SHALL reject the request with an OAuth2 error response

---

### Requirement: Resource server validates GitHub Bearer tokens via user-info introspection
All protected API endpoints on `api-rs` SHALL require a valid GitHub access token supplied as a Bearer token. The resource server SHALL validate each token by calling `GET https://api.github.com/user` with the token; a 200 response confirms validity and the GitHub `login` is used as the principal name.

#### Scenario: Valid GitHub token grants access
- **WHEN** a client sends a request to a protected endpoint with a valid GitHub Bearer token in the `Authorization` header
- **THEN** `api-rs` SHALL call `GET https://api.github.com/user` with the token, receive HTTP 200, and allow the request to proceed

#### Scenario: Invalid or expired GitHub token is rejected
- **WHEN** a client sends a request with a revoked, expired, or malformed GitHub token
- **THEN** `api-rs` SHALL receive a non-200 response from GitHub user-info, and respond with HTTP 401 Unauthorized to the client

#### Scenario: Missing token results in 401
- **WHEN** a client sends a request to a protected endpoint with no `Authorization` header
- **THEN** `api-rs` SHALL respond with HTTP 401 Unauthorized without calling the GitHub user-info endpoint
