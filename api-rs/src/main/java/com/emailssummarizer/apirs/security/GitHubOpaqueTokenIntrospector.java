package com.emailssummarizer.apirs.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Validates GitHub access tokens by calling the GitHub user-info endpoint.
 * A 200 response confirms the token is valid; any other response rejects it.
 *
 * Assigns ROLE_READ, ROLE_EDIT, and/or ROLE_DEL to the principal based on
 * three independent allow-lists configured via environment variables.
 */
@Component
public class GitHubOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger log = LoggerFactory.getLogger(GitHubOpaqueTokenIntrospector.class);
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final RestClient restClient;
    private final Set<String> readers;
    private final Set<String> editors;
    private final Set<String> deleters;

    public GitHubOpaqueTokenIntrospector(
            @Value("${app.readers:}") String readersRaw,
            @Value("${app.editors:}") String editorsRaw,
            @Value("${app.deleters:}") String deletersRaw) {
        this.restClient = RestClient.builder().baseUrl(GITHUB_USER_URL).build();
        this.readers  = parseLogins(readersRaw);
        this.editors  = parseLogins(editorsRaw);
        this.deleters = parseLogins(deletersRaw);

        if (readers.isEmpty())  log.warn("READERS_GITHUB_LOGINS is empty — GET /categories and GET /messages will be denied for all users");
        if (editors.isEmpty())  log.warn("EDITORS_GITHUB_LOGINS is empty — POST/PUT on /categories and /messages will be denied for all users");
        if (deleters.isEmpty()) log.warn("DELETERS_GITHUB_LOGINS is empty — DELETE on /categories and /messages will be denied for all users");
    }

    private static Set<String> parseLogins(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Map<String, Object> userInfo;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(Map.class);
            userInfo = response;
        } catch (Exception ex) {
            throw new BadOpaqueTokenException("Invalid GitHub token: " + ex.getMessage(), ex);
        }

        if (userInfo == null || !userInfo.containsKey("login")) {
            throw new BadOpaqueTokenException("GitHub token introspection returned no user info");
        }

        String login = ((String) userInfo.get("login")).toLowerCase();
        Object id    = userInfo.getOrDefault("id", 0);
        Object name  = userInfo.getOrDefault("name", login);
        if (name == null) name = login;

        Map<String, Object> attributes = Map.of("login", login, "id", id, "name", name);

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (readers.contains(login))  authorities.add(new SimpleGrantedAuthority("ROLE_READ"));
        if (editors.contains(login))  authorities.add(new SimpleGrantedAuthority("ROLE_EDIT"));
        if (deleters.contains(login)) authorities.add(new SimpleGrantedAuthority("ROLE_DEL"));

        return new GitHubPrincipal(login, attributes, authorities);
    }

    /**
     * Simple OAuth2AuthenticatedPrincipal backed by GitHub user attributes.
     */
    static class GitHubPrincipal implements OAuth2AuthenticatedPrincipal {

        private final String name;
        private final Map<String, Object> attributes;
        private final List<GrantedAuthority> authorities;

        GitHubPrincipal(String name, Map<String, Object> attributes, List<GrantedAuthority> authorities) {
            this.name        = name;
            this.attributes  = attributes;
            this.authorities = authorities;
        }

        @Override public Map<String, Object> getAttributes() { return attributes; }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
        @Override public String getName() { return name; }
    }
}
