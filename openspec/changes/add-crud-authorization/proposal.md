## Why

All authenticated users currently have equal access across the API. As the application moves toward real content management, three distinct permission levels are needed: users who may only read, users who may create and update, and users who may delete. Collapsing these into a single allow-list is too coarse — a content author should not necessarily have delete rights.

## What Changes

- `GET /categories` and `GET /messages` require `ROLE_READ` — granted to users in the `READERS_GITHUB_LOGINS` allow-list
- `POST` and `PUT` on `/categories` and `/messages` require `ROLE_EDIT` — granted to users in the `EDITORS_GITHUB_LOGINS` allow-list
- `DELETE` on `/categories` and `/messages` requires `ROLE_DEL` — granted to users in the `DELETERS_GITHUB_LOGINS` allow-list
- Each allow-list is a comma-separated env var; users can appear in multiple lists to hold multiple roles
- Authenticated users absent from all lists receive `403 Forbidden` on every data endpoint
- The `/oauth2/token` exchange endpoint stays permit-all (unchanged)

## Non-Goals

- UI enforcement — the frontend is not in scope; access control is API-only
- GitHub team or organization membership checks — username allow-lists are sufficient for now
- Fine-grained per-resource ownership (e.g., "only the creator can delete")
- Role management UI or API — roles are configured statically via env vars
- Hierarchical roles (e.g., `ROLE_EDIT` implying `ROLE_READ`) — each role is independent

## Capabilities

### New Capabilities

- `crud-authorization`: Three-role access control (`ROLE_READ`, `ROLE_EDIT`, `ROLE_DEL`) enforced per HTTP method across the categories and messages APIs, each backed by a separate GitHub login allow-list

### Modified Capabilities

- `oauth2-security`: Authorization rules are added on top of existing authentication — the introspector now assigns `ROLE_READ`, `ROLE_EDIT`, and/or `ROLE_DEL` based on configured allow-lists; the security filter chain enforces these roles per HTTP method

## Impact

- `api-rs/src/main/java/com/emailssummarizer/apirs/security/` — `ResourceServerConfig` updated with per-method HTTP authorization rules; `GitHubOpaqueTokenIntrospector` updated to assign multiple granted authorities
- `application.yml` — three new properties (`app.readers`, `app.editors`, `app.deleters`) bound from env vars
- `local/setup_env.sh` and `setup_env_template.sh` — three new env var entries
- No new dependencies required (Spring Security already on classpath)
