package com.back.atlas.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies a Google OAuth access token server-side.
 * The client only sends the token: identity data (email, sub, name) is read from Google,
 * never trusted from the request body, and the token must have been issued to our app.
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

    private final RestClient restClient;
    private final String clientId;

    public GoogleTokenVerifier(RestClient.Builder builder,
                               @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {
        this.restClient = builder
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2HttpMessageConverter());
                })
                .build();
        this.clientId = clientId;
    }

    public GoogleUser verify(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw unauthorized();
        }
        try {
            TokenInfo tokenInfo = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?access_token={token}", accessToken)
                    .retrieve()
                    .body(TokenInfo.class);

            // A token issued to another app must not log in to ours (confused deputy).
            if (tokenInfo == null || !clientId.equals(tokenInfo.audience())) {
                log.warn("Google token rejected: wrong audience");
                throw unauthorized();
            }

            GoogleUser user = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUser.class);

            if (user == null || !user.emailVerified() || !tokenInfo.sub().equals(user.sub())) {
                log.warn("Google token rejected: unverified email or subject mismatch");
                throw unauthorized();
            }
            return user;
        } catch (RestClientException e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw unauthorized();
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google authentication failed");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenInfo(@JsonProperty("aud") String audience, String sub) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleUser(
            String sub,
            String email,
            @JsonProperty("email_verified") boolean emailVerified,
            String name,
            @JsonProperty("given_name") String givenName,
            @JsonProperty("family_name") String familyName,
            String picture
    ) {
    }
}
