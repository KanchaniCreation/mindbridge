package com.mindbridge.mood;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MoodEntryResponse(UUID id, int moodValue, List<String> emotions, List<String> tags,
                                String note, Instant recordedAt) {

    /** Must be called inside a transaction (the emotion and tag collections load lazily). */
    public static MoodEntryResponse from(MoodEntry e) {
        return new MoodEntryResponse(e.getId(), e.getMoodValue(),
            e.getEmotions().stream().sorted().toList(),
            e.getTags().stream().sorted().toList(),
            e.getNote(), e.getRecordedAt());
    }
}
