package dev.pubchat.model;

public record Message(
        String sender,
        String message,
        MessageType type) {

    public enum MessageType {
        CHAT,
        TYPING
    }

}
