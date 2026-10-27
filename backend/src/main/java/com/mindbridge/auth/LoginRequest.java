package com.mindbridge.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Email is required.")
    @Size(max = 254, message = "Email is too long.") String email,

    @NotBlank(message = "Password is required.")
    @Size(max = 72, message = "Password is too long.") String password) {
}
