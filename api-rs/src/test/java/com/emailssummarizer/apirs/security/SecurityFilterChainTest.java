package com.emailssummarizer.apirs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring Boot context test that verifies per-endpoint authorization rules
 * declared in {@link ResourceServerConfig}.
 *
 * <p>Covers:
 * <ul>
 *   <li>401 — no token provided</li>
 *   <li>403 — authenticated but missing required role</li>
 * </ul>
 */
@SpringBootTest
class SecurityFilterChainTest {

    @Autowired
    WebApplicationContext ctx;

    @MockitoBean
    GitHubOpaqueTokenIntrospector introspector;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ── 401 — no token ───────────────────────────────────────────────────────

    @Test
    void getCategories_noToken_returns401() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMessages_noToken_returns401() throws Exception {
        mockMvc.perform(get("/messages?categoryCode=INBOX"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postCategories_noToken_returns401() throws Exception {
        mockMvc.perform(post("/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCategory_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/categories/INBOX"))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 — wrong role ─────────────────────────────────────────────────────

    @Test
    void postCategories_readRoleOnly_returns403() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void putCategory_readRoleOnly_returns403() throws Exception {
        mockMvc.perform(put("/categories/INBOX")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategory_editRoleOnly_returns403() throws Exception {
        mockMvc.perform(delete("/categories/INBOX")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_EDIT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postMessages_readRoleOnly_returns403() throws Exception {
        mockMvc.perform(post("/messages")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void putMessage_readRoleOnly_returns403() throws Exception {
        mockMvc.perform(put("/messages/00000000-0000-0000-0000-000000000001")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMessage_editRoleOnly_returns403() throws Exception {
        mockMvc.perform(delete("/messages/00000000-0000-0000-0000-000000000001")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_EDIT"))))
                .andExpect(status().isForbidden());
    }
}
