# api-rs — Users & Role Management

The `user` feature slice manages GitHub user registration and DB-backed role retrieval. Every authenticated GitHub user is automatically registered on their first login and assigned `ROLE_READ`. Additional roles (`ROLE_EDIT`, `ROLE_DEL`) must be granted manually via the `ROLES` table.

---

## Overview

| Layer | Class | Responsibility |
|---|---|---|
| Service | `UserService` | find-or-create user, return roles from DB |
| Repository | `AppUserRepository` | DB access for `AppUser` (USERS table) |
| Repository | `UserRoleRepository` | DB access for `UserRole` (ROLES table) |
| Entity | `AppUser` | JPA entity — GitHub login, id, name, avatar |
| Entity | `UserRole` | JPA entity — one row per `(login, role)` |

---

## How Registration Works

`UserService.findOrRegister(login, githubId, name, avatarUrl)` is called by `GitHubOpaqueTokenIntrospector` on every authenticated request:

1. If `login` is **not** present in `USERS` → insert `AppUser` + insert `UserRole(login, "ROLE_READ")`.
2. If `login` **is** present in `USERS` → load all `UserRole` rows for that login and return the role strings.

The returned role list is converted to `GrantedAuthority` objects and attached to the `GitHubPrincipal`, which Spring Security uses for authorization checks.

**Registration flow**
```
GET /categories
Authorization: Bearer gho_xxxxx
         │
         ▼
GitHubOpaqueTokenIntrospector.introspect(token)
         │
         ├─ GET https://api.github.com/user  →  { login: "alice", id: 42, ... }
         │
         └─ UserService.findOrRegister("alice", 42, "Alice", "https://...")
                  │
                  ├─ First login?  YES → INSERT USERS + INSERT ROLES (ROLE_READ)
                  │                NO  → SELECT ROLES WHERE login = 'alice'
                  │
                  └─ returns ["ROLE_READ"]  (or more if extra roles were granted)
         │
         ▼
GitHubPrincipal { name: "alice", authorities: [ROLE_READ] }
```

---

## Data Model

See [../data-model.md](../data-model.md) for the full DDL. Summary:

| Table | Primary Key | Description |
|---|---|---|
| `USERS` | `login` (VARCHAR) | One row per registered GitHub user |
| `ROLES` | `id` (BIGINT) | One row per `(login, role)` combination |

The `(login, role)` pair is unique — a user can hold each role at most once.

---

## Granting Additional Roles

Roles are managed directly in the `ROLES` table. Use the H2 console (`http://localhost:8080/h2-console`) or any SQL client to insert or remove role rows.

**Example: view all users and their roles**
```sql
SELECT u.login, r.role
FROM USERS u
LEFT JOIN ROLES r ON r.login = u.login
ORDER BY u.login, r.role;
```

**Example: grant ROLE_EDIT and ROLE_DEL to an existing user**
```sql
INSERT INTO ROLES (login, role) VALUES ('octocat', 'ROLE_EDIT');
INSERT INTO ROLES (login, role) VALUES ('octocat', 'ROLE_DEL');
```

After these inserts, `octocat` holds all three roles on the next authenticated request (introspection loads roles fresh from the DB on every call).

**Example: revoke a role**
```sql
DELETE FROM ROLES WHERE login = 'octocat' AND role = 'ROLE_DEL';
```

**Example: check which users have ROLE_EDIT**
```sql
SELECT login FROM ROLES WHERE role = 'ROLE_EDIT';
```

---

## Role Reference

| Role | HTTP Methods | Grants access to |
|---|---|---|
| `ROLE_READ` | GET | `GET /categories`, `GET /messages` |
| `ROLE_EDIT` | POST, PUT | `POST /categories`, `POST /messages`, `PUT /categories/**`, `PUT /messages/**` |
| `ROLE_DEL` | DELETE | `DELETE /categories/**`, `DELETE /messages/**` |

---

## Adding a New Role

To introduce a new role (e.g., `ROLE_ADMIN`):

1. Add the authorization rule in `ResourceServerConfig.securityFilterChain()`:
```java
.requestMatchers(HttpMethod.POST, "/admin/**").hasRole("ADMIN")
```

2. Grant it to a user:
```sql
INSERT INTO ROLES (login, role) VALUES ('octocat', 'ROLE_ADMIN');
```

No code changes are required in `UserService` or `UserRole` — the DB-backed approach handles any role string.

---

## Class Reference

| Class | Package | Javadoc |
|---|---|---|
| `AppUser` | `com.emailssummarizer.apirs.user` | [AppUser.html](../../javadocs/com/emailssummarizer/apirs/user/AppUser.html) |
| `AppUserRepository` | `com.emailssummarizer.apirs.user` | [AppUserRepository.html](../../javadocs/com/emailssummarizer/apirs/user/AppUserRepository.html) |
| `UserRole` | `com.emailssummarizer.apirs.user` | [UserRole.html](../../javadocs/com/emailssummarizer/apirs/user/UserRole.html) |
| `UserRoleRepository` | `com.emailssummarizer.apirs.user` | [UserRoleRepository.html](../../javadocs/com/emailssummarizer/apirs/user/UserRoleRepository.html) |
| `UserService` | `com.emailssummarizer.apirs.user` | [UserService.html](../../javadocs/com/emailssummarizer/apirs/user/UserService.html) |
