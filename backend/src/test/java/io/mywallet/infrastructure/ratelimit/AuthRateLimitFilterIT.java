package io.mywallet.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Overrides the rate limit to a low, test-specific threshold (the global test config
 * disables it entirely - see test/resources/application.yml - so this class needs its own
 * property override, which also gives it its own cached Spring context and avoids
 * polluting the rate limiter's shared state for every other integration test class).
 */
@TestPropertySource(properties = {
    "mywallet.ratelimit.auth.max-requests=3",
    "mywallet.ratelimit.auth.window-ms=60000"
})
class AuthRateLimitFilterIT extends PostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void exceedingTheAuthRateLimitReturns429() throws Exception {
        MockMvc mvc = mockMvc();
        String body = objectMapper.writeValueAsString(new RegisterRequest("ratelimit-probe@mywallet.dev", "whatever-password"));

        // First 3 requests consume the window (regardless of their individual outcome -
        // duplicate email on attempts 2 and 3 doesn't matter, the limiter counts requests,
        // not successes).
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
        }

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
