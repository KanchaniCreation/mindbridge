package com.mindbridge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindbridge.AbstractApiTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class AuthFlowTest extends AbstractApiTest {

    @Test
    void registerStoresBcryptHashAndDefaultPreferences() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/register", Map.of(
                "email", email, "password", PASSWORD, "displayName", "Test", "acceptedTerms", true))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String hash = jdbc.queryForObject(
            "SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);

        Integer prefs = jdbc.queryForObject(
            "SELECT count(*) FROM user_preferences p JOIN users u ON u.id = p.user_id WHERE u.email = ?",
            Integer.class, email);
        assertThat(prefs).isEqualTo(1);
    }

    @Test
    void registerDuplicateEmailIgnoringCaseReturns409() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/register", Map.of(
                "email", email.toUpperCase(), "password", PASSWORD, "acceptedTerms", true))))
            .andExpect(status().isConflict());
    }

    @Test
    void registerWithShortPasswordReturns400WithFieldError() throws Exception {
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/register", Map.of(
                "email", uniqueEmail(), "password", "short", "acceptedTerms", true))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").isNotEmpty());
    }

    @Test
    void registerRequiresAcceptance() throws Exception {
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/register", Map.of(
                "email", uniqueEmail(), "password", PASSWORD, "acceptedTerms", false))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'acceptedTerms')]").isNotEmpty());
    }

    @Test
    void loginCreatesSessionThatCanReadOwnProfile() throws Exception {
        String email = uniqueEmail();
        register(email);

        // Email matching is case-insensitive.
        MockHttpSession session = login(email.toUpperCase(), PASSWORD);

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameGenericError() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", "not-the-right-password"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                Map.of("email", uniqueEmail(), "password", PASSWORD))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void protectedEndpointWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(jsonPost("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD)))
            .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(withCsrf(post("/api/v1/auth/logout")).session(session))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsBlockedAfterFiveFailedAttempts() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                    Map.of("email", email, "password", "wrong-password-" + i))))
                .andExpect(status().isUnauthorized());
        }

        // Even the correct password is refused while the block is active.
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isTooManyRequests());
    }
}
