CREATE TABLE journal_entries (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(120),
    category   VARCHAR(40),
    body       TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_journal_body_length CHECK (char_length(body) BETWEEN 1 AND 10000)
);

CREATE INDEX idx_journal_user_created ON journal_entries (user_id, created_at DESC);

CREATE TABLE mood_entries (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    mood_value  INTEGER       NOT NULL,
    note        VARCHAR(1000),
    recorded_at TIMESTAMPTZ   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_mood_value CHECK (mood_value BETWEEN 1 AND 5)
);

CREATE INDEX idx_mood_user_recorded ON mood_entries (user_id, recorded_at DESC);

CREATE TABLE mood_entry_emotions (
    mood_entry_id UUID        NOT NULL REFERENCES mood_entries (id) ON DELETE CASCADE,
    emotion       VARCHAR(40) NOT NULL,
    PRIMARY KEY (mood_entry_id, emotion)
);

CREATE TABLE mood_entry_tags (
    mood_entry_id UUID        NOT NULL REFERENCES mood_entries (id) ON DELETE CASCADE,
    tag           VARCHAR(40) NOT NULL,
    PRIMARY KEY (mood_entry_id, tag)
);

CREATE INDEX idx_mood_tags_tag ON mood_entry_tags (tag);
