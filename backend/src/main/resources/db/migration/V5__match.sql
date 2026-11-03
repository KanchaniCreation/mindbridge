-- Public alias, short profile and match answers. chat_invites is left in place for now (unused).
ALTER TABLE users
    ADD COLUMN alias        VARCHAR(20),
    ADD COLUMN about        VARCHAR(140),
    ADD COLUMN real_name    VARCHAR(60),
    ADD COLUMN topics       VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN open_to_chat BOOLEAN      NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_alias_format CHECK (alias IS NULL OR alias ~ '^[A-Za-z0-9_]{3,20}$');

CREATE UNIQUE INDEX uq_users_alias_lower ON users (lower(alias));

CREATE TABLE match_decisions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    decision   VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_decision UNIQUE (actor_id, target_id),
    CONSTRAINT chk_match_decision CHECK (decision IN ('MATCH', 'SKIP')),
    CONSTRAINT chk_match_not_self CHECK (actor_id <> target_id)
);

CREATE INDEX idx_match_decisions_target ON match_decisions (target_id);
