package com.emailssummarizer.apirs.category;

import com.emailssummarizer.apirs.security.GitHubOpaqueTokenIntrospector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CategoryRepository}.
 * Verifies custom query methods against the H2 in-memory database.
 * Each test runs in a transaction that is rolled back after completion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class CategoryRepositoryTest {

    @Autowired
    CategoryRepository repository;

    @MockitoBean
    GitHubOpaqueTokenIntrospector introspector;

    @Test
    void findByCode_returnsCategory() {
        repository.save(category("INBOX", "Inbox"));

        assertThat(repository.findByCode("INBOX"))
                .isPresent()
                .get()
                .extracting(Category::getName)
                .isEqualTo("Inbox");
    }

    @Test
    void existsByCode_trueForExisting_falseForMissing() {
        repository.save(category("WORK", "Work"));

        assertThat(repository.existsByCode("WORK")).isTrue();
        assertThat(repository.existsByCode("MISSING")).isFalse();
    }

    @Test
    void deleteByCode_removesRecord() {
        repository.save(category("PERSONAL", "Personal"));

        repository.deleteByCode("PERSONAL");

        assertThat(repository.existsByCode("PERSONAL")).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Category category(String code, String name) {
        Category c = new Category();
        c.setCode(code);
        c.setName(name);
        return c;
    }
}
