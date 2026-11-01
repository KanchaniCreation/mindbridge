package com.mindbridge.chat;

import java.time.Instant;

/** The code is shown once, here. The server keeps only its hash. */
public record InviteResponse(String code, Instant expiresAt) {
}
