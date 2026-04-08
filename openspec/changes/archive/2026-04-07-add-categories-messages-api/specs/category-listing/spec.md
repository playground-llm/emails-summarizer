## ADDED Requirements

### Requirement: List all categories
The API SHALL expose a `GET /categories` endpoint that returns all available email categories. Each category object in the response SHALL include `name` (string), `code` (string), and `description` (string) fields.

#### Scenario: Successful retrieval of categories
- **WHEN** an authenticated client sends `GET /categories` with a valid Bearer token
- **THEN** the API SHALL respond with HTTP 200 and a JSON array of all category objects, each containing `name`, `code`, and `description`

#### Scenario: Empty category list
- **WHEN** an authenticated client sends `GET /categories` and no categories exist in the database
- **THEN** the API SHALL respond with HTTP 200 and an empty JSON array `[]`

#### Scenario: Unauthenticated request is rejected
- **WHEN** a client sends `GET /categories` without a Bearer token
- **THEN** the API SHALL respond with HTTP 401 Unauthorized

#### Scenario: Invalid token is rejected
- **WHEN** a client sends `GET /categories` with a malformed or expired Bearer token
- **THEN** the API SHALL respond with HTTP 401 Unauthorized
