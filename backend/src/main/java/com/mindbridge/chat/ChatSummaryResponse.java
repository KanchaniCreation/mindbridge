package com.mindbridge.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatSummaryResponse(UUID id, ChatStatus status, String myPseudonym,
                                  String partnerPseudonym, Instant createdAt, Instant lastMessageAt) {
}
