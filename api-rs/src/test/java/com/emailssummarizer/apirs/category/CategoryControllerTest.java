package com.emailssummarizer.apirs.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc test for {@link CategoryController}.
 * Verifies HTTP status codes, request mapping, and JSON serialisation.
 * Security filters are excluded — authorization rules are covered by SecurityFilterChainTest.
 * {@link CategoryService} is mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    CategoryService categoryService;

    @InjectMocks
    CategoryController categoryController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test
    void listCategories_returnsOk() throws Exception {
        given(categoryService.listAll()).willReturn(List.of(category("INBOX", "Inbox", null)));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("INBOX"));
    }

    @Test
    void createCategory_returnsCreated() throws Exception {
        given(categoryService.create(any())).willReturn(category("WORK", "Work", "Work items"));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Work","code":"WORK","description":"Work items"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WORK"));
    }

    @Test
    void updateCategory_returnsOk() throws Exception {
        given(categoryService.update(eq("WORK"), any()))
                .willReturn(category("WORK", "Work Updated", "Updated"));

        mockMvc.perform(put("/categories/WORK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Work Updated","code":"WORK","description":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work Updated"));
    }

    @Test
    void deleteCategory_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/categories/WORK"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete("WORK");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Category category(String code, String name, String description) {
        Category c = new Category();
        c.setCode(code);
        c.setName(name);
        c.setDescription(description);
        return c;
    }
}
