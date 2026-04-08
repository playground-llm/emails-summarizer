package com.emailssummarizer.apirs.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Proxies the GitHub OAuth2 Authorization Code → access_token exchange.
 *
 * <p>GitHub's token endpoint does not emit CORS headers, so a browser cannot
 * call it directly. The UI sends the authorization code here; this controller
 * does the server-to-server call and returns only the access_token to the
 * browser — keeping the client_secret on the server side at all times.
 *
 * <p>POST /oauth2/token
 * <pre>
 * Request body (JSON):
 *   { "code": "...", "redirect_uri": "..." }
 *
 * Response body (JSON):
 *   { "access_token": "gho_..." }
 * </pre>
 */
@RestController
@RequestMapping("/oauth2")
public class OAuthController {

    private static final String GITHUB_TOKEN_URL =
            "https://github.com/login/oauth/access_token";

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    public record TokenRequest(String code, String redirectUri) {}

    public record TokenResponse(String accessToken) {}

    @PostMapping(value = "/token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse exchangeToken(@RequestBody TokenRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> githubResponse = restClient.post()
                .uri(GITHUB_TOKEN_URL)
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", request.code(),
                        "redirect_uri", request.redirectUri()
                ))
                .retrieve()
                .body(Map.class);

        if (githubResponse == null || !githubResponse.containsKey("access_token")) {
            throw new RuntimeException("GitHub token exchange failed: " + githubResponse);
        }

        return new TokenResponse((String) githubResponse.get("access_token"));
    }
}
