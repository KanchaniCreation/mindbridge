package com.mindbridge.journal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JournalEntryRequest(
    @Size(max = 120, message = "Title must be at most 120 characters.") String title,
    @Size(max = 40, message = "Category must be at most 40 characters.") String category,
    @NotBlank(message = "Write something before saving.")
    @Size(max = 10000, message = "An entry can be at most 10,000 characters.") String body) {
}
