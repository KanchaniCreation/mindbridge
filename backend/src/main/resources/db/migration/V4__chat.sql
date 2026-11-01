ALTER TABLE users ADD COLUMN adult_confirmed_at TIMESTAMPTZ;

CREATE TABLE chat_invites (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash   VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_invites_creator ON chat_invites (creator_id);

CREATE TABLE chats (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    status     VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    closed_by  UUID        REFERENCES users (id) ON DELETE SET NULL,
    closed_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_chat_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE chat_participants (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id   UUID        NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pseudonym VARCHAR(40) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_chat_participant_user UNIQUE (chat_id, user_id),
    CONSTRAINT uq_chat_participant_pseudonym UNIQUE (chat_id, pseudonym)
);

CREATE INDEX idx_chat_participants_user ON chat_participants (user_id);

CREATE TABLE chat_messages (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id           UUID          NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    client_message_id UUID          NOT NULL,
    body              VARCHAR(2000) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_chat_message_client UNIQUE (chat_id, sender_id, client_message_id),
    CONSTRAINT chk_chat_message_length CHECK (char_length(body) BETWEEN 1 AND 2000)
);

CREATE INDEX idx_chat_messages_chat_created ON chat_messages (chat_id, created_at DESC);

CREATE TABLE chat_reports (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id     UUID        NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    reporter_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason      VARCHAR(20) NOT NULL,
    note        VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_chat_report_reason CHECK (reason IN ('HARASSMENT', 'SPAM', 'UNSAFE', 'OTHER'))
);
