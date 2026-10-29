package com.mindbridge.mood;

import java.time.Instant;
import java.util.Collection;

/** A plain value the calculator works on, so the maths has no database or Spring dependency. */
public record MoodSample(int moodValue, Instant recordedAt, Collection<String> emotions, Collection<String> tags) {
}
