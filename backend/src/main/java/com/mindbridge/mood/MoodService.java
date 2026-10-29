package com.mindbridge.mood;

import com.mindbridge.common.ApiException;
import com.mindbridge.common.PageResponse;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoodService {

    static final int MINIMUM_ENTRIES_FOR_SUMMARY = 5;
    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_RANGE_DAYS = 366;
    private static final Duration CLOCK_SKEW_ALLOWED = Duration.ofMinutes(5);

    private final MoodEntryRepository moods;
    private final Clock clock;

    public MoodService(MoodEntryRepository moods, Clock clock) {
        this.moods = moods;
        this.clock = clock;
    }

    @Transactional
    public MoodEntryResponse create(UUID userId, MoodEntryRequest request) {
        Instant now = clock.instant();
        Instant recordedAt = request.recordedAt() != null ? request.recordedAt() : now;
        if (recordedAt.isAfter(now.plus(CLOCK_SKEW_ALLOWED))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The check-in time cannot be in the future.");
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        MoodEntry entry = new MoodEntry(userId, request.moodValue(),
            normalize(request.emotions()), normalize(request.tags()), note, recordedAt);
        return MoodEntryResponse.from(moods.save(entry));
    }

    @Transactional(readOnly = true)
    public PageResponse<MoodEntryResponse> list(UUID userId, Instant from, Instant to, String tag,
                                                int page, int size) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'.");
            }
            if (Duration.between(from, to).toDays() > MAX_RANGE_DAYS) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The date range can be at most 366 days.");
            }
        }
        Instant start = from != null ? from : Instant.EPOCH;
        Instant end = to != null ? to : clock.instant().plus(Duration.ofDays(1));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        String tagFilter = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        Page<MoodEntry> result = tagFilter.isEmpty()
            ? moods.findInRange(userId, start, end, pageable)
            : moods.findInRangeWithTag(userId, tagFilter, start, end, pageable);
        return PageResponse.from(result.map(MoodEntryResponse::from));
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        MoodEntry entry = moods.findByIdAndUserId(entryId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mood entry not found."));
        moods.delete(entry);
    }

    @Transactional(readOnly = true)
    public MoodSummaryResponse summary(UUID userId, MoodPeriod period, String timezone) {
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown timezone.");
        }
        Instant to = clock.instant();
        Instant from = to.minus(Duration.ofDays(period.days()));
        List<MoodSample> samples = moods.findAllInRange(userId, from, to).stream()
            .map(e -> new MoodSample(e.getMoodValue(), e.getRecordedAt(), e.getEmotions(), e.getTags()))
            .toList();
        return MoodSummaryCalculator.calculate(
            period.name(), from, to, zone, MINIMUM_ENTRIES_FOR_SUMMARY, samples);
    }

    /** Trim, lower-case, drop blanks and duplicates, keep the order the user gave. */
    private static Set<String> normalize(Collection<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String cleaned = value.trim().toLowerCase(Locale.ROOT);
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
        }
        return result;
    }
}
