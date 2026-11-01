package com.mindbridge.chat;

import java.time.Instant;
import java.util.UUID;

/** No user id or email appears here: only whether it is mine and the sender's pseudonym. */
public record ChatMessageResponse(UUID id, boolean mine, String senderPseudonym, String body,
                                  Instant createdAt) {
}
