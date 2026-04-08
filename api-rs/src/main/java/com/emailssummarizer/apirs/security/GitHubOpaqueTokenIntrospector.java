package com.emailssummarizer.apirs.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Validates GitHub access tokens by calling the GitHub user-info endpoint.
 * A 200 response confirms the token is valid; any other response rejects it.
 */
@Component
public class GitHubOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final RestClient restClient;

    public GitHubOpaqueTokenIntrospector() {
        this.restClient = RestClient.builder()
                .baseUrl(GITHUB_USER_URL)
                .build();
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo;
        try {
            userInfo = restClient.get()
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            throw new BadOpaqueTokenException("Invalid GitHub token: " + ex.getMessage(), ex);
        }

        if (userInfo == null || !userInfo.containsKey("login")) {
            throw new BadOpaqueTokenException("GitHub token introspection returned no user info");
        }

        System.out.println("\n\n\n\n\n\nUserInfo: " + userInfo + "\n\n\n\n\n\n");

        String login = (String) userInfo.get("login");
        Object id = userInfo.getOrDefault("id", 0);
        Object name = userInfo.getOrDefault("name", login);
        if (name == null) {
            name = login;
        }
        Map<String, Object> attributes = Map.of(
                "login", login,
                "id", id,
                "name", name
        );

        return new GitHubPrincipal(login, attributes);
    }

    /**
     * Simple OAuth2AuthenticatedPrincipal backed by GitHub user attributes.
     */
    static class GitHubPrincipal implements OAuth2AuthenticatedPrincipal {

        private final String name;
        private final Map<String, Object> attributes;

        GitHubPrincipal(String name, Map<String, Object> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return List.of();
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
