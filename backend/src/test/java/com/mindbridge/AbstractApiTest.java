package com.mindbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Shared helpers: real PostgreSQL (Testcontainers), real security filter chain, real CSRF cookie flow. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class AbstractApiTest {

    protected static final String PASSWORD = "correct-horse-battery";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    protected String csrfToken;

    @BeforeEach
    void resetDatabaseAndFetchCsrfToken() throws Exception {
        jdbc.update("DELETE FROM users");
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isNoContent())
            .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("server must set the XSRF-TOKEN cookie").isNotNull();
        csrfToken = cookie.getValue();
    }

    /** Behaves like the browser app: sends the CSRF cookie and echoes it in the header. */
    protected MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder builder) {
        return builder.header("X-XSRF-TOKEN", csrfToken).cookie(new Cookie("XSRF-TOKEN", csrfToken));
    }

    protected MockHttpServletRequestBuilder jsonPost(String url, Object body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected void register(String email) throws Exception {
        mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/register", Map.of(
                "email", email, "password", PASSWORD, "displayName", "Test User", "acceptedTerms", true))))
            .andExpect(status().isCreated());
    }

    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/auth/login",
                Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).as("login must create a session").isNotNull();
        return session;
    }

    protected MockHttpSession registerAndLogin(String email) throws Exception {
        register(email);
        return login(email, PASSWORD);
    }
}
