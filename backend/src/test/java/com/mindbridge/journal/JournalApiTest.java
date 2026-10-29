package com.mindbridge.journal;

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
import org.springframework.test.web.servlet.MvcResult;

class JournalApiTest extends AbstractApiTest {

    private String createEntry(MockHttpSession session, String title, String category, String body)
            throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/journal",
                Map.of("title", title, "category", category, "body", body))).session(session))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createReadUpdateAndDeleteAnEntry() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        String id = createEntry(session, "First", "Study", "Studying for exams today");

        mockMvc.perform(get("/api/v1/journal/" + id).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.body").value("Studying for exams today"));

        mockMvc.perform(withCsrf(put("/api/v1/journal/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("title", "Edited", "category", "Study", "body", "Updated body"))))
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Edited"));

        mockMvc.perform(get("/api/v1/journal").session(session))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("Edited"));

        mockMvc.perform(withCsrf(delete("/api/v1/journal/" + id)).session(session))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/journal/" + id).session(session))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidBodiesAreRejected() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(withCsrf(jsonPost("/api/v1/journal",
                Map.of("title", "x", "category", "y", "body", "   "))).session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'body')]").isNotEmpty());

        mockMvc.perform(withCsrf(jsonPost("/api/v1/journal",
                Map.of("title", "x", "category", "y", "body", "a".repeat(10001)))).session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchAndCategoryFilterOnlyReturnMatchingOwnEntries() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        createEntry(session, "Exam week", "study", "I feel stressed about my exam");
        createEntry(session, "Walk", "wellbeing", "Long walk by the river");
        createEntry(session, "Plans", "study", "Weekly planning notes");

        mockMvc.perform(get("/api/v1/journal").param("q", "exam").session(session))
            .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/journal").param("q", "EXAM").session(session))
            .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/journal").param("category", "Study").session(session))
            .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/journal").param("q", "river").param("category", "study").session(session))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginationIsAppliedAndSizeIsCapped() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        for (int i = 0; i < 3; i++) {
            createEntry(session, "Entry " + i, "general", "Body " + i);
        }

        mockMvc.perform(get("/api/v1/journal").param("size", "2").session(session))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/journal").param("size", "2").param("page", "1").session(session))
            .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get("/api/v1/journal").param("size", "1000").session(session))
            .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void anotherUserCannotReadChangeOrDeleteMyEntry() throws Exception {
        MockHttpSession sessionA = registerAndLogin(uniqueEmail());
        MockHttpSession sessionB = registerAndLogin(uniqueEmail());
        String id = createEntry(sessionA, "Private", "general", "Only for me");

        mockMvc.perform(get("/api/v1/journal/" + id).session(sessionB))
            .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(put("/api/v1/journal/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("title", "Hacked", "category", "x", "body", "Changed"))))
                .session(sessionB))
            .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(delete("/api/v1/journal/" + id)).session(sessionB))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/journal").session(sessionB))
            .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/journal/" + id).session(sessionA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.body").value("Only for me"));
    }

    @Test
    void anonymousUsersAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/journal")).andExpect(status().isUnauthorized());
    }

    @Test
    void creatingWithoutCsrfTokenIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        mockMvc.perform(jsonPost("/api/v1/journal",
                Map.of("title", "x", "category", "y", "body", "z")).session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    void guidedPromptsAreAvailable() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        mockMvc.perform(get("/api/v1/journal/prompts").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(JournalPrompts.ALL.size()));
    }

    @Test
    void deletingTheAccountRemovesAllJournalEntries() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        createEntry(session, "One", "general", "First");
        createEntry(session, "Two", "general", "Second");

        mockMvc.perform(withCsrf(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", PASSWORD))))
                .session(session))
            .andExpect(status().isNoContent());

        Integer remaining = jdbc.queryForObject("SELECT count(*) FROM journal_entries", Integer.class);
        assertThat(remaining).isZero();
    }
}
