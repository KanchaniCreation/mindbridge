package com.mindbridge.chat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published inside the sending transaction; delivered to listeners only after it commits. */
public record ChatMessageSent(UUID chatId, UUID messageId, UUID senderId, String body, Instant createdAt,
                              Map<UUID, String> pseudonyms) {
}
