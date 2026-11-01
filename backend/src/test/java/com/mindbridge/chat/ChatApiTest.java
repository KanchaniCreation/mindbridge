package com.mindbridge.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.mindbridge.AbstractApiTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class ChatApiTest extends AbstractApiTest {

    private void confirmAdult(MockHttpSession session) throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/chat/adult-confirmation")).session(session))
            .andExpect(status().isNoContent());
    }

    private MockHttpSession adultUser() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        confirmAdult(session);
        return session;
    }

    private String createInvite(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/chat/invites")).session(session))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asText();
    }

    private JsonNode redeem(MockHttpSession session, String code) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(jsonPost("/api/v1/chat/invites/redeem",
                Map.of("code", code))).session(session))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** A creates an invite, B redeems it. Returns the chat summary as B sees it. */
    private JsonNode startChat(MockHttpSession creator, MockHttpSession joiner) throws Exception {
        return redeem(joiner, createInvite(creator));
    }

    private MvcResult sendRaw(MockHttpSession session, String chatId, UUID clientId, String text) throws Exception {
        return mockMvc.perform(withCsrf(jsonPost("/api/v1/chats/" + chatId + "/messages",
                Map.of("clientMessageId", clientId, "body", text))).session(session)).andReturn();
    }

    private String send(MockHttpSession session, String chatId, String text) throws Exception {
        MvcResult result = sendRaw(session, chatId, UUID.randomUUID(), text);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void deleteAccount(MockHttpSession session) throws Exception {
        mockMvc.perform(withCsrf(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", PASSWORD))))
                .session(session))
            .andExpect(status().isNoContent());
    }

    @Test
    void chatNeedsAnAdultConfirmationFirst() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(jsonPath("$.adultConfirmed").value(false));
        mockMvc.perform(withCsrf(post("/api/v1/chat/invites")).session(session))
            .andExpect(status().isForbidden());

        confirmAdult(session);

        mockMvc.perform(get("/api/v1/users/me").session(session))
            .andExpect(jsonPath("$.adultConfirmed").value(true));
        mockMvc.perform(withCsrf(post("/api/v1/chat/invites")).session(session))
            .andExpect(status().isCreated());
    }

    @Test
    void inviteCreatesAPseudonymousChatForBothPeople() throws Exception {
        MockHttpSession creator = adultUser();
        MockHttpSession joiner = adultUser();
        String code = createInvite(creator);

        JsonNode chat = redeem(joiner, code);

        assertThat(chat.toString()).as("no email or user id in the response").doesNotContain("@");
        assertThat(chat.get("myPseudonym").asText()).isNotEqualTo(chat.get("partnerPseudonym").asText());

        MvcResult creatorList = mockMvc.perform(get("/api/v1/chats").session(creator))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andReturn();
        JsonNode first = objectMapper.readTree(creatorList.getResponse().getContentAsString()).get(0);
        assertThat(first.get("myPseudonym").asText()).isEqualTo(chat.get("partnerPseudonym").asText());
        assertThat(first.get("partnerPseudonym").asText()).isEqualTo(chat.get("myPseudonym").asText());

        // Only a hash of the code is stored, never the code itself.
        Integer rawRows = jdbc.queryForObject(
            "SELECT count(*) FROM chat_invites WHERE code_hash = ?", Integer.class, InviteCodes.normalize(code));
        Integer hashedRows = jdbc.queryForObject(
            "SELECT count(*) FROM chat_invites WHERE code_hash = ?", Integer.class,
            InviteCodes.hash(InviteCodes.normalize(code)));
        assertThat(rawRows).isZero();
        assertThat(hashedRows).isEqualTo(1);
    }

    @Test
    void anInviteCanBeUsedOnlyOnce() throws Exception {
        MockHttpSession creator = adultUser();
        String code = createInvite(creator);
        redeem(adultUser(), code);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/chat/invites/redeem", Map.of("code", code)))
                .session(adultUser()))
            .andExpect(status().isNotFound());
    }

    @Test
    void youCannotUseYourOwnInvite() throws Exception {
        MockHttpSession creator = adultUser();
        String code = createInvite(creator);

        mockMvc.perform(withCsrf(jsonPost("/api/v1/chat/invites/redeem", Map.of("code", code)))
                .session(creator))
            .andExpect(status().isNotFound());
    }

    @Test
    void wrongAndExpiredCodesGetTheSameGenericAnswer() throws Exception {
        MockHttpSession creator = adultUser();
        MockHttpSession joiner = adultUser();
        String expiredCode = createInvite(creator);
        jdbc.update("UPDATE chat_invites SET expires_at = now() - interval '1 hour'");

        mockMvc.perform(withCsrf(jsonPost("/api/v1/chat/invites/redeem", Map.of("code", "ZZZZZ-ZZZZZ")))
                .session(joiner))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("That invite code is not valid or has expired."));
        mockMvc.perform(withCsrf(jsonPost("/api/v1/chat/invites/redeem", Map.of("code", expiredCode)))
                .session(joiner))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("That invite code is not valid or has expired."));
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
