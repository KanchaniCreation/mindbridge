package com.mindbridge.mood;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** What the user reports. recordedAt is optional (defaults to now) so past check-ins can be added. */
public record MoodEntryRequest(
    @Min(value = 1, message = "Mood must be between 1 and 5.")
    @Max(value = 5, message = "Mood must be between 1 and 5.") int moodValue,

    @Size(max = 5, message = "Choose at most 5 emotions.")
    List<@NotBlank(message = "Emotions cannot be empty.")
         @Size(max = 40, message = "Each emotion must be at most 40 characters.") String> emotions,

    @Size(max = 8, message = "Use at most 8 tags.")
    List<@NotBlank(message = "Tags cannot be empty.")
         @Size(max = 40, message = "Each tag must be at most 40 characters.") String> tags,

    @Size(max = 1000, message = "A note can be at most 1,000 characters.") String note,

    Instant recordedAt) {
}
