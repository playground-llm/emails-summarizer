package com.emailssummarizer.apirs.message;

import com.emailssummarizer.apirs.category.Category;
import com.emailssummarizer.apirs.category.CategoryRepository;
import com.emailssummarizer.apirs.security.GitHubOpaqueTokenIntrospector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link MessageRepository}.
 * Verifies custom query methods against the H2 in-memory database.
 * Each test runs in a transaction that is rolled back after completion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class MessageRepositoryTest {

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @MockitoBean
    GitHubOpaqueTokenIntrospector introspector;

    @BeforeEach
    void seedCategory() {
        Category cat = new Category();
        cat.setName("Inbox");
        cat.setCode("INBOX");
        categoryRepository.save(cat);
    }

    @Test
    void findByCategoryCode_returnsMatchingMessages() {
        messageRepository.save(message("Hello", "INBOX"));
        messageRepository.save(message("World", "INBOX"));

        assertThat(messageRepository.findByCategoryCode("INBOX")).hasSize(2);
        assertThat(messageRepository.findByCategoryCode("WORK")).isEmpty();
    }

    @Test
    void existsByCategoryCode_trueWhenMessagesExist() {
        messageRepository.save(message("Hi", "INBOX"));

        assertThat(messageRepository.existsByCategoryCode("INBOX")).isTrue();
        assertThat(messageRepository.existsByCategoryCode("WORK")).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Message message(String title, String categoryCode) {
        Message m = new Message();
        m.setTitle(title);
        m.setCategoryCode(categoryCode);
        return m;
    }
}
