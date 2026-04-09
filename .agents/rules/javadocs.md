---
paths:
  - "**/*.java"
---

# JavaDoc Rules

Rules for writing JavaDoc comments in the `api-rs` Java codebase.
Apply these rules to every Java class and method, new or modified.

---

## General principles

- Every public class and every public method MUST have a JavaDoc comment.
- Package-private and protected members SHOULD have a JavaDoc comment when their purpose is not self-evident.
- Private methods may omit JavaDoc unless they contain non-trivial logic worth explaining.
- JavaDoc is written for the *reader of the API*, not the implementer — describe *what* and *why*, not *how*.
- Do not restate the method signature in prose ("This method takes a String and returns a List"). Instead explain the contract, constraints, and intent.

---

## Class-level JavaDoc

Every class MUST have a class-level comment that includes:

1. A one-sentence summary on the first line (used by IDE tooltips and generated HTML).
2. A longer description (one or more paragraphs) covering:
   - The responsibility of the class within its layer (controller / service / repository / entity / DTO / config)
   - Key invariants or constraints it enforces
   - Any important collaborators or dependencies
3. `@see` tags pointing to related classes where helpful.

**Template:**

```java
/**
 * One-sentence summary of what the class does.
 *
 * <p>Additional context explaining the class's responsibility, which layer it belongs to,
 * and any important constraints or design decisions. Mention key collaborators where relevant.
 *
 * @see RelatedClass
 */
public class MyClass { … }
```

**Example — service class:**

```java
/**
 * Service responsible for all category business logic.
 *
 * <p>Acts as the sole entry point for category mutations and queries within the application.
 * Validates uniqueness of {@code code} on creation, enforces the immutability of {@code code}
 * on update, and prevents deletion of categories that still have associated messages.
 *
 * <p>Controllers must call this service rather than interacting with
 * {@link CategoryRepository} directly.
 *
 * @see CategoryController
 * @see CategoryRepository
 */
@Service
public class CategoryService { … }
```

**Example — controller class:**

```java
/**
 * REST controller that exposes the category management API under {@code /categories}.
 *
 * <p>Handles HTTP request/response mapping only; all business logic is delegated to
 * {@link CategoryService}. This class must not interact with any repository directly.
 */
@RestController
@RequestMapping("/categories")
public class CategoryController { … }
```

**Example — entity class:**

```java
/**
 * JPA entity representing a message category.
 *
 * <p>Maps to the {@code CATEGORY} database table. The {@code code} field is a unique
 * business key used as the URL path parameter and as a foreign key target from
 * {@link Message}. Once set, {@code code} must not be changed.
 */
@Entity
@Table(name = "CATEGORY")
public class Category { … }
```

**Example — security component:**

```java
/**
 * Validates GitHub opaque access tokens and assigns Spring Security granted authorities.
 *
 * <p>Calls {@code GET https://api.github.com/user} with the Bearer token to confirm
 * validity and retrieve the authenticated user's GitHub {@code login}. The login is
 * matched (case-insensitively) against three independent allow-lists to assign
 * {@code ROLE_READ}, {@code ROLE_EDIT}, and/or {@code ROLE_DEL}.
 *
 * <p>Allow-lists are injected from {@code app.readers}, {@code app.editors}, and
 * {@code app.deleters} application properties, which are bound from environment variables
 * {@code READERS_GITHUB_LOGINS}, {@code EDITORS_GITHUB_LOGINS}, and
 * {@code DELETERS_GITHUB_LOGINS} respectively.
 *
 * @see ResourceServerConfig
 */
@Component
public class GitHubOpaqueTokenIntrospector implements OpaqueTokenIntrospector { … }
```

---

## Method-level JavaDoc

Every public method MUST have a JavaDoc comment that includes:

1. **Summary sentence** — what the method does (first line).
2. **Description** — constraints, side effects, or notable behaviour (omit if the summary is sufficient).
3. **`@param`** — one tag per parameter; describe the expected value, valid range, and what `null` means (if applicable).
4. **`@return`** — describe the returned value and what it represents; omit for `void`.
5. **`@throws`** — one tag per checked or documented unchecked exception; describe the condition that triggers it.

**Template:**

```java
/**
 * One-sentence summary of what the method does.
 *
 * <p>Optional additional details about constraints, side effects, or notable behaviour.
 *
 * @param paramName  description of the parameter and its valid values
 * @return           description of the returned value
 * @throws SomeException  condition under which this exception is thrown
 */
public ReturnType methodName(ParamType paramName) { … }
```

**Example — service create method:**

```java
/**
 * Creates a new category from the given request.
 *
 * <p>The {@code code} field of the request must be unique across all categories.
 * If a category with the same {@code code} already exists, a conflict exception is thrown.
 *
 * @param request  the category data to persist; must not be {@code null};
 *                 {@code request.code()} must be non-blank and unique
 * @return         the persisted {@link Category} with its generated {@code id} populated
 * @throws CategoryCodeConflictException  if a category with {@code request.code()} already exists
 */
public Category create(CategoryRequest request) { … }
```

**Example — service delete method:**

```java
/**
 * Deletes the category identified by the given code.
 *
 * <p>Deletion is blocked if any {@link Message} records still reference this category.
 * The caller must delete all associated messages before removing the category.
 *
 * @param code  the unique business key of the category to delete; must not be {@code null}
 * @throws CategoryNotFoundException       if no category with {@code code} exists
 * @throws CategoryHasMessagesException    if the category still has associated messages
 */
public void delete(String code) { … }
```

**Example — controller endpoint method:**

```java
/**
 * Handles {@code POST /categories} requests to create a new category.
 *
 * <p>Delegates to {@link CategoryService#create(CategoryRequest)} and returns
 * the created resource with HTTP 201 Created.
 *
 * @param request  the request body containing {@code name}, {@code code}, and
 *                 optional {@code description}; validated by the service layer
 * @return         a {@link ResponseEntity} with status 201 and the created
 *                 {@link Category} as the body; or 409 if the code already exists
 */
@PostMapping
public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest request) { … }
```

**Example — repository method:**

```java
/**
 * Returns the category with the given business code, if it exists.
 *
 * @param code  the unique category code to look up; must not be {@code null}
 * @return      an {@link Optional} containing the matching {@link Category},
 *              or an empty {@link Optional} if none is found
 */
Optional<Category> findByCode(String code);
```

**Example — security introspection method:**

```java
/**
 * Introspects the given opaque GitHub token by calling the GitHub user-info endpoint.
 *
 * <p>Sends a {@code GET https://api.github.com/user} request with the token as a
 * Bearer credential. On success, extracts the GitHub {@code login} and builds a
 * {@link GitHubPrincipal} with the applicable granted authorities based on the
 * configured allow-lists.
 *
 * @param token  the raw GitHub access token to validate; must not be {@code null}
 * @return       an {@link OAuth2AuthenticatedPrincipal} representing the authenticated
 *               GitHub user with zero or more of {@code ROLE_READ}, {@code ROLE_EDIT},
 *               {@code ROLE_DEL} granted authorities
 * @throws BadOpaqueTokenException  if the token is invalid, expired, or the GitHub
 *                                  user-info endpoint returns a non-200 response
 */
@Override
public OAuth2AuthenticatedPrincipal introspect(String token) { … }
```

---

## Formatting rules

- Use `{@link ClassName}` or `{@link ClassName#methodName(ParamType)}` to cross-reference related types and methods.
- Use `{@code value}` for inline code (parameter names, literals, HTTP methods, paths).
- Wrap lines at 100 characters inside the comment.
- Use `<p>` to start a new paragraph inside a multi-paragraph comment — blank lines alone are not rendered.
- Do not use `@author` or `@version` tags — version history belongs in git.
