package com.mindbridge.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.mindbridge.MatchFixtures;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class MatchApiTest extends MatchFixtures {

    @Test
    void profileCanBeSavedAndReadBack() throws Exception {
        MockHttpSession me = registerAndLogin(uniqueEmail());
        Map<String, Object> body = profileBody("night_owl", true, Topic.SLEEP, Topic.STUDY);
        body.put("realName", "Test Person");

        assertThat(putProfile(me, body).getResponse().getStatus()).isEqualTo(200);

        mockMvc.perform(get("/api/v1/match/profile").session(me))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alias").value("night_owl"))
            .andExpect(jsonPath("$.realName").value("Test Person"))
            .andExpect(jsonPath("$.openToChat").value(true))
            .andExpect(jsonPath("$.topics.length()").value(2));
    }

    @Test
    void anAliasIsUniqueIgnoringCase() throws Exception {
        MockHttpSession first = registerAndLogin(uniqueEmail());
        MockHttpSession second = registerAndLogin(uniqueEmail());

        assertThat(putProfile(first, profileBody("Night_Owl", false)).getResponse().getStatus()).isEqualTo(200);
        assertThat(putProfile(second, profileBody("night_owl", false)).getResponse().getStatus()).isEqualTo(409);
        // The owner can save their own alias again.
        assertThat(putProfile(first, profileBody("Night_Owl", false)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void invalidAndReservedAliasesAreRejected() throws Exception {
        MockHttpSession me = registerAndLogin(uniqueEmail());

        for (String bad : new String[] {"ab", "has space", "me@mail.com", "a".repeat(21), "Admin"}) {
            assertThat(putProfile(me, profileBody(bad, false)).getResponse().getStatus())
                .as("alias '%s'", bad).isEqualTo(400);
        }
    }

    @Test
    void beingOpenToChatNeedsAtLeastOneTopic() throws Exception {
        MockHttpSession me = registerAndLogin(uniqueEmail());

        assertThat(putProfile(me, profileBody("no_topics", true)).getResponse().getStatus()).isEqualTo(400);
        assertThat(putProfile(me, profileBody("no_topics", false)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void meetingPeopleNeedsAdultAliasAndBeingOpen() throws Exception {
        MockHttpSession me = registerAndLogin(uniqueEmail());
        mockMvc.perform(get("/api/v1/match/discover").session(me)).andExpect(status().isForbidden());

        confirmAdult(me);
        mockMvc.perform(get("/api/v1/match/discover").session(me)).andExpect(status().isConflict());

        putProfile(me, profileBody("me_user", false, Topic.STRESS));
        mockMvc.perform(get("/api/v1/match/discover").session(me)).andExpect(status().isConflict());

        putProfile(me, profileBody("me_user", true, Topic.STRESS));
        mockMvc.perform(get("/api/v1/match/discover").session(me)).andExpect(status().isOk());
    }

    @Test
    void discoverRanksPeopleWhoShareMoreTopicsFirst() throws Exception {
        openUser("no_overlap", Topic.CREATIVITY);
        openUser("one_shared", Topic.STUDY, Topic.SLEEP);
        openUser("two_shared", Topic.STRESS, Topic.STUDY);
        MockHttpSession me = openUser("me_user", Topic.STRESS, Topic.STUDY);

        JsonNode cards = discover(me);

        assertThat(aliasesIn(cards)).containsExactly("two_shared", "one_shared", "no_overlap");
        assertThat(cards.get(0).get("sharedTopics").size()).isEqualTo(2);
        assertThat(cards.toString()).as("no email or user id").doesNotContain("@");
        assertThat(cards.get(0).has("id")).isFalse();
    }

    @Test
    void theRealNameIsNeverShownToOthers() throws Exception {
        MockHttpSession other = openUser("other_user", Topic.STRESS);
        Map<String, Object> body = profileBody("other_user", true, Topic.STRESS);
        body.put("realName", "Secret Realname");
        assertThat(putProfile(other, body).getResponse().getStatus()).isEqualTo(200);
        MockHttpSession me = openUser("me_user", Topic.STRESS);

        assertThat(discover(me).toString()).doesNotContain("Secret Realname");
        JsonNode matchResult = answer(other, "me_user", "MATCH");
        assertThat(matchResult.toString()).doesNotContain("Secret Realname");
        assertThat(answer(me, "other_user", "MATCH").toString()).doesNotContain("Secret Realname");
    }

    @Test
    void discoverLeavesOutMyselfAndPeopleWhoAreNotOpen() throws Exception {
        MockHttpSession me = openUser("me_user", Topic.STRESS);
        MockHttpSession notOpen = openUser("not_open", Topic.STRESS);
        putProfile(notOpen, profileBody("not_open", false, Topic.STRESS));

        assertThat(discover(me).size()).isZero();
    }

    @Test
    void whenBothChooseMatchAChatOpens() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        MockHttpSession bob = openUser("bob_b", Topic.STRESS);

        JsonNode first = answer(alice, "bob_b", "MATCH");
        assertThat(first.get("matched").asBoolean()).isFalse();
        mockMvc.perform(get("/api/v1/chats").session(bob)).andExpect(jsonPath("$.length()").value(0));

        JsonNode second = answer(bob, "alice_a", "MATCH");
        assertThat(second.get("matched").asBoolean()).isTrue();
        assertThat(second.get("chat").get("myPseudonym").asText()).isEqualTo("bob_b");
        assertThat(second.get("chat").get("partnerPseudonym").asText()).isEqualTo("alice_a");

        mockMvc.perform(get("/api/v1/chats").session(alice))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].partnerPseudonym").value("bob_b"));
    }

    @Test
    void aOneSidedMatchLooksLikeASkipAndRevealsNothing() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        MockHttpSession bob = openUser("bob_b", Topic.STRESS);
        openUser("carol_c", Topic.STRESS);

        JsonNode matchAnswer = answer(alice, "bob_b", "MATCH");
        JsonNode skipAnswer = answer(alice, "carol_c", "SKIP");

        assertThat(matchAnswer).isEqualTo(skipAnswer);
        mockMvc.perform(get("/api/v1/chats").session(bob)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aSkippedPersonIsNotShownAgainInEitherDirection() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        MockHttpSession bob = openUser("bob_b", Topic.STRESS);

        answer(alice, "bob_b", "SKIP");

        assertThat(aliasesIn(discover(alice))).doesNotContain("bob_b");
        assertThat(aliasesIn(discover(bob))).doesNotContain("alice_a");
    }

    @Test
    void youCannotAnswerForTheSamePersonTwice() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        openUser("bob_b", Topic.STRESS);

        answer(alice, "bob_b", "SKIP");

        assertThat(answerRaw(alice, "bob_b", "MATCH").getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void unknownSelfAndHiddenPeopleGetTheSameAnswer() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        MockHttpSession hidden = openUser("hidden_h", Topic.STRESS);
        putProfile(hidden, profileBody("hidden_h", false, Topic.STRESS));

        for (String alias : new String[] {"nobody_here", "alice_a", "hidden_h"}) {
            assertThat(answerRaw(alice, alias, "MATCH").getResponse().getStatus())
                .as("alias '%s'", alias).isEqualTo(404);
        }
    }

    @Test
    void thereIsADailyLimitOnAnswers() throws Exception {
        MockHttpSession me = openUser("me_user", Topic.STRESS);
        for (int i = 0; i < 20; i++) {
            openUser("target_" + i, Topic.STRESS);
            assertThat(answerRaw(me, "target_" + i, "SKIP").getResponse().getStatus()).isEqualTo(200);
        }
        openUser("target_20", Topic.STRESS);

        assertThat(answerRaw(me, "target_20", "SKIP").getResponse().getStatus()).isEqualTo(429);
    }

    @Test
    void thereIsALimitOfFiveOpenChats() throws Exception {
        MockHttpSession me = openUser("me_user", Topic.STRESS);
        String firstChatId = null;
        for (int i = 0; i < 5; i++) {
            MockHttpSession partner = openUser("partner_" + i, Topic.STRESS);
            answer(partner, "me_user", "MATCH");
            JsonNode result = answer(me, "partner_" + i, "MATCH");
            assertThat(result.get("matched").asBoolean()).isTrue();
            if (firstChatId == null) {
                firstChatId = result.get("chat").get("id").asText();
            }
        }
        MockHttpSession sixth = openUser("partner_5", Topic.STRESS);
        answer(sixth, "me_user", "MATCH");

        assertThat(answerRaw(me, "partner_5", "MATCH").getResponse().getStatus()).isEqualTo(409);

        mockMvc.perform(withCsrf(post("/api/v1/chats/" + firstChatId + "/close")).session(me))
            .andExpect(status().isNoContent());

        assertThat(answer(me, "partner_5", "MATCH").get("matched").asBoolean()).isTrue();
    }

    @Test
    void peopleWhoHadAChatNeverSeeEachOtherAgain() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        MockHttpSession bob = openUser("bob_b", Topic.STRESS);
        String chatId = startChat(alice, bob).get("id").asText();

        assertThat(aliasesIn(discover(alice))).isEmpty();
        assertThat(aliasesIn(discover(bob))).isEmpty();

        // Closing the chat (the way to block someone) keeps them apart for good.
        mockMvc.perform(withCsrf(post("/api/v1/chats/" + chatId + "/close")).session(alice))
            .andExpect(status().isNoContent());

        assertThat(aliasesIn(discover(alice))).isEmpty();
        assertThat(aliasesIn(discover(bob))).isEmpty();
    }

    @Test
    void anonymousVisitorsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/match/discover")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/match/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAnAccountRemovesItsAnswers() throws Exception {
        MockHttpSession alice = openUser("alice_a", Topic.STRESS);
        MockHttpSession bob = openUser("bob_b", Topic.STRESS);
        answer(alice, "bob_b", "SKIP");

        deleteAccount(alice);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM match_decisions", Integer.class)).isZero();
        assertThat(aliasesIn(discover(bob))).isEmpty();
    }
}
