ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE user_preferences (
    user_id           UUID        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    preferred_style   VARCHAR(30) NOT NULL DEFAULT 'NO_PREFERENCE',
    quiet_start       TIME,
    quiet_end         TIME,
    reminders_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_preferred_style
        CHECK (preferred_style IN ('NO_PREFERENCE', 'PRACTICAL', 'REFLECTIVE', 'CREATIVE')),
    CONSTRAINT chk_quiet_hours_pair
        CHECK ((quiet_start IS NULL) = (quiet_end IS NULL))
);
