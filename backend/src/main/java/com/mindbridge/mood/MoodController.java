package com.mindbridge.mood;

import com.mindbridge.common.PageResponse;
import com.mindbridge.security.AppUserDetails;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moods")
public class MoodController {

    private final MoodService moods;

    public MoodController(MoodService moods) {
        this.moods = moods;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MoodEntryResponse create(@AuthenticationPrincipal AppUserDetails me,
                                    @Valid @RequestBody MoodEntryRequest request) {
        return moods.create(me.getId(), request);
    }

    @GetMapping
    public PageResponse<MoodEntryResponse> list(@AuthenticationPrincipal AppUserDetails me,
                                                @RequestParam(name = "from", required = false) Instant from,
                                                @RequestParam(name = "to", required = false) Instant to,
                                                @RequestParam(name = "tag", required = false) String tag,
                                                @RequestParam(name = "page", defaultValue = "0") int page,
                                                @RequestParam(name = "size", defaultValue = "20") int size) {
        return moods.list(me.getId(), from, to, tag, page, size);
    }

    @GetMapping("/summary")
    public MoodSummaryResponse summary(@AuthenticationPrincipal AppUserDetails me,
                                       @RequestParam(name = "period", defaultValue = "WEEK") MoodPeriod period,
                                       @RequestParam(name = "timezone", defaultValue = "UTC") String timezone) {
        return moods.summary(me.getId(), period, timezone);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails me, @PathVariable("id") UUID id) {
        moods.delete(me.getId(), id);
    }
}
