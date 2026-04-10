## 1. CORS Update

- [x] 1.1 In `ResourceServerConfig.java`, add `"PUT"` and `"DELETE"` to `config.setAllowedMethods(...)` so write requests from the browser are not blocked

## 2. Category Write Endpoints

- [x] 2.1 Create `CategoryRequest.java` record in `category/` package with fields: `String name`, `String code`, `String description`
- [x] 2.2 Add `POST /categories` to `CategoryController`: accept `@RequestBody CategoryRequest`, persist via `categoryRepository.save(...)`, return `ResponseEntity<Category>` with status `201 Created`
- [x] 2.3 Add `PUT /categories/{code}` to `CategoryController`: look up by code (404 if missing), update `name` and `description`, save, return `200 OK` with updated entity
- [x] 2.4 Add `DELETE /categories/{code}` to `CategoryController`: look up by code (404 if missing), check `messageRepository.existsByCategoryCode(code)` and return `409 Conflict` if true, otherwise delete and return `204 No Content`
- [x] 2.5 Add `existsByCategoryCode(String categoryCode)` derived query to `MessageRepository`

## 3. Message Write Endpoints

- [x] 3.1 Create `MessageRequest.java` record in `message/` package with fields: `String title`, `String body`, `String categoryCode`
- [x] 3.2 Add `POST /messages` to `MessageController`: verify `categoryCode` exists (409 if not), persist new `Message`, return `201 Created` with body
- [x] 3.3 Add `PUT /messages/{id}` to `MessageController`: look up by UUID id (404 if missing), verify new `categoryCode` exists (409 if not), update fields, save, return `200 OK`
- [x] 3.4 Add `DELETE /messages/{id}` to `MessageController`: look up by UUID id (404 if missing), delete, return `204 No Content`

## 4. Verification

- [x] 4.1 Run `./gradlew :api-rs:bootRun` and confirm startup with no errors
- [x] 4.2 `POST /categories` with a valid body — confirm `201` and the category appears in `GET /categories`
- [x] 4.3 `POST /categories` with a duplicate `code` — confirm `409`
- [x] 4.4 `PUT /categories/{code}` — confirm `200` and name is updated
- [x] 4.5 `PUT /categories/NONEXISTENT` — confirm `404`
- [x] 4.6 `DELETE /categories/{code}` on a category with messages — confirm `409`
- [x] 4.7 `DELETE /categories/{code}` on an empty category — confirm `204` and it's gone from `GET /categories`
- [x] 4.8 `POST /messages` with a valid body — confirm `201` and message appears in `GET /messages?categoryCode=...`
- [x] 4.9 `POST /messages` with unknown `categoryCode` — confirm `409`
- [x] 4.10 `PUT /messages/{id}` — confirm `200` and updated fields
- [x] 4.11 `DELETE /messages/{id}` — confirm `204` and message is gone
- [x] 4.12 Call any write endpoint without a Bearer token — confirm `401`
