package com.mindbridge.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
    @NotBlank(message = "Password is required.")
    @Size(max = 72, message = "Password is too long.") String password) {
}
