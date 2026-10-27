package com.mindbridge.user;

import java.time.Instant;
import java.util.UUID;

/** What the API returns about a user. Note: no password or hash fields exist here at all. */
public record UserResponse(UUID id, String email, String displayName, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
