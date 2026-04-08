package com.emailssummarizer.apirs.message;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that exposes the message management API under {@code /messages}.
 *
 * <p>Handles HTTP request/response mapping only; all business logic is delegated to
 * {@link MessageService}. This class must not interact with any repository directly.
 *
 * <p>Requires the following roles depending on the HTTP method:
 * <ul>
 *   <li>{@code ROLE_READ} — {@code GET /messages}</li>
 *   <li>{@code ROLE_EDIT} — {@code POST /messages}, {@code PUT /messages/{id}}</li>
 *   <li>{@code ROLE_DEL}  — {@code DELETE /messages/{id}}</li>
 * </ul>
 *
 * @see MessageService
 */
@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    /**
     * Constructs a {@code MessageController} with its required service dependency.
     *
     * @param messageService  the service handling message business logic; must not be {@code null}
     */
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Handles {@code GET /messages?categoryCode={code}} requests to list messages
     * belonging to a specific category.
     *
     * <p>Delegates to {@link MessageService#listByCategoryCode(String)}.
     * Requires {@code ROLE_READ}. Returns an empty list if the category has no messages;
     * returns 400 if the {@code categoryCode} query parameter is absent.
     *
     * @param categoryCode  the code of the category whose messages to retrieve;
     *                      must not be {@code null}
     * @return              a list of {@link Message} entities belonging to the specified
     *                      category; never {@code null}, may be empty
     */
    @GetMapping
    public List<Message> listMessages(@RequestParam String categoryCode) {
        return messageService.listByCategoryCode(categoryCode);
    }

    /**
     * Handles {@code POST /messages} requests to create a new message.
     *
     * <p>Delegates to {@link MessageService#create(MessageRequest)} and returns the
     * created resource with HTTP 201 Created. Requires {@code ROLE_EDIT}.
     *
     * @param request  the request body containing {@code title}, {@code body}, and
     *                 {@code categoryCode}; validated by the service layer
     * @return         a {@link ResponseEntity} with status 201 and the persisted
     *                 {@link Message} as the body; or 409 if the {@code categoryCode}
     *                 does not reference an existing category
     */
    @PostMapping
    public ResponseEntity<Message> createMessage(@RequestBody MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.create(request));
    }

    /**
     * Handles {@code PUT /messages/{id}} requests to update an existing message.
     *
     * <p>Delegates to {@link MessageService#update(UUID, MessageRequest)}.
     * Requires {@code ROLE_EDIT}.
     *
     * @param id       the UUID of the message to update
     * @param request  the request body with the new {@code title} and {@code body}
     * @return         the updated {@link Message}; or 404 if no message with {@code id} exists
     */
    @PutMapping("/{id}")
    public Message updateMessage(@PathVariable UUID id,
                                 @RequestBody MessageRequest request) {
        return messageService.update(id, request);
    }

    /**
     * Handles {@code DELETE /messages/{id}} requests to remove a message.
     *
     * <p>Delegates to {@link MessageService#delete(UUID)} and responds with
     * HTTP 204 No Content on success. Requires {@code ROLE_DEL}.
     *
     * @param id  the UUID of the message to delete; returns 404 if no such message exists
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID id) {
        messageService.delete(id);
    }
}
