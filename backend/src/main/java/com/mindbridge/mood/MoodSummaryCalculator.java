package com.mindbridge.mood;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pure calculation: the same samples always give the same summary.
 * It reports what was recorded. It never says WHY a mood changed.
 */
public final class MoodSummaryCalculator {

    static final String NOTE =
        "Calculated from the check-ins you recorded. It describes what you entered, not why.";
    private static final double DIRECTION_THRESHOLD = 0.5;
    private static final int MIN_ENTRIES_PER_HALF = 2;
    private static final int TOP_LIMIT = 5;

    private MoodSummaryCalculator() {
    }

    public static MoodSummaryResponse calculate(String period, Instant from, Instant to, ZoneId zone,
                                                int minimumEntries, List<MoodSample> samples) {
        int count = samples.size();
        boolean sufficient = count >= minimumEntries;

        Map<Integer, Long> moodCounts = new LinkedHashMap<>();
        for (int value = 1; value <= 5; value++) {
            moodCounts.put(value, 0L);
        }
        for (MoodSample sample : samples) {
            moodCounts.merge(sample.moodValue(), 1L, Long::sum);
        }

        TreeMap<LocalDate, List<MoodSample>> byDay = new TreeMap<>();
        for (MoodSample sample : samples) {
            byDay.computeIfAbsent(sample.recordedAt().atZone(zone).toLocalDate(), d -> new ArrayList<>())
                .add(sample);
        }
        List<DailyMood> daily = byDay.entrySet().stream()
            .map(e -> new DailyMood(e.getKey(), round(average(e.getValue())), e.getValue().size()))
            .toList();

        Double averageMood = null;
        Double firstHalfAverage = null;
        Double secondHalfAverage = null;
        String direction = null;

        if (sufficient) {
            averageMood = round(average(samples));
            Instant midpoint = from.plus(Duration.between(from, to).dividedBy(2));
            List<MoodSample> firstHalf = samples.stream().filter(s -> s.recordedAt().isBefore(midpoint)).toList();
            List<MoodSample> secondHalf = samples.stream().filter(s -> !s.recordedAt().isBefore(midpoint)).toList();
            if (firstHalf.size() >= MIN_ENTRIES_PER_HALF && secondHalf.size() >= MIN_ENTRIES_PER_HALF) {
                double first = average(firstHalf);
                double second = average(secondHalf);
                firstHalfAverage = round(first);
                secondHalfAverage = round(second);
                double difference = second - first;
                direction = difference >= DIRECTION_THRESHOLD ? "HIGHER"
                    : difference <= -DIRECTION_THRESHOLD ? "LOWER" : "SIMILAR";
            }
        }

        return new MoodSummaryResponse(
            period, from, to, zone.getId(), count, byDay.size(), minimumEntries, sufficient,
            moodCounts,
            top(samples.stream().flatMap(s -> s.emotions().stream())),
            top(samples.stream().flatMap(s -> s.tags().stream())),
            averageMood, firstHalfAverage, secondHalfAverage, direction, daily, NOTE);
    }

    private static List<NamedCount> top(Stream<String> values) {
        Comparator<Map.Entry<String, Long>> byCountDescending =
            Map.Entry.<String, Long>comparingByValue().reversed();
        return values
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(byCountDescending.thenComparing(Map.Entry.comparingByKey()))
            .limit(TOP_LIMIT)
            .map(e -> new NamedCount(e.getKey(), e.getValue()))
            .toList();
    }

    private static double average(List<MoodSample> samples) {
        return samples.stream().mapToInt(MoodSample::moodValue).average().orElse(0);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
