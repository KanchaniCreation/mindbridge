package com.mindbridge.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MessageRequest(
    @NotNull(message = "clientMessageId is required.") UUID clientMessageId,
    @NotBlank(message = "Write a message first.")
    @Size(max = 2000, message = "A message can be at most 2,000 characters.") String body) {
}
