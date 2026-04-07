## ADDED Requirements

### Requirement: Initiate OAuth2 Authorization Code flow
The page SHALL detect that no valid access token is present in `sessionStorage` and SHALL redirect the browser to the Spring Authorization Server's `/oauth2/authorize` endpoint with the correct `client_id`, `response_type=code`, `redirect_uri`, and `scope` parameters.

#### Scenario: Unauthenticated user is redirected to login
- **WHEN** the page loads and no `access_token` is found in `sessionStorage`
- **THEN** the browser SHALL be redirected to the authorization server's authorization endpoint

#### Scenario: Redirect includes required OAuth2 parameters
- **WHEN** the authorization redirect is constructed
- **THEN** the URL SHALL contain `response_type=code`, a registered `client_id`, and a `redirect_uri` matching the registered value in the Spring Authorization Server

---

### Requirement: Handle OAuth2 authorization code callback
After the user authenticates on the authorization server, the browser is redirected back to `index.html` with a `code` query parameter. The page SHALL detect this `code`, exchange it for an access token, store the token, and remove the `code` from the URL.

#### Scenario: Authorization code is exchanged for an access token
- **WHEN** the page loads with a `code` query parameter in the URL
- **THEN** the page SHALL send a `POST /oauth2/token` request with `grant_type=authorization_code`, the `code`, `client_id`, `client_secret`, and `redirect_uri`, and store the returned `access_token` in `sessionStorage`

#### Scenario: URL is cleaned after token exchange
- **WHEN** the token is successfully stored
- **THEN** the `code` query parameter SHALL be removed from the browser URL (via `history.replaceState`) so a page refresh does not attempt to re-use the one-time code

#### Scenario: Token exchange failure shows an error
- **WHEN** the `POST /oauth2/token` request fails or returns an error
- **THEN** the page SHALL display an error message and SHALL NOT attempt to call protected API endpoints

---

### Requirement: Attach Bearer token to all API requests
Every request to a protected `api-rs` endpoint SHALL include the stored `access_token` as a Bearer token in the `Authorization` header.

#### Scenario: API request includes Authorization header
- **WHEN** the application calls `GET /categories` or `GET /messages`
- **THEN** the request SHALL include the header `Authorization: Bearer <access_token>`

#### Scenario: 401 response triggers re-authentication
- **WHEN** any API request receives an HTTP 401 response
- **THEN** the page SHALL clear the stored token from `sessionStorage` and redirect the user to the authorization server to re-authenticate
