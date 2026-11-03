package com.mindbridge.match;

import java.util.List;

/** What one person sees of another: alias, short intro and topics. No id, email or real name. */
public record DiscoverCard(String alias, String about, List<Topic> topics, List<Topic> sharedTopics) {
}
