package com.mindbridge.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemInviteRequest(
    @NotBlank(message = "Enter the invite code.")
    @Size(max = 32, message = "That code is too long.") String code) {
}
