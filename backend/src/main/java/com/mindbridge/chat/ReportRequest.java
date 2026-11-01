package com.mindbridge.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
    @NotNull(message = "Choose a reason.") ReportReason reason,
    @Size(max = 500, message = "A note can be at most 500 characters.") String note) {
}
