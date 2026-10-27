package com.mindbridge.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindbridge.AbstractApiTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class UserAccountTest extends AbstractApiTest {

    @Test
    void twoUsersOnlyEverSeeTheirOwnProfile() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        MockHttpSession sessionA = registerAndLogin(emailA);
        MockHttpSession sessionB = registerAndLogin(emailB);

        mockMvc.perform(get("/api/v1/users/me").session(sessionA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(emailA));
        mockMvc.perform(get("/api/v1/users/me").session(sessionB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(emailB));
    }

    @Test
    void profileNameCanBeUpdated() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(withCsrf(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"New Name\"}")).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("New Name"));

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    @Test
    void preferencesDefaultToOffAndCanBeUpdated() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(get("/api/v1/users/me/preferences").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.preferredStyle").value("NO_PREFERENCE"))
            .andExpect(jsonPath("$.remindersEnabled").value(false));

        mockMvc.perform(withCsrf(put("/api/v1/users/me/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredStyle\":\"PRACTICAL\",\"quietStart\":\"22:00\","
                    + "\"quietEnd\":\"07:00\",\"remindersEnabled\":true}")).session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me/preferences").session(session))
            .andExpect(jsonPath("$.preferredStyle").value("PRACTICAL"))
            .andExpect(jsonPath("$.remindersEnabled").value(true));
    }

    @Test
    void preferencesRejectHalfSetQuietHours() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(withCsrf(put("/api/v1/users/me/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredStyle\":\"PRACTICAL\",\"quietStart\":\"22:00\","
                    + "\"remindersEnabled\":false}")).session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteWithWrongPasswordIsRefusedAndKeepsTheAccount() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(withCsrf(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "wrong-password-123"))))
                .session(session))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(status().isOk());
    }

    @Test
    void deleteRemovesAllOfMyDataAndNobodyElses() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        MockHttpSession sessionA = registerAndLogin(emailA);
        MockHttpSession sessionB = registerAndLogin(emailB);

        mockMvc.perform(withCsrf(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", PASSWORD))))
                .session(sessionA))
            .andExpect(status().isNoContent());

        Integer usersA = jdbc.queryForObject(
            "SELECT count(*) FROM users WHERE email = ?", Integer.class, emailA);
        Integer allPreferences = jdbc.queryForObject(
            "SELECT count(*) FROM user_preferences", Integer.class);
        assertThat(usersA).isZero();
        assertThat(allPreferences).as("only B's preferences row remains").isEqualTo(1);

        mockMvc.perform(get("/api/v1/users/me").session(sessionA))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").session(sessionB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(emailB));
    }
}
