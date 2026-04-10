## 1. Configuration

- [x] 1.1 Add `app.readers`, `app.editors`, and `app.deleters` properties to `api-rs/src/main/resources/application.yml`, each bound from the corresponding env var with an empty default
- [x] 1.2 Add `READERS_GITHUB_LOGINS=`, `EDITORS_GITHUB_LOGINS=`, and `DELETERS_GITHUB_LOGINS=` entries to `local/setup_env.sh` (populate your own login for each role you want to test)
- [x] 1.3 Add the same three entries as placeholders to `setup_env_template.sh`

## 2. Introspector — assign ROLE_READ, ROLE_EDIT, ROLE_DEL authorities

- [x] 2.1 Inject three `Set<String>` values into `GitHubOpaqueTokenIntrospector` via `@Value` (one per allow-list), normalising all entries to lowercase at construction time
- [x] 2.2 In `GitHubOpaqueTokenIntrospector.introspect()`, after resolving the GitHub `login`, build a list of `SimpleGrantedAuthority` instances for each set the lowercase login appears in (`ROLE_READ`, `ROLE_EDIT`, `ROLE_DEL`)
- [x] 2.3 Update `GitHubPrincipal.getAuthorities()` to return this dynamically built authority list
- [x] 2.4 Add a startup WARN log for each allow-list that is empty, indicating which operations will be universally denied

## 3. Security filter chain — enforce per-method roles

- [x] 3.1 In `ResourceServerConfig.securityFilterChain()`, replace (or augment) the terminal `.anyRequest().authenticated()` with explicit rules:
  - `DELETE /categories/**` and `DELETE /messages/**` → `hasRole("DEL")`
  - `POST /categories`, `POST /messages` → `hasRole("EDIT")`
  - `PUT /categories/**`, `PUT /messages/**` → `hasRole("EDIT")`
  - `GET /categories`, `GET /messages` → `hasRole("READ")`
  - Remaining paths (token exchange, h2-console) remain `permitAll` / `authenticated` as before

## 4. Verification

- [x] 4.1 Start `api-rs` with only `READERS_GITHUB_LOGINS=<your-login>`; confirm `GET /categories` returns `200` and `POST /categories` returns `403`
- [x] 4.2 Add your login to `EDITORS_GITHUB_LOGINS`; confirm `POST /categories` and `PUT /categories/{code}` are no longer blocked
- [x] 4.3 Without `DELETERS_GITHUB_LOGINS` set, confirm `DELETE /categories/{code}` returns `403`
- [x] 4.4 Add your login to `DELETERS_GITHUB_LOGINS`; confirm `DELETE /categories/{code}` is no longer blocked
- [x] 4.5 Start `api-rs` with all three lists empty; confirm every data endpoint returns `403` for a valid token
- [x] 4.6 Verify an unauthenticated request (no `Authorization` header) to any endpoint still returns `401`, not `403`
