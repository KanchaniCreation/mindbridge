package com.mindbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.mindbridge.match.Topic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** Shared helpers for tests that need people who can match and chat. */
public abstract class MatchFixtures extends AbstractApiTest {

    private final Map<MockHttpSession, String> aliases = new HashMap<>();

    protected void confirmAdult(MockHttpSession session) throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/chat/adult-confirmation")).session(session))
            .andExpect(status().isNoContent());
    }

    protected Map<String, Object> profileBody(String alias, boolean open, Topic... topics) {
        Map<String, Object> body = new HashMap<>();
        body.put("alias", alias);
        body.put("about", "Hello from " + alias);
        body.put("topics", Arrays.stream(topics).map(Enum::name).toList());
        body.put("openToChat", open);
        return body;
    }

    protected MvcResult putProfile(MockHttpSession session, Map<String, Object> body) throws Exception {
        return mockMvc.perform(withCsrf(put("/api/v1/match/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))).session(session))
            .andReturn();
    }

    /** A logged-in adult with a profile that is open to chat. */
    protected MockHttpSession openUser(String alias, Topic... topics) throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        confirmAdult(session);
        assertThat(putProfile(session, profileBody(alias, true, topics)).getResponse().getStatus())
            .isEqualTo(200);
        aliases.put(session, alias);
        return session;
    }

    protected String randomAlias() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    protected MockHttpSession adultUser() throws Exception {
        return openUser(randomAlias(), Topic.STRESS);
    }

    protected String aliasOf(MockHttpSession session) {
        return aliases.get(session);
    }

    protected MvcResult answerRaw(MockHttpSession session, String alias, String decision) throws Exception {
        return mockMvc.perform(withCsrf(jsonPost("/api/v1/match/decisions",
                Map.of("alias", alias, "decision", decision))).session(session))
            .andReturn();
    }

    protected JsonNode answer(MockHttpSession session, String alias, String decision) throws Exception {
        MvcResult result = answerRaw(session, alias, decision);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** First person chooses Match, then the second does too. Returns the chat as the second sees it. */
    protected JsonNode startChat(MockHttpSession first, MockHttpSession second) throws Exception {
        assertThat(answer(first, aliasOf(second), "MATCH").get("matched").asBoolean()).isFalse();
        JsonNode result = answer(second, aliasOf(first), "MATCH");
        assertThat(result.get("matched").asBoolean()).isTrue();
        return result.get("chat");
    }

    protected JsonNode discover(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/match/discover").session(session))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected List<String> aliasesIn(JsonNode cards) {
        List<String> found = new ArrayList<>();
        cards.forEach(card -> found.add(card.get("alias").asText()));
        return found;
    }

    protected void deleteAccount(MockHttpSession session) throws Exception {
        mockMvc.perform(withCsrf(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", PASSWORD))))
                .session(session))
            .andExpect(status().isNoContent());
    }
}
