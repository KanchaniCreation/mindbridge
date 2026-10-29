package com.mindbridge.journal;

import java.time.Instant;
import java.util.UUID;

public record JournalEntryResponse(UUID id, String title, String category, String body,
                                   Instant createdAt, Instant updatedAt) {

    public static JournalEntryResponse from(JournalEntry e) {
        return new JournalEntryResponse(e.getId(), e.getTitle(), e.getCategory(), e.getBody(),
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
