package com.emailssummarizer.apirs.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Category} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository} plus
 * custom query methods that operate on the {@code code} business key. This repository
 * must only be called from {@link CategoryService} — controllers must not interact
 * with it directly.
 *
 * @see CategoryService
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Returns the category with the given business code, if it exists.
     *
     * @param code  the unique category code to look up; must not be {@code null}
     * @return      an {@link Optional} containing the matching {@link Category},
     *              or an empty {@link Optional} if none is found
     */
    Optional<Category> findByCode(String code);

    /**
     * Returns {@code true} if a category with the given code already exists in the database.
     *
     * <p>Used by the service layer to guard against duplicate-code conflicts before
     * attempting an insert or to verify existence before a delete.
     *
     * @param code  the unique category code to check; must not be {@code null}
     * @return      {@code true} if a category with {@code code} exists; {@code false} otherwise
     */
    boolean existsByCode(String code);

    /**
     * Deletes the category identified by the given business code.
     *
     * <p>This method performs a no-op if no category with the given code exists.
     * Callers should verify existence and check for associated messages via
     * {@link #existsByCode(String)} and
     * {@link com.emailssummarizer.apirs.message.MessageRepository#existsByCategoryCode(String)}
     * before invoking this method.
     *
     * @param code  the unique business key of the category to delete; must not be {@code null}
     */
    void deleteByCode(String code);
}
