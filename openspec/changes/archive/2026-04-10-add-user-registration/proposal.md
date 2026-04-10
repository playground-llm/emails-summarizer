## Why

When three-role authorization was implemented, a new user domain was introduced to the backend: GitHub users are automatically registered in a local `USERS` table on first login, and a default `ROLE_READ` is assigned via a `ROLES` table. This capability was implemented alongside `add-crud-authorization` but was never formally specified. This change captures that work so the data model and security behaviour are documented and promotable.

## What Changes

- A `user/` package in `api-rs` manages the `USERS` and `ROLES` tables
- `UserService.findOrRegister` registers new GitHub users with `ROLE_READ` on first login
- On subsequent logins, stored roles are returned from the `ROLES` table
- `GitHubOpaqueTokenIntrospector` calls `UserService.findOrRegister` after resolving the GitHub login to build the principal's authority list
- `schema.sql` is extended with `USERS` and `ROLES` DDL

## Non-Goals

- A role-management API or UI — roles beyond `ROLE_READ` are granted via direct DB access until a future change
- User profile display in the frontend
- Email or notification on registration
- Deregistration or account deletion
- Any persistence beyond the H2 in-memory store (a future change will introduce a persistent datasource)

## Capabilities

### New Capabilities

- `user-registration`: Automatic registration of authenticated GitHub users in the local `USERS` table with a default `ROLE_READ` grant on first login; subsequent logins load roles from the `ROLES` table

## Impact

- `api-rs/src/main/java/com/emailssummarizer/apirs/user/` — new package: `AppUser`, `UserRole`, `AppUserRepository`, `UserRoleRepository`, `UserService`
- `api-rs/src/main/java/com/emailssummarizer/apirs/security/GitHubOpaqueTokenIntrospector` — calls `UserService.findOrRegister` to load authorities
- `api-rs/src/main/resources/schema.sql` — `USERS` and `ROLES` table DDL added
- No new Gradle dependencies required
