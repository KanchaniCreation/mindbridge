package com.mindbridge.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email is too long.") String email,

    // BCrypt only uses the first 72 bytes, so longer passwords are rejected instead of silently cut.
    @NotBlank(message = "Password is required.")
    @Size(min = 12, max = 72, message = "Password must be 12 to 72 characters.") String password,

    @Size(max = 60, message = "Name must be at most 60 characters.") String displayName,

    @AssertTrue(message = "You must accept to continue.") boolean acceptedTerms) {
}
