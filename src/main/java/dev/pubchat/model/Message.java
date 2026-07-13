package dev.pubchat.model;

import java.time.Instant;

public record Message(
        String sender,
        String roomId,
        String message,
        MessageType type,
        Instant sentAt
) {
    public static Message chat(String sender, String roomId, String message) {
        return new Message(sender, roomId, message, MessageType.CHAT, Instant.now());
    }

    public static Message typing(String sender, String roomId) {
        return new Message(sender, roomId, null, MessageType.TYPING, Instant.now());
    }
}
