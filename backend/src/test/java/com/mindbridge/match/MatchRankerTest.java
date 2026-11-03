package com.mindbridge.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.user.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchRankerTest {

    private static User user(String alias, String topicsCsv) {
        User user = new User(alias + "@example.com", "hash", null, Instant.now());
        user.updateMatchProfile(alias, null, null, topicsCsv, true);
        return user;
    }

    private static List<String> aliases(List<MatchRanker.Ranked> ranked) {
        return ranked.stream().map(r -> r.user().getAlias()).toList();
    }

    @Test
    void peopleWithMoreSharedTopicsComeFirst() {
        List<User> candidates = List.of(
            user("none", "CREATIVITY"), user("one", "STUDY,SLEEP"), user("two", "STRESS,STUDY"));

        var ranked = MatchRanker.rank(EnumSet.of(Topic.STRESS, Topic.STUDY), candidates, 10);

        assertThat(aliases(ranked)).containsExactly("two", "one", "none");
        assertThat(ranked.get(0).shared()).containsExactlyInAnyOrder(Topic.STRESS, Topic.STUDY);
    }

    @Test
    void equalScoresKeepTheirOriginalOrder() {
        List<User> candidates = List.of(user("first", "STRESS"), user("second", "STRESS"), user("third", "STRESS"));

        var ranked = MatchRanker.rank(EnumSet.of(Topic.STRESS), candidates, 10);

        assertThat(aliases(ranked)).containsExactly("first", "second", "third");
    }

    @Test
    void theLimitIsRespected() {
        List<User> candidates = List.of(user("a_user", "STRESS"), user("b_user", "STRESS"), user("c_user", "STRESS"));

        assertThat(MatchRanker.rank(EnumSet.of(Topic.STRESS), candidates, 2)).hasSize(2);
    }

    @Test
    void everyoneStillAppearsWhenNothingIsShared() {
        List<User> candidates = List.of(user("a_user", "SLEEP"), user("b_user", ""));

        var ranked = MatchRanker.rank(EnumSet.of(Topic.STRESS), candidates, 10);

        assertThat(aliases(ranked)).containsExactly("a_user", "b_user");
        assertThat(ranked.get(1).shared()).isEmpty();
    }

    @Test
    void topicsAreStoredAndReadBackIgnoringUnknownNames() {
        assertThat(Topic.parse("STRESS,NOT_A_TOPIC,SLEEP")).containsExactlyInAnyOrder(Topic.STRESS, Topic.SLEEP);
        assertThat(Topic.join(List.of(Topic.STUDY, Topic.STRESS, Topic.STUDY))).isEqualTo("STRESS,STUDY");
        assertThat(Topic.parse(null)).isEmpty();
    }
}
