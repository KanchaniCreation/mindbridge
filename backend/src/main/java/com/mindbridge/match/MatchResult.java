package com.mindbridge.match;

import com.mindbridge.chat.ChatSummaryResponse;

/**
 * The answer to a Match or Skip. {@code matched} is true only when both people chose Match, so a
 * one-sided Match looks exactly like a Skip and never tells the other person anything.
 */
public record MatchResult(boolean matched, ChatSummaryResponse chat) {

    static MatchResult none() {
        return new MatchResult(false, null);
    }
}
