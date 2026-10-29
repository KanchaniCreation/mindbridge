package com.mindbridge.mood;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoodSummaryCalculatorTest {

    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-08T00:00:00Z");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static MoodSample sample(int mood, String at, String... tags) {
        return new MoodSample(mood, Instant.parse(at), List.of(), List.of(tags));
    }

    @Test
    void tooFewEntriesGiveCountsButNoAveragesOrDirection() {
        MoodSummaryResponse summary = MoodSummaryCalculator.calculate("WEEK", FROM, TO, UTC, 5, List.of(
            sample(4, "2026-01-02T10:00:00Z"),
            sample(4, "2026-01-03T10:00:00Z"),
            sample(2, "2026-01-04T10:00:00Z")));

        assertThat(summary.sufficientData()).isFalse();
        assertThat(summary.entryCount()).isEqualTo(3);
        assertThat(summary.averageMood()).isNull();
        assertThat(summary.direction()).isNull();
        assertThat(summary.moodCounts().get(4)).isEqualTo(2L);
        assertThat(summary.moodCounts().get(1)).isZero();
    }

    @Test
    void topTagsAreSortedByCountThenName() {
        MoodSummaryResponse summary = MoodSummaryCalculator.calculate("WEEK", FROM, TO, UTC, 1, List.of(
            sample(3, "2026-01-02T10:00:00Z", "b", "a"),
            sample(3, "2026-01-03T10:00:00Z", "b", "c"),
            sample(3, "2026-01-04T10:00:00Z", "a")));

        assertThat(summary.topTags()).extracting(NamedCount::name).containsExactly("a", "b", "c");
        assertThat(summary.topTags().get(0).count()).isEqualTo(2L);
    }

    @Test
    void smallDifferencesBetweenHalvesAreReportedAsSimilar() {
        MoodSummaryResponse summary = MoodSummaryCalculator.calculate("WEEK", FROM, TO, UTC, 5, List.of(
            sample(3, "2026-01-01T12:00:00Z"),
            sample(3, "2026-01-02T12:00:00Z"),
            sample(3, "2026-01-03T12:00:00Z"),
            sample(3, "2026-01-05T12:00:00Z"),
            sample(4, "2026-01-06T12:00:00Z"),
            sample(3, "2026-01-07T12:00:00Z")));

        assertThat(summary.sufficientData()).isTrue();
        assertThat(summary.direction()).isEqualTo("SIMILAR");
    }

    @Test
    void dailyBucketsUseTheRequestedTimezone() {
        MoodSummaryResponse summary = MoodSummaryCalculator.calculate(
            "WEEK", FROM, TO, ZoneId.of("Asia/Kolkata"), 1, List.of(sample(3, "2026-01-01T20:00:00Z")));

        assertThat(summary.daily()).hasSize(1);
        assertThat(summary.daily().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 2));
    }
}
