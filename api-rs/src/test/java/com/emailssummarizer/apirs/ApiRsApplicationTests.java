package com.emailssummarizer.apirs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.emailssummarizer.apirs.security.GitHubOpaqueTokenIntrospector;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 */
@SpringBootTest
class ApiRsApplicationTests {

    @MockitoBean
    GitHubOpaqueTokenIntrospector introspector;

    @Test
    void contextLoads() {
    }
}
