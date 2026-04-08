## Why

The `api-rs` subproject is an empty Gradle stub with no backend implementation. The emails-summarizer application needs a functional REST API that serves email categories and the messages within each category, secured with OAuth2 via GitHub as the identity provider, so the frontend can eventually replace its inline mock data with real, authenticated data.

## What Changes

- Bootstrap the `api-rs` Spring Boot 4+ application (Spring Web, Spring Data, Spring Security).
- Add a `GET /categories` endpoint returning all categories (name, code, description).
- Add a `GET /messages?categoryCode={code}` endpoint returning messages (id, title, body) filtered by category code.
- Secure all endpoints with OAuth2 — GitHub is the identity provider (OAuth App named "emails-summarizer"); Authorization Code flow only (GitHub does not support Client Credentials for OAuth Apps).
- Validate incoming GitHub Bearer tokens on api-rs by calling GitHub's user info endpoint (`https://api.github.com/user`) via a custom `OpaqueTokenIntrospector`.
- Persist data in an embedded H2 database; provide a schema and sample data via SQL scripts.
- Update `api-rs/build.gradle` with the Spring Boot Gradle plugin and required dependencies.

## Capabilities

### New Capabilities

- `category-listing`: REST endpoint that exposes the list of email categories.
- `message-listing`: REST endpoint that returns messages filtered by a category code.
- `oauth2-security`: OAuth2 resource server configuration using GitHub as the identity provider; validates GitHub Bearer tokens via user-info introspection.

### Modified Capabilities

*(none — no existing specs)*

## Non-Goals

- Email create, update, or delete operations.
- Full email body storage / retrieval beyond `body` text field.
- Integration with the Vue UI (that is a separate follow-up change).
- OAuth2 Client Credentials flow — GitHub OAuth Apps do not support this grant type.
- Embedded Spring Authorization Server — GitHub is used instead.
- Production-grade database (PostgreSQL, etc.) — H2 is sufficient for this phase.

## Impact

- **Files added**: `api-rs/src/main/java/...` (controllers, entities, repositories, security config), `api-rs/src/main/resources/application.yml`, `api-rs/src/main/resources/data.sql`, `api-rs/src/main/resources/schema.sql`
- **Files modified**: `api-rs/build.gradle` (Spring Boot plugin + dependencies)
- **New runtime dependencies**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `h2`
- **External dependency**: GitHub OAuth App ("emails-summarizer") must be registered in GitHub with the `ui` redirect URI
- **No changes** to the `ui` subproject or root build files
