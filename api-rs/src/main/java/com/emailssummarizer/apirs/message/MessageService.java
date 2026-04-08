package com.emailssummarizer.apirs.message;

import com.emailssummarizer.apirs.category.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for the Message feature slice.
 * Owns all validation, FK checks, and orchestration.
 * The only class allowed to call {@link MessageRepository}.
 */
@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final CategoryRepository categoryRepository;

    public MessageService(MessageRepository messageRepository,
                          CategoryRepository categoryRepository) {
        this.messageRepository  = messageRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all messages belonging to the given category code.
     */
    @Transactional(readOnly = true)
    public List<Message> listByCategoryCode(String categoryCode) {
        return messageRepository.findByCategoryCode(categoryCode);
    }

    /**
     * Creates a new message.
     *
     * @throws ResponseStatusException 404 if the referenced category does not exist.
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
     * @throws ResponseStatusException 404 if no message exists with the given id.
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
     * Deletes a message by id.
     *
     * @throws ResponseStatusException 404 if not found.
     */
    public void delete(UUID id) {
        if (!messageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Message not found: " + id);
        }
        messageRepository.deleteById(id);
    }
}
