package com.emailssummarizer.apirs.category;

import com.emailssummarizer.apirs.message.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.*;

/**
 * Unit tests for {@link CategoryService}.
 * Exercises business logic, validation, and error conditions in isolation
 * using Mockito mocks — no Spring context loaded.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    CategoryService categoryService;

    @Test
    void listAll_returnsList() {
        given(categoryRepository.findAll()).willReturn(List.of(new Category()));

        assertThat(categoryService.listAll()).hasSize(1);
    }

    @Test
    void create_success() {
        given(categoryRepository.existsByCode("WORK")).willReturn(false);
        given(categoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        Category result = categoryService.create(new CategoryRequest("Work", "WORK", "desc"));

        assertThat(result.getCode()).isEqualTo("WORK");
        assertThat(result.getName()).isEqualTo("Work");
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        given(categoryRepository.existsByCode("WORK")).willReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Work", "WORK", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }

    @Test
    void update_success() {
        Category existing = new Category();
        existing.setCode("WORK");
        existing.setName("Work");
        given(categoryRepository.findByCode("WORK")).willReturn(Optional.of(existing));
        given(categoryRepository.save(any())).willAnswer(i -> i.getArgument(0));

        Category result = categoryService.update("WORK", new CategoryRequest("Work Updated", "WORK", "new desc"));

        assertThat(result.getName()).isEqualTo("Work Updated");
        assertThat(result.getDescription()).isEqualTo("new desc");
    }

    @Test
    void update_notFound_throws404() {
        given(categoryRepository.findByCode("MISSING")).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update("MISSING", new CategoryRequest("X", "MISSING", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_success() {
        given(categoryRepository.existsByCode("WORK")).willReturn(true);
        given(messageRepository.existsByCategoryCode("WORK")).willReturn(false);

        categoryService.delete("WORK");

        verify(categoryRepository).deleteByCode("WORK");
    }

    @Test
    void delete_notFound_throws404() {
        given(categoryRepository.existsByCode("MISSING")).willReturn(false);

        assertThatThrownBy(() -> categoryService.delete("MISSING"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_hasMessages_throwsConflict() {
        given(categoryRepository.existsByCode("INBOX")).willReturn(true);
        given(messageRepository.existsByCategoryCode("INBOX")).willReturn(true);

        assertThatThrownBy(() -> categoryService.delete("INBOX"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(CONFLICT);
    }
}
