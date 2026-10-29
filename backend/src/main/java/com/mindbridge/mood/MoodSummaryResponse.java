package com.mindbridge.mood;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Everything here is CALCULATED from the check-ins the user recorded. Fields that would be
 * misleading with too little data (averages, direction) are null until there is enough data.
 */
public record MoodSummaryResponse(
    String period,
    Instant from,
    Instant to,
    String timezone,
    int entryCount,
    int daysWithCheckIn,
    int minimumEntries,
    boolean sufficientData,
    Map<Integer, Long> moodCounts,
    List<NamedCount> topEmotions,
    List<NamedCount> topTags,
    Double averageMood,
    Double firstHalfAverage,
    Double secondHalfAverage,
    String direction,
    List<DailyMood> daily,
    String note) {
}
