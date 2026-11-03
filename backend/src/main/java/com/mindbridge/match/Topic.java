package com.mindbridge.match;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** What a person would like to talk about. The matching engine compares these. */
public enum Topic {
    STRESS,
    STUDY,
    CAREER,
    LONELINESS,
    RELATIONSHIPS,
    FAMILY,
    SLEEP,
    MOTIVATION,
    SELF_CONFIDENCE,
    CREATIVITY;

    /** Reads the comma-separated form stored in the database. Unknown names are ignored. */
    public static Set<Topic> parse(String csv) {
        Set<Topic> result = EnumSet.noneOf(Topic.class);
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String part : csv.split(",")) {
            try {
                result.add(Topic.valueOf(part.trim()));
            } catch (IllegalArgumentException ignored) {
                // not a known topic
            }
        }
        return result;
    }

    public static String join(Collection<Topic> topics) {
        return topics.stream().distinct().sorted().map(Enum::name).collect(Collectors.joining(","));
    }
}
