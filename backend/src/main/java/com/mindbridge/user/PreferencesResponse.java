package com.mindbridge.user;

import java.time.LocalTime;

public record PreferencesResponse(
    PreferredStyle preferredStyle,
    LocalTime quietStart,
    LocalTime quietEnd,
    boolean remindersEnabled) {

    public static PreferencesResponse from(UserPreferences p) {
        return new PreferencesResponse(
            p.getPreferredStyle(), p.getQuietStart(), p.getQuietEnd(), p.isRemindersEnabled());
    }
}
