package com.emailssummarizer.apirs.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller that exposes the category management API under {@code /categories}.
 *
 * <p>Handles HTTP request/response mapping only; all business logic is delegated to
 * {@link CategoryService}. This class must not interact with any repository directly.
 *
 * <p>Requires the following roles depending on the HTTP method:
 * <ul>
 *   <li>{@code ROLE_READ} — {@code GET /categories}</li>
 *   <li>{@code ROLE_EDIT} — {@code POST /categories}, {@code PUT /categories/{code}}</li>
 *   <li>{@code ROLE_DEL}  — {@code DELETE /categories/{code}}</li>
 * </ul>
 *
 * @see CategoryService
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Constructs a {@code CategoryController} with its required service dependency.
     *
     * @param categoryService  the service handling category business logic; must not be {@code null}
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Handles {@code GET /categories} requests to retrieve all categories.
     *
     * <p>Delegates to {@link CategoryService#listAll()} and returns the full list
     * of persisted categories in insertion order. Requires {@code ROLE_READ}.
     *
     * @return a list of all {@link Category} entities; never {@code null}, may be empty
     */
    @GetMapping
    public List<Category> listCategories() {
        return categoryService.listAll();
    }

    /**
     * Handles {@code POST /categories} requests to create a new category.
     *
     * <p>Delegates to {@link CategoryService#create(CategoryRequest)} and returns
     * the created resource with HTTP 201 Created. Requires {@code ROLE_EDIT}.
     *
     * @param request  the request body containing {@code name}, {@code code}, and optional
     *                 {@code description}; validated by the service layer
     * @return         a {@link ResponseEntity} with status 201 and the persisted
     *                 {@link Category} as the body; or 409 if the code already exists
     */
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    /**
     * Handles {@code PUT /categories/{code}} requests to update an existing category.
     *
     * <p>Delegates to {@link CategoryService#update(String, CategoryRequest)}. Only
     * {@code name} and {@code description} are updated; the {@code code} path variable
     * is used as a lookup key and remains immutable. Requires {@code ROLE_EDIT}.
     *
     * @param code     the unique business key of the category to update
     * @param request  the request body with the new {@code name} and optional {@code description}
     * @return         the updated {@link Category}; or 404 if no category with {@code code} exists
     */
    @PutMapping("/{code}")
    public Category updateCategory(@PathVariable String code,
                                   @RequestBody CategoryRequest request) {
        return categoryService.update(code, request);
    }

    /**
     * Handles {@code DELETE /categories/{code}} requests to remove a category.
     *
     * <p>Delegates to {@link CategoryService#delete(String)} and responds with
     * HTTP 204 No Content on success. Requires {@code ROLE_DEL}.
     *
     * @param code  the unique business key of the category to delete; returns 404 if not found,
     *              409 if the category still has associated messages
     */
    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String code) {
        categoryService.delete(code);
    }
}
