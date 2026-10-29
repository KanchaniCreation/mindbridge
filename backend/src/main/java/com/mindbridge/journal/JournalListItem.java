package com.mindbridge.journal;

import java.time.Instant;
import java.util.UUID;

/** Lists return a short excerpt only, so a page of entries stays small. */
public record JournalListItem(UUID id, String title, String category, String excerpt,
                              Instant createdAt, Instant updatedAt) {

    private static final int EXCERPT_LENGTH = 200;

    public static JournalListItem from(JournalEntry e) {
        String body = e.getBody();
        String excerpt = body.length() <= EXCERPT_LENGTH ? body : body.substring(0, EXCERPT_LENGTH) + "…";
        return new JournalListItem(e.getId(), e.getTitle(), e.getCategory(), excerpt,
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
