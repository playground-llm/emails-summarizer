## Context

`api-rs` currently authenticates all requests via GitHub opaque token introspection (`GitHubOpaqueTokenIntrospector`). Every valid GitHub user has identical access — there is no distinction between readers, editors, and deleters. The `GitHubPrincipal` returned by the introspector always has an empty authorities list (`List.of()`), so there is no existing authority mechanism to extend.

The goal is to introduce three independent access tiers enforced per HTTP method:
- `ROLE_READ` — required for `GET` on data endpoints
- `ROLE_EDIT` — required for `POST` and `PUT`
- `ROLE_DEL` — required for `DELETE`

Roles are stored in the database (a `user/` package implemented alongside this change). On first successful GitHub login a user record and a default `ROLE_READ` row are created automatically. Additional roles (`ROLE_EDIT`, `ROLE_DEL`) are granted via direct database manipulation until a role-management API exists.

> **Implementation note:** An earlier design used static env-var allow-lists (`READERS_GITHUB_LOGINS`, etc.). The final implementation chose a DB-backed model (`USERS` / `ROLES` tables, `UserService.findOrRegister`) for flexibility and to support future role-management features. The env-var approach was abandoned.

## Goals / Non-Goals

**Goals:**
- `GET /categories` and `GET /messages` restricted to users with `ROLE_READ`
- `POST` and `PUT` on `/categories` and `/messages` restricted to `ROLE_EDIT`
- `DELETE` on `/categories` and `/messages` restricted to `ROLE_DEL`
- Authenticated users absent from all lists receive `403 Forbidden`
- Each allow-list configured independently via a separate env var
- Roles are independent — holding `ROLE_EDIT` does not imply `ROLE_READ`
- No new library dependencies

**Non-Goals:**
- UI enforcement (no frontend changes)
- GitHub team or organization membership checks
- Per-resource ownership or per-user scoping
- Hierarchical roles or role inheritance
- Dynamic role management API

## Decisions

### Decision 1: Assign granted authorities at introspection time from the database

**Chosen**: After resolving the GitHub `login`, the introspector calls `UserService.findOrRegister(login, ...)`, which returns the list of role strings stored in the `ROLES` table for that user. These are converted to `SimpleGrantedAuthority` instances. On first login the user is inserted into `USERS` and given `ROLE_READ` automatically.

**Alternatives considered:**
- **Option A — static env-var allow-lists**: Simple, zero-DB. Initially designed. Abandoned because it makes role changes require a restart and cannot support a future role-management API.
- **Option C — `@PreAuthorize` on controller methods**: Fine-grained but adds annotations across many methods and requires enabling `@EnableMethodSecurity`. URL-pattern rules in the filter chain are easier to audit.

**Rationale**: DB-backed roles allow role changes without restarts and lay the groundwork for a future admin API. The tradeoff is that `ROLE_EDIT` and `ROLE_DEL` must currently be granted via direct DB access until a management endpoint is built.

---

### Decision 2: `user/` package — `USERS` + `ROLES` tables, find-or-create on login

**Chosen**: A new `user` package introduces `AppUser` (pk = GitHub login), `UserRole` (fk → `USERS.login`, unique on `(login, role)`), `AppUserRepository`, `UserRoleRepository`, and `UserService`. `UserService.findOrRegister` is the single entry point called by the introspector.

- First login → `INSERT INTO USERS` + `INSERT INTO ROLES (login, 'ROLE_READ')`
- Subsequent logins → `SELECT FROM ROLES WHERE login = ?`

**Rationale**: Using the GitHub login as the natural primary key avoids a surrogate ID and aligns with how Spring Security identifies the principal throughout the stack. The `(login, role)` unique constraint prevents duplicate rows.

---

### Decision 3: Three URL-pattern + HTTP-method rules in `ResourceServerConfig`

**Chosen**: Replace `.anyRequest().authenticated()` with explicit method rules ordered from most-specific (DELETE) to least (GET), plus a catch-all `denyAll()` for any authenticated user not matching:

```
.requestMatchers(HttpMethod.DELETE, "/categories/**", "/messages/**").hasRole("DEL")
.requestMatchers(HttpMethod.POST,   "/categories",    "/messages").hasRole("EDIT")
.requestMatchers(HttpMethod.PUT,    "/categories/**", "/messages/**").hasRole("EDIT")
.requestMatchers(HttpMethod.GET,    "/categories",    "/messages").hasRole("READ")
.anyRequest().authenticated()   // token exchange, h2-console etc. still reachable
```

Permit-all paths (`/oauth2/token`, `/h2-console/**`) are declared before these rules and are unaffected.

## Risks / Trade-offs

- **Risk: No role-management API** → `ROLE_EDIT` and `ROLE_DEL` can only be granted via direct DB access (H2 console in dev). Mitigation: accepted for now; a future `add-role-management` change will address this.
- **Risk: Case sensitivity** → GitHub logins are case-insensitive. Mitigation: the introspector normalises the login to lowercase before calling `UserService`.
- **Risk: Roles are independent, not hierarchical** → a user who should read + edit + delete needs three separate `ROLES` rows. Mitigation: document clearly.
- **Risk: Token caching** → Spring Security may cache introspection results; role changes take effect only after cache expiry or service restart. Mitigation: documented as expected behaviour.
- **Risk: DB is in-memory (H2)** → roles are lost on restart in dev. Mitigation: seed data or manual re-grant after each restart until persistent storage is introduced.

## Migration Plan

1. Add `READERS_GITHUB_LOGINS`, `EDITORS_GITHUB_LOGINS`, `DELETERS_GITHUB_LOGINS` to `local/setup_env.sh` and `setup_env_template.sh`
2. Bind `app.readers`, `app.editors`, `app.deleters` in `application.yml`
3. Modify `GitHubOpaqueTokenIntrospector` to accept three sets and emit the corresponding `ROLE_*` authorities
4. Update `ResourceServerConfig` with three per-method authorization rules
5. Restart `api-rs` — no database or schema changes needed, fully stateless rollout
6. **Rollback**: Revert the `requestMatchers` rules to `.anyRequest().authenticated()`; remove the `@Value`/property bindings; redeploy

## Open Questions

- Should the `/h2-console` path also require a role? (Currently `permitAll` — no change needed for dev.)
- Should `DELETE /categories/{code}` cascade-delete messages or return `409`? (Owned by `add-crud-categories-messages-api`; this design defers.)
