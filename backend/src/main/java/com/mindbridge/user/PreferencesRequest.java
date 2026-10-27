package com.mindbridge.user;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record PreferencesRequest(
    @NotNull(message = "Preferred style is required.") PreferredStyle preferredStyle,
    LocalTime quietStart,
    LocalTime quietEnd,
    boolean remindersEnabled) {
}
