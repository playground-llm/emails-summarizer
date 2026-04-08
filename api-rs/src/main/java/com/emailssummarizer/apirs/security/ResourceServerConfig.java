package com.emailssummarizer.apirs.security;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the OAuth2 Resource Server.
 *
 * <p>Configures a stateless security filter chain that:
 * <ul>
 *   <li>Validates incoming Bearer tokens via {@link GitHubOpaqueTokenIntrospector}.</li>
 *   <li>Enforces role-based access control on all category and message endpoints.</li>
 *   <li>Permits {@code POST /oauth2/token} and the H2 console without authentication.</li>
 *   <li>Applies CORS rules that allow only the known Vue UI origins.</li>
 * </ul>
 *
 * <p>Session management is set to {@code STATELESS} — no HTTP session is created or used;
 * every request must carry a valid Bearer token (except permit-all paths).
 *
 * @see GitHubOpaqueTokenIntrospector
 */
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    private final GitHubOpaqueTokenIntrospector introspector;

    /**
     * Constructs a {@code ResourceServerConfig} with the required token introspector.
     *
     * @param introspector  the component that validates GitHub Bearer tokens and assigns
     *                      roles; must not be {@code null}
     */
    public ResourceServerConfig(GitHubOpaqueTokenIntrospector introspector) {
        this.introspector = introspector;
    }

    /**
     * Builds and returns the primary {@link SecurityFilterChain} for the application.
     *
     * <p>Authorization rules applied in order:
     * <ol>
     *   <li>{@code /h2-console/**} and {@code POST /oauth2/token} — permit all (no token).</li>
     *   <li>{@code DELETE /categories/**}, {@code DELETE /messages/**} — requires
     *       {@code ROLE_DEL}.</li>
     *   <li>{@code POST /categories}, {@code POST /messages},
     *       {@code PUT /categories/**}, {@code PUT /messages/**} — requires
     *       {@code ROLE_EDIT}.</li>
     *   <li>{@code GET /categories}, {@code GET /messages} — requires {@code ROLE_READ}.</li>
     *   <li>All other requests — requires authentication (any valid token).</li>
     * </ol>
     *
     * @param http  the {@link HttpSecurity} builder provided by Spring Security
     * @return      the configured {@link SecurityFilterChain}
     * @throws Exception  if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public paths — no token required
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/oauth2/token").permitAll()
                // DELETE requires ROLE_DEL
                .requestMatchers(HttpMethod.DELETE, "/categories/**", "/messages/**").hasRole("DEL")
                // POST and PUT require ROLE_EDIT
                .requestMatchers(HttpMethod.POST,   "/categories",    "/messages").hasRole("EDIT")
                .requestMatchers(HttpMethod.PUT,     "/categories/**", "/messages/**").hasRole("EDIT")
                // GET requires ROLE_READ
                .requestMatchers(HttpMethod.GET,     "/categories",    "/messages").hasRole("READ")
                // Everything else (e.g. future endpoints) still requires authentication
                .anyRequest().authenticated()
            )
            // Configure as OAuth2 Resource Server with opaque token introspection
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaque -> opaque.introspector(introspector))
            )
            // Allow H2 console to render in a frame
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    /**
     * Returns the CORS configuration applied to all endpoints ({@code /**}).
     *
     * <p>Allows requests from the known Vue UI origins served on localhost or
     * {@code local.example.com} on ports 5500 and 8000. Credentials
     * ({@code allowCredentials}) are explicitly disabled — authentication is carried
     * via the {@code Authorization: Bearer} header, not cookies.
     *
     * @return a {@link CorsConfigurationSource} that permits {@code GET}, {@code POST},
     *         {@code PUT}, {@code DELETE}, and {@code OPTIONS} from the configured origins
     *         with {@code Authorization} and {@code Content-Type} headers
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow the Vue UI origin — update if the ui is served on a different port
        config.setAllowedOrigins(List.of(
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://localhost:8000",
            "http://127.0.0.1:8000",
            "http://local.example.com:5500"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
