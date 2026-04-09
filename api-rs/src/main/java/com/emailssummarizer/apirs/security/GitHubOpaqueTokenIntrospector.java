package com.emailssummarizer.apirs.security;

import com.emailssummarizer.apirs.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Validates GitHub opaque access tokens and assigns Spring Security granted authorities
 * based on roles stored in the database.
 *
 * <p>Calls {@code GET https://api.github.com/user} with the Bearer token to confirm
 * validity and retrieve the authenticated user's GitHub profile ({@code login},
 * {@code id}, {@code name}, {@code avatar_url}).
 *
 * <p>The GitHub {@code login} is then passed to {@link UserService#findOrRegister},
 * which either returns the user's existing roles from the {@code ROLES} table or —
 * if this is the user's first login — creates a new entry in {@code USERS} and assigns
 * {@code ROLE_READ} automatically.
 *
 * @see ResourceServerConfig
 * @see UserService
 */
@Component
public class GitHubOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger log = LoggerFactory.getLogger(GitHubOpaqueTokenIntrospector.class);
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final RestClient restClient;
    private final UserService userService;

    /**
     * Constructs the introspector with the user service used for role resolution.
     *
     * @param userService  service that registers new users and retrieves roles;
     *                     must not be {@code null}
     */
    public GitHubOpaqueTokenIntrospector(UserService userService) {
        this.restClient  = RestClient.builder().baseUrl(GITHUB_USER_URL).build();
        this.userService = userService;
    }

    /**
     * Introspects the given opaque GitHub token by calling the GitHub user-info endpoint.
     *
     * <p>Sends a {@code GET https://api.github.com/user} request with the token as a
     * Bearer credential. On success, extracts the GitHub {@code login} and delegates to
     * {@link UserService#findOrRegister} to obtain the applicable Spring Security roles.
     * New users receive {@code ROLE_READ} automatically on their first authentication.
     *
     * @param token  the raw GitHub access token to validate; must not be {@code null}
     * @return       an {@link OAuth2AuthenticatedPrincipal} representing the authenticated
     *               GitHub user with the roles stored in the database
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

        String login     = ((String) userInfo.get("login")).toLowerCase();
        Long   githubId  = tolong(userInfo.get("id"));
        String name      = userInfo.get("name") != null ? (String) userInfo.get("name") : login;
        String avatarUrl = (String) userInfo.get("avatar_url");

        List<String> roles = userService.findOrRegister(login, githubId, name, avatarUrl);
        log.debug("Introspected token for '{}', roles={}", login, roles);

        List<GrantedAuthority> authorities = roles.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        Map<String, Object> attributes = Map.of(
                "login",      login,
                "id",         githubId != null ? githubId : 0L,
                "name",       name,
                "avatar_url", avatarUrl != null ? avatarUrl : ""
        );

        return new GitHubPrincipal(login, attributes, authorities);
    }

    /**
     * Safely converts a numeric value from the GitHub API response to a {@code Long}.
     *
     * <p>GitHub IDs arrive as JSON numbers and are deserialised as {@code Integer} or
     * {@code Long} depending on their magnitude; this helper handles both cases.
     *
     * @param value  the raw value from the user-info map; may be {@code null}
     * @return       the value as a {@code Long}, or {@code null} if {@code value} is
     *               {@code null} or not a {@link Number}
     */
    private static Long tolong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return null;
    }

    /**
     * Minimal {@link OAuth2AuthenticatedPrincipal} backed by GitHub user attributes.
     *
     * <p>Holds the GitHub {@code login} as the principal name along with a subset of
     * user attributes ({@code login}, {@code id}, {@code name}, {@code avatar_url}) and
     * the granted authorities resolved from the database by
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
         * @param attributes  a map of user attributes ({@code login}, {@code id}, {@code name},
         *                    {@code avatar_url}); must not be {@code null}
         * @param authorities the list of granted authorities resolved from the DB;
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
         * @return a map containing at least {@code login}, {@code id}, {@code name}, and
         *         {@code avatar_url}; never {@code null}
         */
        @Override
        public Map<String, Object> getAttributes() { return attributes; }

        /**
         * Returns the granted authorities assigned to this principal.
         *
         * @return a collection of one or more authorities from
         *         {@code ROLE_READ}, {@code ROLE_EDIT}, {@code ROLE_DEL};
         *         never {@code null} or empty (every user has at least {@code ROLE_READ})
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
