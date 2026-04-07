package com.emailssummarizer.apirs.message;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public List<Message> listMessages(@RequestParam String categoryCode) {
        return messageRepository.findByCategoryCode(categoryCode);
    }
}
