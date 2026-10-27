package com.mindbridge.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 60, message = "Name must be at most 60 characters.") String displayName) {
}
