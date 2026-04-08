## Context

`api-rs` currently authenticates all requests via GitHub opaque token introspection (`GitHubOpaqueTokenIntrospector`). Every valid GitHub user has identical access — there is no distinction between readers, editors, and deleters. The `GitHubPrincipal` returned by the introspector always has an empty authorities list (`List.of()`), so there is no existing authority mechanism to extend.

The goal is to introduce three independent access tiers enforced per HTTP method:
- `ROLE_READ` — required for `GET` on data endpoints
- `ROLE_EDIT` — required for `POST` and `PUT`
- `ROLE_DEL` — required for `DELETE`

Each role is backed by a separate allow-list of GitHub logins supplied via environment variable. A user may hold multiple roles by appearing in more than one list.

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

### Decision 1: Assign all applicable granted authorities at introspection time (inside `GitHubOpaqueTokenIntrospector`)

**Chosen**: After resolving the GitHub `login`, the introspector checks three independent sets (readers, editors, deleters) and builds a list of `SimpleGrantedAuthority` instances (`ROLE_READ`, `ROLE_EDIT`, `ROLE_DEL`) for any sets the login belongs to. `GitHubPrincipal.getAuthorities()` returns this list.

**Alternatives considered:**
- **Option B — custom `AuthorizationManager` beans**: Would work but scatters the "who has what role" logic away from the introspection step, and requires wiring three separate managers.
- **Option C — `@PreAuthorize` on controller methods**: Fine-grained but adds annotations across many methods and requires enabling `@EnableMethodSecurity`. URL-pattern rules in the filter chain are easier to audit for a small API.

**Rationale**: Centralising all authority assignment in one place (the introspector) keeps `ResourceServerConfig` a clean, declarative list of HTTP-method rules. Three `requestMatchers` lines replace three policy decisions.

---

### Decision 2: Three independent comma-separated env vars, each bound to a `Set<String>`

**Chosen**:
- `READERS_GITHUB_LOGINS` → `app.readers`
- `EDITORS_GITHUB_LOGINS` → `app.editors`
- `DELETERS_GITHUB_LOGINS` → `app.deleters`

Each defaults to empty. Comparisons are case-insensitive (values normalised to lowercase at startup).

**Alternatives considered:**
- Single env var with role prefixes (e.g., `alice:read,bob:edit,alice:del`) — harder to parse, harder to document, harder to override in container orchestration.
- YAML list format — less convenient when overriding via env var in twelve-factor deployments.

**Rationale**: Separate env vars are independently overridable, clearly named, and trivially parseable.

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

- **Risk: All lists empty at startup** → every data endpoint returns `403`. Mitigation: log a WARN at startup for each empty role list.
- **Risk: Case sensitivity** → GitHub logins are case-insensitive. Mitigation: normalise all configured values and the introspected login to lowercase.
- **Risk: Roles are independent, not hierarchical** → an admin who should read + edit + delete must appear in all three env vars. Mitigation: document clearly; the tradeoff is simplicity over convenience.
- **Risk: Token caching** → Spring Security may cache introspection results; role changes take effect only after cache expiry or service restart. Mitigation: documented as expected — this is a static config model.

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
