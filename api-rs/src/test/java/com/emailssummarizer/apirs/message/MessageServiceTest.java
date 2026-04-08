package com.emailssummarizer.apirs.message;

import com.emailssummarizer.apirs.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.*;

/**
 * Unit tests for {@link MessageService}.
 * Exercises business logic, validation, and error conditions in isolation
 * using Mockito mocks — no Spring context loaded.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    MessageRepository messageRepository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    MessageService messageService;

    @Test
    void listByCategoryCode_returnsList() {
        given(messageRepository.findByCategoryCode("INBOX")).willReturn(List.of(new Message()));

        assertThat(messageService.listByCategoryCode("INBOX")).hasSize(1);
    }

    @Test
    void create_success() {
        given(categoryRepository.existsByCode("INBOX")).willReturn(true);
        given(messageRepository.save(any())).willAnswer(i -> i.getArgument(0));

        Message result = messageService.create(new MessageRequest("Hello", "Body", "INBOX"));

        assertThat(result.getTitle()).isEqualTo("Hello");
        assertThat(result.getCategoryCode()).isEqualTo("INBOX");
    }

    @Test
    void create_categoryNotFound_throws404() {
        given(categoryRepository.existsByCode("MISSING")).willReturn(false);

        assertThatThrownBy(() -> messageService.create(new MessageRequest("Hi", "Body", "MISSING")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void update_success() {
        UUID id = UUID.randomUUID();
        Message existing = new Message();
        existing.setId(id);
        existing.setTitle("Old");
        given(messageRepository.findById(id)).willReturn(Optional.of(existing));
        given(messageRepository.save(any())).willAnswer(i -> i.getArgument(0));

        Message result = messageService.update(id, new MessageRequest("New", "New body", "INBOX"));

        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getBody()).isEqualTo("New body");
    }

    @Test
    void update_notFound_throws404() {
        UUID id = UUID.randomUUID();
        given(messageRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.update(id, new MessageRequest("X", "Y", "Z")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_success() {
        UUID id = UUID.randomUUID();
        given(messageRepository.existsById(id)).willReturn(true);

        messageService.delete(id);

        verify(messageRepository).deleteById(id);
    }

    @Test
    void delete_notFound_throws404() {
        UUID id = UUID.randomUUID();
        given(messageRepository.existsById(id)).willReturn(false);

        assertThatThrownBy(() -> messageService.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }
}
