## Context

The `add-crud-authorization` change required knowing which roles a GitHub user holds. Rather than using static env-var allow-lists (the originally proposed approach), the implementation chose to persist users and their roles in the H2 database. This enables role changes without restarting the service and lays the groundwork for a future role-management API.

The `user/` package was introduced to own this concern cleanly, separate from the security infrastructure in the `security/` package.

## Goals / Non-Goals

**Goals:**
- Persist GitHub user profile data (`login`, `github_id`, `name`, `avatar_url`) on first successful token introspection
- Assign `ROLE_READ` by default to every first-time user
- Load stored roles from DB on subsequent logins
- Keep the concern isolated in a `user/` package called only by the introspector

**Non-Goals:**
- Role-management API (POST/PUT/DELETE on roles)
- Persistent storage — H2 in-memory is used; roles are lost on restart
- User profile endpoint
- Frontend integration

## Decisions

### Decision 1: GitHub `login` as the natural primary key for `USERS`

**Chosen**: `USERS.login VARCHAR(100) NOT NULL PRIMARY KEY`. No surrogate ID.

**Rationale**: GitHub logins are unique across the platform and are the identifier already used as `principal.getName()` throughout the security layer. Avoids an extra join for every role lookup.

---

### Decision 2: `(login, role)` unique constraint in `ROLES`

**Chosen**: `CONSTRAINT uq_roles_login_role UNIQUE (login, role)` in addition to a surrogate `id` PK.

**Rationale**: Prevents duplicate role rows, which would cause the same authority to appear twice in the principal's authority list — harmless but noisy and confusing.

---

### Decision 3: `UserService.findOrRegister` as the single entry point

**Chosen**: The introspector calls one method; internally it either inserts new rows or selects existing ones. The entire operation runs in a single `@Transactional` boundary.

**Rationale**: Keeps introspection code simple — one call, one result. The service owns all DB logic so the introspector stays focused on token validation and authority mapping.

---

### Decision 4: Default `ROLE_READ` for all new users

**Chosen**: Every new GitHub user gets `ROLE_READ` automatically.

**Rationale**: The application is currently private. Everyone who can authenticate via GitHub OAuth can reasonably be allowed to read. `ROLE_EDIT` and `ROLE_DEL` are granted explicitly (via DB) to trusted users.

## Risks / Trade-offs

- **Risk: Roles lost on restart** — H2 is in-memory; all `USERS`/`ROLES` rows are wiped on each restart. Mitigation: users re-register automatically on next login and get `ROLE_READ` again; EDIT/DEL must be re-granted manually until persistent storage is added.
- **Risk: No eviction / de-registration** — once a row is in `USERS` it persists for the lifetime of the H2 instance. Mitigation: accepted for now; a future change will add user management.
- **Risk: Token cache races** — Spring Security may cache introspection results; a freshly granted role won't be visible until the cache expires or the service restarts. Mitigation: documented as expected; operators should restart to pick up role changes immediately.
