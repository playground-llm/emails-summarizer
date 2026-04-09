package com.emailssummarizer.apirs.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Message} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository} plus
 * custom query methods that operate on the {@code categoryCode} foreign key. This
 * repository must only be called from {@link MessageService} and
 * {@link com.emailssummarizer.apirs.category.CategoryService} — controllers must
 * not interact with it directly.
 *
 * @see MessageService
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Returns all messages that belong to the given category code.
     *
     * @param categoryCode  the code of the category to filter by; must not be {@code null}
     * @return              a list of {@link Message} entities with a matching
     *                      {@code categoryCode}; never {@code null}, may be empty
     */
    List<Message> findByCategoryCode(String categoryCode);

    /**
     * Returns {@code true} if at least one message references the given category code.
     *
     * <p>Used by {@link com.emailssummarizer.apirs.category.CategoryService} to enforce
     * the constraint that a category cannot be deleted while it still has associated messages.
     *
     * @param categoryCode  the category code to check; must not be {@code null}
     * @return              {@code true} if one or more messages exist with this
     *                      {@code categoryCode}; {@code false} otherwise
     */
    boolean existsByCategoryCode(String categoryCode);
}
