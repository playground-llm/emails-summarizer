package com.emailssummarizer.apirs.category;

import com.emailssummarizer.apirs.message.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MessageRepository messageRepository;

    /**
     * Constructs a {@code CategoryService} with its required repository dependencies.
     *
     * @param categoryRepository  the repository for category persistence; must not be {@code null}
     * @param messageRepository   the repository used to check for message references before
     *                            deletion; must not be {@code null}
     */
    public CategoryService(CategoryRepository categoryRepository,
                           MessageRepository messageRepository) {
        this.categoryRepository = categoryRepository;
        this.messageRepository  = messageRepository;
    }

    /**
     * Returns all categories in insertion order.
     *
     * <p>This operation is read-only and does not modify any state.
     *
     * @return a list of all persisted {@link Category} entities; never {@code null}, may be empty
     */
    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    /**
     * Creates a new category from the given request.
     *
     * <p>The {@code code} field of the request must be unique across all categories.
     * If a category with the same {@code code} already exists, a 409 Conflict is returned.
     *
     * @param request  the category data to persist; must not be {@code null};
     *                 {@code request.code()} must be non-blank and unique
     * @return         the persisted {@link Category} with its generated {@code id} populated
     * @throws ResponseStatusException  with status 409 if a category with {@code request.code()}
     *                                  already exists
     */
    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category code already exists: " + request.code());
        }
        Category category = new Category();
        category.setName(request.name());
        category.setCode(request.code());
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    /**
     * Updates the name and description of an existing category.
     *
     * <p>The {@code code} is immutable and is used only as the lookup key to identify
     * the category to update. The {@code code} field of the request body is ignored.
     *
     * @param code     the unique business key of the category to update; must not be {@code null}
     * @param request  the new values for {@code name} and {@code description}; must not be
     *                 {@code null}
     * @return         the updated and persisted {@link Category}
     * @throws ResponseStatusException  with status 404 if no category with {@code code} exists
     */
    public Category update(String code, CategoryRequest request) {
        Category category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found: " + code));
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    /**
     * Deletes the category identified by the given code.
     *
     * <p>Deletion is blocked if any {@link com.emailssummarizer.apirs.message.Message} records
     * still reference this category. The caller must delete all associated messages before
     * removing the category.
     *
     * @param code  the unique business key of the category to delete; must not be {@code null}
     * @throws ResponseStatusException  with status 404 if no category with {@code code} exists
     * @throws ResponseStatusException  with status 409 if the category still has associated
     *                                  messages
     */
    public void delete(String code) {
        if (!categoryRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Category not found: " + code);
        }
        if (messageRepository.existsByCategoryCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete category with existing messages: " + code);
        }
        categoryRepository.deleteByCode(code);
    }
}
