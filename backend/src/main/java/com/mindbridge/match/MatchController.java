package com.mindbridge.match;

import com.mindbridge.security.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/match")
public class MatchController {

    private final MatchService match;

    public MatchController(MatchService match) {
        this.match = match;
    }

    @GetMapping("/profile")
    public MatchProfileResponse profile(@AuthenticationPrincipal AppUserDetails me) {
        return match.getProfile(me.getId());
    }

    @PutMapping("/profile")
    public MatchProfileResponse updateProfile(@AuthenticationPrincipal AppUserDetails me,
                                              @Valid @RequestBody MatchProfileRequest request) {
        return match.updateProfile(me.getId(), request);
    }

    @GetMapping("/discover")
    public List<DiscoverCard> discover(@AuthenticationPrincipal AppUserDetails me,
                                       @RequestParam(name = "size", defaultValue = "10") int size) {
        return match.discover(me.getId(), size);
    }

    @PostMapping("/decisions")
    public MatchResult decide(@AuthenticationPrincipal AppUserDetails me,
                              @Valid @RequestBody DecisionRequest request) {
        return match.decide(me.getId(), request);
    }
}
