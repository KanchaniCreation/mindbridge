package com.mindbridge.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_style", nullable = false, length = 30)
    private PreferredStyle preferredStyle;

    @Column(name = "quiet_start")
    private LocalTime quietStart;

    @Column(name = "quiet_end")
    private LocalTime quietEnd;

    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreferences() {
        // for JPA
    }

    /** Reminders are opt-in, so the default is OFF. */
    public static UserPreferences defaults(UUID userId) {
        UserPreferences p = new UserPreferences();
        p.userId = userId;
        p.preferredStyle = PreferredStyle.NO_PREFERENCE;
        p.remindersEnabled = false;
        p.updatedAt = Instant.now();
        return p;
    }

    public void update(PreferredStyle style, LocalTime quietStart, LocalTime quietEnd, boolean remindersEnabled) {
        this.preferredStyle = style;
        this.quietStart = quietStart;
        this.quietEnd = quietEnd;
        this.remindersEnabled = remindersEnabled;
        this.updatedAt = Instant.now();
    }

    public PreferredStyle getPreferredStyle() {
        return preferredStyle;
    }

    public LocalTime getQuietStart() {
        return quietStart;
    }

    public LocalTime getQuietEnd() {
        return quietEnd;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }
}
