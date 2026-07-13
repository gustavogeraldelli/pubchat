package dev.pubchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank
        @Size(max = 40)
        String sender,

        @NotBlank
        @Size(max = 500)
        String message
) {
}
