package com.mindbridge.mood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindbridge.AbstractApiTest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class MoodApiTest extends AbstractApiTest {

    private String createMood(MockHttpSession session, int mood, List<String> tags, Instant recordedAt)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("moodValue", mood);
        body.put("emotions", List.of("calm"));
        body.put("tags", tags);
        if (recordedAt != null) {
            body.put("recordedAt", recordedAt.toString());
        }
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/moods", body)).session(session))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void checkInIsStoredWithNormalizedEmotionsAndTags() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(withCsrf(jsonPost("/api/v1/moods", Map.of(
                "moodValue", 3,
                "emotions", List.of("Anxious", "anxious ", "Hopeful"),
                "tags", List.of(" Exam Stress ", "sleep"),
                "note", "A mixed day"))).session(session))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.moodValue").value(3))
            .andExpect(jsonPath("$.emotions.length()").value(2))
            .andExpect(jsonPath("$.emotions[0]").value("anxious"))
            .andExpect(jsonPath("$.tags[0]").value("exam stress"))
            .andExpect(jsonPath("$.note").value("A mixed day"));
    }

    @Test
    void invalidCheckInsAreRejected() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        for (int badValue : new int[] {0, 6}) {
            mockMvc.perform(withCsrf(jsonPost("/api/v1/moods", Map.of("moodValue", badValue))).session(session))
                .andExpect(status().isBadRequest());
        }
        mockMvc.perform(withCsrf(jsonPost("/api/v1/moods", Map.of(
                "moodValue", 3, "emotions", List.of("a", "b", "c", "d", "e", "f")))).session(session))
            .andExpect(status().isBadRequest());
        mockMvc.perform(withCsrf(jsonPost("/api/v1/moods", Map.of(
                "moodValue", 3, "recordedAt", Instant.now().plus(Duration.ofDays(1)).toString())))
                .session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listCanBeFilteredByTagAndDateRange() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        Instant now = Instant.now();
        createMood(session, 2, List.of("a"), now.minus(Duration.ofDays(3)));
        createMood(session, 3, List.of("b"), now.minus(Duration.ofDays(2)));
        createMood(session, 4, List.of("a", "b"), now.minus(Duration.ofDays(1)));

        mockMvc.perform(get("/api/v1/moods").param("tag", "a").session(session))
            .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/moods").param("tag", "B").session(session))
            .andExpect(jsonPath("$.totalElements").value(2));

        String from = now.minus(Duration.ofHours(60)).toString();
        mockMvc.perform(get("/api/v1/moods").param("from", from).session(session))
            .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/moods").param("from", from).param("tag", "a").session(session))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void summaryWithTooLittleDataDoesNotInventAverages() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        for (int i = 0; i < 3; i++) {
            createMood(session, 4, List.of("exam"), null);
        }

        mockMvc.perform(get("/api/v1/moods/summary").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entryCount").value(3))
            .andExpect(jsonPath("$.sufficientData").value(false))
            .andExpect(jsonPath("$.averageMood").value(nullValue()))
            .andExpect(jsonPath("$.direction").value(nullValue()))
            .andExpect(jsonPath("$.moodCounts['4']").value(3));
    }

    @Test
    void summaryWithEnoughDataReportsAveragesAndDirection() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        Instant now = Instant.now();
        // First half of the week: low moods. Second half: higher moods.
        createMood(session, 2, List.of("exam"), now.minus(Duration.ofDays(6)));
        createMood(session, 2, List.of("exam"), now.minus(Duration.ofDays(5)));
        createMood(session, 2, List.of("exam"), now.minus(Duration.ofDays(4)));
        createMood(session, 4, List.of("exam"), now.minus(Duration.ofDays(2)));
        createMood(session, 4, List.of("exam"), now.minus(Duration.ofDays(1)));
        createMood(session, 4, List.of("exam"), now.minus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/v1/moods/summary")
                .param("period", "WEEK").param("timezone", "Asia/Kolkata").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entryCount").value(6))
            .andExpect(jsonPath("$.sufficientData").value(true))
            .andExpect(jsonPath("$.averageMood").value(3.0))
            .andExpect(jsonPath("$.firstHalfAverage").value(2.0))
            .andExpect(jsonPath("$.secondHalfAverage").value(4.0))
            .andExpect(jsonPath("$.direction").value("HIGHER"))
            .andExpect(jsonPath("$.moodCounts['2']").value(3))
            .andExpect(jsonPath("$.topTags[0].name").value("exam"))
            .andExpect(jsonPath("$.topTags[0].count").value(6))
            .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));
    }

    @Test
    void anotherUserCannotSeeOrDeleteMyMoodEntries() throws Exception {
        MockHttpSession sessionA = registerAndLogin(uniqueEmail());
        MockHttpSession sessionB = registerAndLogin(uniqueEmail());
        String id = createMood(sessionA, 3, List.of("private"), null);

        mockMvc.perform(get("/api/v1/moods").session(sessionB))
            .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(withCsrf(delete("/api/v1/moods/" + id)).session(sessionB))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/moods/summary").session(sessionB))
            .andExpect(jsonPath("$.entryCount").value(0));

        mockMvc.perform(get("/api/v1/moods").session(sessionA))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void ownerCanDeleteAMoodEntry() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        String id = createMood(session, 3, List.of("x"), null);

        mockMvc.perform(withCsrf(delete("/api/v1/moods/" + id)).session(session))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/moods").session(session))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void anonymousUsersAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/moods")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/moods/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSummaryParametersAreRejected() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(get("/api/v1/moods/summary").param("timezone", "Mars/Base").session(session))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/moods/summary").param("period", "YEAR").session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deletingTheAccountRemovesMoodEntriesEmotionsAndTags() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        createMood(session, 3, List.of("tag-one", "tag-two"), null);

        mockMvc.perform(withCsrf(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", PASSWORD))))
                .session(session))
            .andExpect(status().isNoContent());

        for (String table : List.of("mood_entries", "mood_entry_emotions", "mood_entry_tags")) {
            Integer rows = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
            assertThat(rows).as(table).isZero();
        }
    }
}
