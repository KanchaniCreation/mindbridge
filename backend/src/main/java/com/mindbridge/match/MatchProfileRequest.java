package com.mindbridge.match;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record MatchProfileRequest(
    @NotBlank(message = "Choose an alias.")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$",
        message = "Alias must be 3 to 20 letters, numbers or underscores.") String alias,

    @Size(max = 140, message = "The short intro can be at most 140 characters.") String about,

    @Size(max = 60, message = "Name must be at most 60 characters.") String realName,

    @Size(max = 3, message = "Choose up to 3 topics.") Set<Topic> topics,

    boolean openToChat) {
}
