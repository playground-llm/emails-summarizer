package com.emailssummarizer.apirs.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Proxies the GitHub OAuth2 Authorization Code → access_token exchange.
 *
 * <p>GitHub's token endpoint does not emit CORS headers, so a browser cannot call it
 * directly. The UI sends the authorization code to {@code POST /oauth2/token} on this
 * controller, which performs the server-to-server exchange and returns only the
 * {@code access_token} to the browser — keeping the {@code client_secret} on the
 * server at all times.
 *
 * <p>This endpoint is permitted without authentication (no Bearer token required)
 * so that the UI can obtain a token before it has one.
 *
 * <p>Request / response contract:
 * <pre>
 * POST /oauth2/token  Content-Type: application/json
 * { "code": "...", "redirectUri": "..." }
 *
 * 200 OK  Content-Type: application/json
 * { "accessToken": "gho_..." }
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

    /**
     * Immutable DTO representing the JSON body sent by the UI to request a token exchange.
     *
     * @param code         the GitHub authorization code received by the UI after the OAuth2
     *                     redirect; must not be {@code null}
     * @param redirectUri  the redirect URI used during the authorization request; must match
     *                     the value registered with the GitHub OAuth App exactly
     */
    public record TokenRequest(String code, String redirectUri) {}

    /**
     * Immutable DTO representing the JSON response returned to the UI after a successful
     * token exchange.
     *
     * @param accessToken  the GitHub access token that the UI should store and send as
     *                     a {@code Authorization: Bearer} header on subsequent API calls
     */
    public record TokenResponse(String accessToken) {}

    /**
     * Handles {@code POST /oauth2/token} requests to exchange a GitHub authorization
     * code for an access token.
     *
     * <p>Calls {@value #GITHUB_TOKEN_URL} server-side with the configured
     * {@code client_id} and {@code client_secret}, then extracts and returns the
     * {@code access_token} from GitHub's response. The {@code client_secret} is never
     * exposed to the browser.
     *
     * @param request  the request body containing the authorization {@code code} and
     *                 the {@code redirectUri}; must not be {@code null}
     * @return         a {@link TokenResponse} containing the GitHub access token
     * @throws RuntimeException  if GitHub does not return an {@code access_token}
     *                           (e.g. the code is invalid, expired, or already used)
     */
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
