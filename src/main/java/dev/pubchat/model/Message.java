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

    public static Message join(String sender, String roomId) {
        return new Message(sender, roomId, sender + " joined the room", MessageType.JOIN, Instant.now());
    }

    public static Message leave(String sender, String roomId) {
        return new Message(sender, roomId, sender + " left the room", MessageType.LEAVE, Instant.now());
    }
}
