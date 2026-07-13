package dev.pubchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TypingStatusRequest(
        @NotBlank
        @Size(max = 40)
        String sender
) {
}
