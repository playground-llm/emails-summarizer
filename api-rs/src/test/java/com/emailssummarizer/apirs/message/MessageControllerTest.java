package com.emailssummarizer.apirs.message;

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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc test for {@link MessageController}.
 * Verifies HTTP status codes, request mapping, and JSON serialisation.
 * Security filters are excluded — authorization rules are covered by SecurityFilterChainTest.
 * {@link MessageService} is mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    MessageService messageService;

    @InjectMocks
    MessageController messageController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
    }

    @Test
    void listMessages_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        given(messageService.listByCategoryCode("INBOX"))
                .willReturn(List.of(message(id, "Subject", "Body", "INBOX")));

        mockMvc.perform(get("/messages?categoryCode=INBOX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Subject"));
    }

    @Test
    void createMessage_returnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        given(messageService.create(any())).willReturn(message(id, "Hello", "Body", "INBOX"));

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hello","body":"Body","categoryCode":"INBOX"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Hello"));
    }

    @Test
    void updateMessage_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        given(messageService.update(eq(id), any()))
                .willReturn(message(id, "Updated", "New body", "INBOX"));

        mockMvc.perform(put("/messages/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated","body":"New body","categoryCode":"INBOX"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deleteMessage_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/messages/" + id))
                .andExpect(status().isNoContent());

        verify(messageService).delete(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Message message(UUID id, String title, String body, String categoryCode) {
        Message m = new Message();
        m.setId(id);
        m.setTitle(title);
        m.setBody(body);
        m.setCategoryCode(categoryCode);
        return m;
    }
}
