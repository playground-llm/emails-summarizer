package com.emailssummarizer.apirs.message;

import com.emailssummarizer.apirs.category.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for all message business logic.
 *
 * <p>Acts as the sole entry point for message mutations and queries within the application.
 * Validates that a referenced category exists before creating a message, and prevents
 * deletion or update of messages that cannot be found.
 *
 * <p>Controllers must call this service rather than interacting with
 * {@link MessageRepository} directly.
 *
 * @see MessageController
 * @see MessageRepository
 */
@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Constructs a {@code MessageService} with its required repository dependencies.
     *
     * @param messageRepository   the repository for message persistence; must not be {@code null}
     * @param categoryRepository  the repository used to verify that a referenced category
     *                            exists; must not be {@code null}
     */
    public MessageService(MessageRepository messageRepository,
                          CategoryRepository categoryRepository) {
        this.messageRepository  = messageRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all messages belonging to the given category code.
     *
     * <p>This operation is read-only and does not modify any state. Returns an empty
     * list if the category exists but has no messages, or if the category code is unknown.
     *
     * @param categoryCode  the code of the category to filter messages by;
     *                      must not be {@code null}
     * @return              a list of {@link Message} entities for the given category;
     *                      never {@code null}, may be empty
     */
    @Transactional(readOnly = true)
    public List<Message> listByCategoryCode(String categoryCode) {
        return messageRepository.findByCategoryCode(categoryCode);
    }

    /**
     * Creates a new message from the given request.
     *
     * <p>The {@code categoryCode} in the request must reference an existing
     * {@link com.emailssummarizer.apirs.category.Category}. If it does not,
     * a 409 Conflict is returned.
     *
     * @param request  the message data to persist; must not be {@code null};
     *                 {@code request.categoryCode()} must reference an existing category
     * @return         the persisted {@link Message} with its generated {@link UUID} populated
     * @throws ResponseStatusException  with status 409 if {@code request.categoryCode()}
     *                                  does not match any existing category
     */
    public Message create(MessageRequest request) {
        if (!categoryRepository.existsByCode(request.categoryCode())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Category not found: " + request.categoryCode());
        }
        Message message = new Message();
        message.setTitle(request.title());
        message.setBody(request.body());
        message.setCategoryCode(request.categoryCode());
        return messageRepository.save(message);
    }

    /**
     * Updates the title and body of an existing message.
     *
     * <p>Only {@code title} and {@code body} are updated; the {@code categoryCode}
     * of an existing message cannot be changed through this method.
     *
     * @param id       the UUID of the message to update; must not be {@code null}
     * @param request  the new values for {@code title} and {@code body};
     *                 must not be {@code null}
     * @return         the updated and persisted {@link Message}
     * @throws ResponseStatusException  with status 404 if no message with {@code id} exists
     */
    public Message update(UUID id, MessageRequest request) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Message not found: " + id));
        message.setTitle(request.title());
        message.setBody(request.body());
        return messageRepository.save(message);
    }

    /**
     * Deletes the message identified by the given UUID.
     *
     * @param id  the UUID of the message to delete; must not be {@code null}
     * @throws ResponseStatusException  with status 404 if no message with {@code id} exists
     */
    public void delete(UUID id) {
        if (!messageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Message not found: " + id);
        }
        messageRepository.deleteById(id);
    }
}
