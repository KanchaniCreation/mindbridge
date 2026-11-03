package com.mindbridge.match;

import com.mindbridge.user.User;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The rule-based part of matching: people who share more topics with you come first.
 * It is stable, so people with the same score keep the (random) order they arrived in.
 */
public final class MatchRanker {

    public record Ranked(User user, Set<Topic> topics, Set<Topic> shared) {
    }

    private MatchRanker() {
    }

    public static List<Ranked> rank(Set<Topic> mine, List<User> candidates, int limit) {
        return candidates.stream()
            .map(user -> {
                Set<Topic> theirs = Topic.parse(user.getTopicsCsv());
                Set<Topic> shared = EnumSet.noneOf(Topic.class);
                shared.addAll(theirs);
                shared.retainAll(mine);
                return new Ranked(user, theirs, shared);
            })
            .sorted(Comparator.comparingInt((Ranked r) -> r.shared().size()).reversed())
            .limit(limit)
            .toList();
    }
}
