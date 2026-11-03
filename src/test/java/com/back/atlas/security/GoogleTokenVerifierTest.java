package com.back.atlas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(OutputCaptureExtension.class)
class GoogleTokenVerifierTest {

    private static final String TOKENINFO = "https://oauth2.googleapis.com/tokeninfo?access_token=tok";
    private static final String USERINFO = "https://www.googleapis.com/oauth2/v3/userinfo";

    private MockRestServiceServer server;
    private GoogleTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new GoogleTokenVerifier(builder, "our-client-id");
    }

    @Test
    void acceptsTokenIssuedToOurAppAndReadsIdentityFromGoogle() {
        server.expect(requestTo(TOKENINFO))
                .andRespond(withSuccess("{\"aud\":\"our-client-id\",\"sub\":\"42\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERINFO)).andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("""
                        {"sub":"42","email":"ada@example.com","email_verified":true,
                         "given_name":"Ada","family_name":"Lovelace"}""", MediaType.APPLICATION_JSON));

        GoogleTokenVerifier.GoogleUser user = verifier.verify("tok");

        assertThat(user.email()).isEqualTo("ada@example.com");
        assertThat(user.givenName()).isEqualTo("Ada");
    }

    @Test
    void rejectsTokenIssuedToAnotherApp() {
        server.expect(requestTo(TOKENINFO))
                .andRespond(withSuccess("{\"aud\":\"someone-else\",\"sub\":\"42\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("tok")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsUnverifiedEmail() {
        server.expect(requestTo(TOKENINFO))
                .andRespond(withSuccess("{\"aud\":\"our-client-id\",\"sub\":\"42\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERINFO)).andRespond(withSuccess(
                "{\"sub\":\"42\",\"email\":\"x@example.com\",\"email_verified\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("tok")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void networkErrorsDoNotLogTheToken(CapturedOutput output) {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?access_token=secret-token"))
                .andRespond(withException(new IOException("connection reset")));

        assertThatThrownBy(() -> verifier.verify("secret-token")).isInstanceOf(ResponseStatusException.class);
        assertThat(output).contains("Google token verification failed").doesNotContain("secret-token");
    }

    @Test
    void rejectsInvalidOrMissingToken() {
        server.expect(requestTo(TOKENINFO)).andRespond(withStatus(BAD_REQUEST));

        assertThatThrownBy(() -> verifier.verify("tok")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> verifier.verify(" ")).isInstanceOf(ResponseStatusException.class);
    }
}
