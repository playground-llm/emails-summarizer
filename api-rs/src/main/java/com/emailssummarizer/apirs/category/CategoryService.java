package com.emailssummarizer.apirs.category;

import com.emailssummarizer.apirs.message.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for the Category feature slice.
 * Owns all validation, conflict detection, and orchestration.
 * The only class allowed to call {@link CategoryRepository}.
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MessageRepository messageRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           MessageRepository messageRepository) {
        this.categoryRepository = categoryRepository;
        this.messageRepository  = messageRepository;
    }

    /**
     * Returns all categories in insertion order.
     */
    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    /**
     * Creates a new category.
     *
     * @throws ResponseStatusException 409 if the code is already taken.
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
     * The code is immutable and used only as the lookup key.
     *
     * @throws ResponseStatusException 404 if no category exists with the given code.
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
     * Deletes a category by code.
     *
     * @throws ResponseStatusException 404 if not found.
     * @throws ResponseStatusException 409 if messages still reference this category.
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
