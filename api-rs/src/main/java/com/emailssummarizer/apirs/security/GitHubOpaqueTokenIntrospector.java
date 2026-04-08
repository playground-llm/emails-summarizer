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
 * Validates GitHub opaque access tokens and assigns Spring Security granted authorities.
 *
 * <p>Calls {@code GET https://api.github.com/user} with the Bearer token to confirm
 * validity and retrieve the authenticated user's GitHub {@code login}. The login is
 * matched (case-insensitively) against three independent allow-lists to assign
 * {@code ROLE_READ}, {@code ROLE_EDIT}, and/or {@code ROLE_DEL}.
 *
 * <p>Allow-lists are injected from {@code app.readers}, {@code app.editors}, and
 * {@code app.deleters} application properties, which are bound from environment variables
 * {@code READERS_GITHUB_LOGINS}, {@code EDITORS_GITHUB_LOGINS}, and
 * {@code DELETERS_GITHUB_LOGINS} respectively. Each variable is a comma-separated list
 * of GitHub login names. Roles are independent — a user must appear in each list
 * separately to hold multiple roles.
 *
 * <p>Warnings are logged at startup if any allow-list is empty, since this would deny
 * all requests for the corresponding HTTP methods.
 *
 * @see ResourceServerConfig
 */
@Component
public class GitHubOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger log = LoggerFactory.getLogger(GitHubOpaqueTokenIntrospector.class);
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final RestClient restClient;
    private final Set<String> readers;
    private final Set<String> editors;
    private final Set<String> deleters;

    /**
     * Constructs the introspector, parsing and normalising each allow-list at startup.
     *
     * <p>Empty or blank values for any allow-list are accepted and result in no users
     * being granted the corresponding role; a warning is logged in each such case.
     *
     * @param readersRaw   comma-separated GitHub logins that should receive {@code ROLE_READ};
     *                     may be empty or blank
     * @param editorsRaw   comma-separated GitHub logins that should receive {@code ROLE_EDIT};
     *                     may be empty or blank
     * @param deletersRaw  comma-separated GitHub logins that should receive {@code ROLE_DEL};
     *                     may be empty or blank
     */
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

    /**
     * Parses a comma-separated string of GitHub login names into a lower-cased set.
     *
     * @param raw  the raw comma-separated string from configuration; may be {@code null}
     *             or blank
     * @return     an immutable set of lower-cased, trimmed login names; empty if {@code raw}
     *             is blank or {@code null}
     */
    private static Set<String> parseLogins(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * Introspects the given opaque GitHub token by calling the GitHub user-info endpoint.
     *
     * <p>Sends a {@code GET https://api.github.com/user} request with the token as a
     * Bearer credential. On success, extracts the GitHub {@code login} and builds a
     * {@link GitHubPrincipal} with the applicable granted authorities based on the
     * configured allow-lists.
     *
     * @param token  the raw GitHub access token to validate; must not be {@code null}
     * @return       an {@link OAuth2AuthenticatedPrincipal} representing the authenticated
     *               GitHub user with zero or more of {@code ROLE_READ}, {@code ROLE_EDIT},
     *               {@code ROLE_DEL} granted authorities
     * @throws BadOpaqueTokenException  if the token is invalid, expired, or the GitHub
     *                                  user-info endpoint returns a non-200 response, or
     *                                  if the response does not contain a {@code login} field
     */
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
     * Minimal {@link OAuth2AuthenticatedPrincipal} backed by GitHub user attributes.
     *
     * <p>Holds the GitHub {@code login} as the principal name along with a subset of
     * user attributes ({@code login}, {@code id}, {@code name}) and the granted
     * authorities assigned by the allow-list checks in
     * {@link GitHubOpaqueTokenIntrospector#introspect(String)}.
     */
    static class GitHubPrincipal implements OAuth2AuthenticatedPrincipal {

        private final String name;
        private final Map<String, Object> attributes;
        private final List<GrantedAuthority> authorities;

        /**
         * Constructs a {@code GitHubPrincipal} with the given identity and authorities.
         *
         * @param name        the GitHub login of the authenticated user; must not be {@code null}
         * @param attributes  a map of user attributes ({@code login}, {@code id}, {@code name});
         *                    must not be {@code null}
         * @param authorities the list of granted authorities assigned to this principal;
         *                    must not be {@code null}, may be empty
         */
        GitHubPrincipal(String name, Map<String, Object> attributes, List<GrantedAuthority> authorities) {
            this.name        = name;
            this.attributes  = attributes;
            this.authorities = authorities;
        }

        /**
         * Returns the GitHub user attributes associated with this principal.
         *
         * @return a map containing at least {@code login}, {@code id}, and {@code name};
         *         never {@code null}
         */
        @Override
        public Map<String, Object> getAttributes() { return attributes; }

        /**
         * Returns the granted authorities assigned to this principal.
         *
         * @return a collection of zero or more authorities from
         *         {@code ROLE_READ}, {@code ROLE_EDIT}, {@code ROLE_DEL};
         *         never {@code null}
         */
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

        /**
         * Returns the GitHub login of the authenticated user, used as the principal name.
         *
         * @return the lower-cased GitHub login; never {@code null}
         */
        @Override
        public String getName() { return name; }
    }
}
