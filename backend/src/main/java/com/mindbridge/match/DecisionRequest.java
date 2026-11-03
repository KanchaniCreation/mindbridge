package com.mindbridge.match;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
    @NotBlank(message = "Choose a person.")
    @Size(max = 20, message = "That alias is too long.") String alias,

    @NotNull(message = "Choose Match or Skip.") Decision decision) {
}
