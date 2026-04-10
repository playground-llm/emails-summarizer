# Data Model

This document describes the database schema, entity relationships, and data access rules for `api-rs`.

---

## Overview

`api-rs` uses an **H2 in-memory database** initialised at startup from `schema.sql` (DDL) and `data.sql` (seed data). The schema contains four tables: `USERS` and `ROLES` (user identity and authorisation), plus `CATEGORY` and `MESSAGE` (content), linked by foreign keys.

---

## Tech Stack

- **Database**: H2 in-memory (`jdbc:h2:mem:emailsdb`)
- **ORM**: Spring Data JPA (Hibernate)
- **Schema init**: `schema.sql` + `data.sql` — run at every startup (`spring.sql.init.mode: always`)
- **DDL mode**: `ddl-auto: none` — schema is managed manually via `schema.sql`

---

## Schema

**Full DDL**
```sql
CREATE TABLE IF NOT EXISTS USERS (
    login       VARCHAR(100) NOT NULL PRIMARY KEY,
    github_id   BIGINT,
    name        VARCHAR(255),
    avatar_url  VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS ROLES (
    id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(100) NOT NULL,
    role  VARCHAR(50)  NOT NULL,
    CONSTRAINT fk_roles_user       FOREIGN KEY (login) REFERENCES USERS(login),
    CONSTRAINT uq_roles_login_role UNIQUE (login, role)
);

CREATE TABLE IF NOT EXISTS CATEGORY (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS MESSAGE (
    id            UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    body          CLOB,
    category_code VARCHAR(100) NOT NULL,
    CONSTRAINT fk_message_category FOREIGN KEY (category_code) REFERENCES CATEGORY(code)
);
```

---

## Entity Descriptions

### USERS

| Column | Type | Constraints | Description |
|---|---|---|---|
| `login` | `VARCHAR(100)` | PK | GitHub login name; natural primary key |
| `github_id` | `BIGINT` | nullable | Numeric GitHub user ID |
| `name` | `VARCHAR(255)` | nullable | Display name from GitHub profile |
| `avatar_url` | `VARCHAR(500)` | nullable | GitHub avatar image URL |

Rows are created automatically by `UserService.findOrRegister` on every user's first successful login. No manual insert is required.

### ROLES

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Surrogate primary key |
| `login` | `VARCHAR(100)` | NOT NULL, FK → USERS.login | GitHub login of the user who holds this role |
| `role` | `VARCHAR(50)` | NOT NULL | Spring Security role string (`ROLE_READ`, `ROLE_EDIT`, `ROLE_DEL`) |

The `(login, role)` pair is unique. Every newly registered user automatically receives a `ROLE_READ` row. Additional roles must be inserted manually.

### CATEGORY

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Surrogate primary key |
| `name` | `VARCHAR(255)` | NOT NULL | Human-readable display name |
| `code` | `VARCHAR(100)` | NOT NULL, UNIQUE | Business key — used in URLs and as FK target |
| `description` | `VARCHAR(500)` | nullable | Optional description |

> `code` is **immutable** after creation. It is used as a URL path parameter and as the foreign key target in `MESSAGE`. Changing it would break existing references.

### MESSAGE

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | PK, DEFAULT RANDOM_UUID() | Randomly generated UUID |
| `title` | `VARCHAR(255)` | NOT NULL | Subject / title of the message |
| `body` | `CLOB` | nullable | Full message body text |
| `category_code` | `VARCHAR(100)` | NOT NULL, FK → CATEGORY.code | Category this message belongs to |

---

## Entity Relationship

```
USERS (1) ──────── (many) ROLES
  login (PK)               login (FK)

CATEGORY (1) ──────── (many) MESSAGE
   code (PK/UK)              category_code (FK)
```

One user can hold many roles (one row per role). One category can have many messages. A message belongs to exactly one category. The two groups (`USERS`/`ROLES` and `CATEGORY`/`MESSAGE`) are independent — there is no FK between them.

---

## Business Rules

- **`category.code` is immutable** — the `PUT /categories/{code}` endpoint updates `name` and `description` only; `code` cannot be changed.
- **Cascade delete is not automatic** — deleting a category that still has messages returns `409 Conflict`. The caller must delete all messages in the category first.
- **Duplicate codes** — attempting to create a category with a `code` that already exists returns `409 Conflict`.
- **Invalid `category_code` in messages** — attempting to create or update a message with a `category_code` that does not exist returns `409 Conflict`.

---

## Seed Data

The following rows are inserted at startup by `data.sql`:

**Categories**
```sql
INSERT INTO CATEGORY (name, code, description) VALUES
    ('Inbox',    'INBOX',    'General incoming messages'),
    ('Work',     'WORK',     'Work-related emails and notifications'),
    ('Personal', 'PERSONAL', 'Personal correspondence');
```

**Messages (sample)**
```sql
INSERT INTO MESSAGE (title, body, category_code) VALUES
    ('Welcome to Emails Summarizer',
     'Thank you for trying Emails Summarizer.',
     'INBOX'),
    ('Q2 Planning Meeting',
     'The Q2 planning meeting is scheduled for next Monday at 10:00 AM.',
     'WORK'),
    ('Weekend Plans',
     'Hey! Are you free this weekend?',
     'PERSONAL');
```

---

## H2 Console

The H2 console is available in development at `http://localhost:8080/h2-console`.

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:emailsdb` |
| Username | `sa` |
| Password | _(empty)_ |

**Example: query all categories via H2 console SQL**
```sql
SELECT * FROM CATEGORY;
SELECT * FROM MESSAGE WHERE category_code = 'INBOX';
```

---

## Configuration

Datasource config lives in `api-rs/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:emailsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
  sql:
    init:
      mode: always
  h2:
    console:
      enabled: true
      path: /h2-console
```

To enable SQL logging during development, set `show-sql: true`.
