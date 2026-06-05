package io.mywallet.auth.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.auth.interfaces.rest.dto.LoginRequest;
import io.mywallet.auth.interfaces.rest.dto.RefreshRequest;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * End-to-end auth tests through the real HTTP layer (not just the service), so that
 * SecurityConfig's filter chain, the JWT filter, and GlobalExceptionHandler's error
 * shapes are all exercised exactly as a real client would hit them.
 */
class AuthControllerIT extends PostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void registerThenLoginThenAccessProtectedEndpoint() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "investor+" + System.nanoTime() + "@mywallet.dev";

        String registerBody = objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"));
        String registerResponse = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(registerResponse).get("accessToken").asText();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.roles[0]").value("INVESTOR"));
    }

    @Test
    void loginWithWrongPasswordAndLoginWithUnknownEmailReturnTheSameGenericError() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "investor2+" + System.nanoTime() + "@mywallet.dev";
        mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"))));

        String wrongPasswordBody = objectMapper.writeValueAsString(new LoginRequest(email, "totally-wrong-password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongPasswordBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));

        String unknownEmailBody = objectMapper.writeValueAsString(new LoginRequest("does-not-exist@mywallet.dev", "whatever"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(unknownEmailBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshTokenRotatesAndRejectsReuseOfTheOldOne() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "investor3+" + System.nanoTime() + "@mywallet.dev";
        String registerResponse = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"))))
            .andReturn().getResponse().getContentAsString();
        String firstRefreshToken = objectMapper.readTree(registerResponse).get("refreshToken").asText();

        String refreshBody = objectMapper.writeValueAsString(new RefreshRequest(firstRefreshToken));
        String refreshedResponse = mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String secondRefreshToken = objectMapper.readTree(refreshedResponse).get("refreshToken").asText();

        org.assertj.core.api.Assertions.assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        // Reusing the already-rotated first token must be rejected.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void accessingProtectedEndpointWithoutTokenIsRejected() throws Exception {
        mockMvc().perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "duplicate+" + System.nanoTime() + "@mywallet.dev";
        String body = objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void accountLocksAfterFiveFailedAttemptsAndRejectsEvenTheCorrectPasswordWhileLocked() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "lockout+" + System.nanoTime() + "@mywallet.dev";
        String correctPassword = "a-strong-password-123";

        mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterRequest(email, correctPassword))));

        String wrongPasswordBody = objectMapper.writeValueAsString(new LoginRequest(email, "totally-wrong-password"));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongPasswordBody))
                .andExpect(status().isUnauthorized());
        }

        // The account is now locked - even the CORRECT password must be rejected, with
        // the exact same generic error (no "account locked" message, to avoid confirming
        // to a prober that they hit the lockout threshold on a real account).
        String correctPasswordBody = objectMapper.writeValueAsString(new LoginRequest(email, correctPassword));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(correctPasswordBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
