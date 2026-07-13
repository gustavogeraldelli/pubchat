package dev.pubchat.dto;

import java.time.Instant;

public record ChatErrorResponse(
        String message,
        String roomId,
        String action,
        Instant sentAt
) {
    public static ChatErrorResponse of(String message, String roomId, String action) {
        return new ChatErrorResponse(message, roomId, action, Instant.now());
    }
}
