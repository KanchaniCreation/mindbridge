package com.mindbridge.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.mindbridge.MatchFixtures;
import com.mindbridge.match.Topic;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class ChatApiTest extends MatchFixtures {

    private MvcResult sendRaw(MockHttpSession session, String chatId, UUID clientId, String text) throws Exception {
        return mockMvc.perform(withCsrf(jsonPost("/api/v1/chats/" + chatId + "/messages",
                Map.of("clientMessageId", clientId, "body", text))).session(session)).andReturn();
    }

    private String send(MockHttpSession session, String chatId, String text) throws Exception {
        MvcResult result = sendRaw(session, chatId, UUID.randomUUID(), text);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void meetingPeopleNeedsAnAdultConfirmationFirst() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        assertThat(putProfile(session, profileBody("adult_check", true, Topic.STRESS))
            .getResponse().getStatus()).isEqualTo(200);

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(jsonPath("$.adultConfirmed").value(false));
        mockMvc.perform(get("/api/v1/match/discover").session(session))
            .andExpect(status().isForbidden());

        confirmAdult(session);

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(jsonPath("$.adultConfirmed").value(true));
        mockMvc.perform(get("/api/v1/match/discover").session(session))
            .andExpect(status().isOk());
    }

    @Test
    void aMatchCreatesAChatWhereEachPersonSeesTheOthersAlias() throws Exception {
        MockHttpSession first = openUser("first_person", Topic.SLEEP);
        MockHttpSession second = openUser("second_person", Topic.SLEEP);

        JsonNode chat = startChat(first, second);

        assertThat(chat.toString()).as("no email or user id in the response").doesNotContain("@");
        assertThat(chat.get("myPseudonym").asText()).isEqualTo("second_person");
        assertThat(chat.get("partnerPseudonym").asText()).isEqualTo("first_person");

        MvcResult firstList = mockMvc.perform(get("/api/v1/chats").session(first))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andReturn();
        JsonNode listed = objectMapper.readTree(firstList.getResponse().getContentAsString()).get(0);
        assertThat(listed.get("myPseudonym").asText()).isEqualTo("first_person");
        assertThat(listed.get("partnerPseudonym").asText()).isEqualTo("second_person");
    }

    @Test
    void messagesReachThePartnerWithoutAnyIdentity() throws Exception {
        MockHttpSession creator = adultUser();
        MockHttpSession joiner = adultUser();
        JsonNode chat = startChat(creator, joiner);
        String chatId = chat.get("id").asText();
        String creatorPseudonym = chat.get("partnerPseudonym").asText();

        send(creator, chatId, "hello there");

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/messages").session(joiner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].body").value("hello there"))
            .andExpect(jsonPath("$.items[0].mine").value(false))
            .andExpect(jsonPath("$.items[0].senderPseudonym").value(creatorPseudonym))
            .andExpect(jsonPath("$.items[0].senderId").doesNotExist());
        mockMvc.perform(get("/api/v1/chats/" + chatId + "/messages").session(creator))
            .andExpect(jsonPath("$.items[0].mine").value(true));
    }

    @Test
    void sendingTheSameClientMessageIdTwiceCreatesOneMessage() throws Exception {
        MockHttpSession creator = adultUser();
        JsonNode chat = startChat(creator, adultUser());
        String chatId = chat.get("id").asText();
        UUID clientId = UUID.randomUUID();

        MvcResult first = sendRaw(creator, chatId, clientId, "only once");
        MvcResult second = sendRaw(creator, chatId, clientId, "only once");

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getStatus()).isEqualTo(201);
        assertThat(objectMapper.readTree(second.getResponse().getContentAsString()).get("id"))
            .isEqualTo(objectMapper.readTree(first.getResponse().getContentAsString()).get("id"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_messages", Integer.class)).isEqualTo(1);
    }

    @Test
    void aStrangerCannotReadSendStreamCloseOrReport() throws Exception {
        MockHttpSession creator = adultUser();
        JsonNode chat = startChat(creator, adultUser());
        String chatId = chat.get("id").asText();
        MockHttpSession stranger = adultUser();

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/messages").session(stranger))
            .andExpect(status().isNotFound());
        assertThat(sendRaw(stranger, chatId, UUID.randomUUID(), "let me in").getResponse().getStatus())
            .isEqualTo(404);
        mockMvc.perform(get("/api/v1/chats/" + chatId + "/stream").session(stranger))
            .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(post("/api/v1/chats/" + chatId + "/close")).session(stranger))
            .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(jsonPost("/api/v1/chats/" + chatId + "/report", Map.of("reason", "SPAM")))
                .session(stranger))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/chats").session(stranger))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void messagesArePagedNewestFirst() throws Exception {
        MockHttpSession creator = adultUser();
        String chatId = startChat(creator, adultUser()).get("id").asText();
        send(creator, chatId, "one");
        send(creator, chatId, "two");
        send(creator, chatId, "three");

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/messages").param("size", "2").session(creator))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].body").value("three"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void aClosedChatStaysReadableButRejectsNewMessages() throws Exception {
        MockHttpSession creator = adultUser();
        MockHttpSession joiner = adultUser();
        String chatId = startChat(creator, joiner).get("id").asText();
        send(creator, chatId, "before closing");

        mockMvc.perform(withCsrf(post("/api/v1/chats/" + chatId + "/close")).session(creator))
            .andExpect(status().isNoContent());

        assertThat(sendRaw(joiner, chatId, UUID.randomUUID(), "too late").getResponse().getStatus())
            .isEqualTo(409);
        mockMvc.perform(get("/api/v1/chats/" + chatId + "/messages").session(joiner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void reportingStoresTheReasonAndClosesTheChat() throws Exception {
        MockHttpSession creator = adultUser();
        MockHttpSession joiner = adultUser();
        String chatId = startChat(creator, joiner).get("id").asText();

        mockMvc.perform(withCsrf(jsonPost("/api/v1/chats/" + chatId + "/report",
                Map.of("reason", "HARASSMENT", "note", "Not respectful"))).session(joiner))
            .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT reason FROM chat_reports", String.class)).isEqualTo("HARASSMENT");
        assertThat(jdbc.queryForObject("SELECT status FROM chats", String.class)).isEqualTo("CLOSED");
        assertThat(sendRaw(creator, chatId, UUID.randomUUID(), "hello?").getResponse().getStatus())
            .isEqualTo(409);
    }

    @Test
    void sendingTooManyMessagesTooQuicklyIsLimited() throws Exception {
        MockHttpSession creator = adultUser();
        String chatId = startChat(creator, adultUser()).get("id").asText();

        for (int i = 0; i < 30; i++) {
            assertThat(sendRaw(creator, chatId, UUID.randomUUID(), "message " + i).getResponse().getStatus())
                .isEqualTo(201);
        }

        assertThat(sendRaw(creator, chatId, UUID.randomUUID(), "one too many").getResponse().getStatus())
            .isEqualTo(429);
    }

    @Test
    void invalidMessagesAreRejected() throws Exception {
        MockHttpSession creator = adultUser();
        String chatId = startChat(creator, adultUser()).get("id").asText();

        assertThat(sendRaw(creator, chatId, UUID.randomUUID(), "   ").getResponse().getStatus()).isEqualTo(400);
        assertThat(sendRaw(creator, chatId, UUID.randomUUID(), "a".repeat(2001)).getResponse().getStatus())
            .isEqualTo(400);
        mockMvc.perform(withCsrf(jsonPost("/api/v1/chats/" + chatId + "/messages", Map.of("body", "no id")))
                .session(creator))
            .andExpect(status().isBadRequest());
    }

    @Test
    void membersCanOpenALiveStream() throws Exception {
        MockHttpSession creator = adultUser();
        String chatId = startChat(creator, adultUser()).get("id").asText();

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/stream").session(creator))
            .andExpect(request().asyncStarted());
    }

    @Test
    void anonymousVisitorsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/chats")).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAnAccountRemovesMyMessagesAndClosesTheChatForThePartner() throws Exception {
        MockHttpSession creator = adultUser();
        MockHttpSession joiner = adultUser();
        String chatId = startChat(creator, joiner).get("id").asText();
        send(creator, chatId, "from the creator");
        send(joiner, chatId, "from the joiner");

        deleteAccount(creator);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_messages", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM chats", String.class)).isEqualTo("CLOSED");
        mockMvc.perform(get("/api/v1/chats").session(joiner))
            .andExpect(jsonPath("$[0].status").value("CLOSED"))
            .andExpect(jsonPath("$[0].partnerPseudonym").value("Former participant"));
        mockMvc.perform(get("/api/v1/chats/" + chatId + "/messages").session(joiner))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].body").value("from the joiner"));

        deleteAccount(joiner);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM chats", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_messages", Integer.class)).isZero();
    }
}
