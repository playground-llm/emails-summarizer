package com.emailssummarizer.apirs.message;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<Message> listMessages(@RequestParam String categoryCode) {
        return messageService.listByCategoryCode(categoryCode);
    }

    @PostMapping
    public ResponseEntity<Message> createMessage(@RequestBody MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.create(request));
    }

    @PutMapping("/{id}")
    public Message updateMessage(@PathVariable UUID id,
                                 @RequestBody MessageRequest request) {
        return messageService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID id) {
        messageService.delete(id);
    }
}
