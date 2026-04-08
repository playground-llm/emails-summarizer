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

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    private final GitHubOpaqueTokenIntrospector introspector;

    public ResourceServerConfig(GitHubOpaqueTokenIntrospector introspector) {
        this.introspector = introspector;
    }

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
